package com.example.data

/**
 * Google Apps Script Checkpoint & Snapsheet AI Embed Script.
 * Contains 1 Device 1 User Policy, User Auth, Receipt/Visitor OCR Logging,
 * and Drive Folder synchronization.
 */
object GoogleAppsScriptTemplate {

  val SCRIPT_CODE: String = """
// =============================================================================
// SMARTCHECK & SNAPSHEET AI - GOOGLE SHEETS & DRIVE CHECKPOINT SCRIPT
// Features: 1 Device 1 User Policy, User Auth, Visitor Pass Logging & Drive Upload
// =============================================================================

function doGet(e) {
  var action = (e && e.parameter && e.parameter.action) || "";
  var email = (e && e.parameter && e.parameter.email) || "";
  var deviceId = (e && e.parameter && e.parameter.deviceId) || "";
  if (action === "CHECK_AUTH" && email) {
    return handleCheckAuth(email, deviceId);
  }
  if (action === "VERIFY_SESSION" && email) {
    return handleVerifySession(email, deviceId);
  }
  if (action === "GET_USERS") {
    return handleGetUsers();
  }
  if (action === "GET_TENANTS") {
    return handleGetTenants();
  }
  return ContentService.createTextOutput(JSON.stringify({
    status: "ok",
    message: "SmartCheck AI Google Apps Script Checkpoint is active & running.",
    timestamp: new Date().toISOString()
  })).setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  try {
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var contents = (e && e.postData && e.postData.contents) ? e.postData.contents : "{}";
    var data = JSON.parse(contents);

    // 0. Verify Session (1 Device 1 User Policy)
    if (data.action === "VERIFY_SESSION") {
      return handleVerifySession(data.email || "", data.deviceId || "");
    }

    // 1. User Authentication Checkpoint
    if (data.action === "CHECK_AUTH" || data.action === "LOGIN") {
      return handleCheckAuth(data.email || "", data.deviceId || "");
    }

    // 2. Fetch Authorized Users List
    if (data.action === "GET_USERS") {
      return handleGetUsers();
    }

    // 3. Add Authorized Staff User
    if (data.action === "ADD_USER") {
      return handleAddUser(data.email, data.name, data.role);
    }

    // 4. Remove / Deactivate User
    if (data.action === "REMOVE_USER") {
      return handleRemoveUser(data.email);
    }

    // 5. Register Tenant in Multiple Tenant Register Sheet
    if (data.action === "REGISTER_TENANT") {
      return handleRegisterTenant(data);
    }

    // 6. Connection Ping Test
    if (data.type === "TEST_PING" || data.action === "PING") {
      var folderStatus = checkOrCreateDriveFolder(data.driveFolder || "SmartCheckPasses");
      ensureAuthorizedUsersSheet(ss);
      ensureTenantRegisterSheet(ss);
      return ContentService.createTextOutput(JSON.stringify({
        status: "ok",
        message: "Connected to Google Sheet & Drive! " + folderStatus
      })).setMimeType(ContentService.MimeType.JSON);
    }

    // 7. Check single-device session on row append if userEmail provided
    if (data.userEmail && data.deviceId) {
      var sessionCheck = checkSessionValid(data.userEmail, data.deviceId);
      if (!sessionCheck.valid) {
        return ContentService.createTextOutput(JSON.stringify({
          status: "SESSION_REVOKED",
          message: sessionCheck.message
        })).setMimeType(ContentService.MimeType.JSON);
      }
    }

    // 8. Log Gate Pass / Visitor Row to Monthly Sheet + Upload Image to Drive
    var sheet = ss.getActiveSheet();
    var fileUrl = "No Snapshot Attached";
    if (data.imageBase64 && data.imageBase64.length > 50) {
      try {
        var folder = getDriveFolder(data.driveFolder || "SmartCheckPasses");
        var fileName = (data.vehicleNumber || data.visitorName || "Visitor").replace(/[^a-zA-Z0-9_-]/g, "_") + "_" + (data.date || "Date") + ".jpg";
        var blob = Utilities.newBlob(Utilities.base64Decode(data.imageBase64), "image/jpeg", fileName);
        var file = folder.createFile(blob);
        fileUrl = file.getUrl();
      } catch (imgErr) {
        fileUrl = "Drive Upload Error: " + imgErr.toString();
      }
    }

    // Auto-create header row if sheet is empty
    if (sheet.getLastRow() === 0) {
      var headers = ["Date", "Ticket ID", "Tenant ID", "Visitor Name", "Mobile", "Purpose", "Target Destination", "Vehicle Number", "Status", "Approver Name", "QR Token", "Snapshot Link"];
      sheet.appendRow(headers);
      sheet.getRange(1, 1, 1, headers.length).setFontWeight("bold").setBackground("#10b981").setFontColor("#FFFFFF");
    }

    var rowValues = [
      data.date || new Date().toISOString(),
      data.ticketId || "",
      data.tenantId || "",
      data.visitorName || "",
      data.visitorMobile || "",
      data.purpose || "",
      data.targetDestination || "",
      data.vehicleNumber || "",
      data.status || "APPROVED",
      data.approverName || "",
      data.qrToken || "",
      fileUrl
    ];
    sheet.appendRow(rowValues);

    return ContentService.createTextOutput(JSON.stringify({
      status: "success",
      fileUrl: fileUrl,
      message: "Row added to monthly spreadsheet successfully."
    })).setMimeType(ContentService.MimeType.JSON);

  } catch(err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "error",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}

function ensureTenantRegisterSheet(ss) {
  var tenantSheet = ss.getSheetByName("MultipleTenantRegister");
  if (!tenantSheet) {
    tenantSheet = ss.insertSheet("MultipleTenantRegister");
    var headers = ["Tenant Code", "Organization Name", "Category", "Assigned Role", "Contact Person", "Email ID", "Mobile Number", "Reception Clearance", "Reception Staff Names", "PWA App URL", "Monthly Sheet URL", "Registered At", "Status"];
    tenantSheet.appendRow(headers);
    tenantSheet.getRange(1, 1, 1, headers.length).setFontWeight("bold").setBackground("#1e293b").setFontColor("#FFFFFF");
  }
  return tenantSheet;
}

function handleRegisterTenant(data) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ensureTenantRegisterSheet(ss);
  sheet.appendRow([
    data.code || "",
    data.name || "",
    data.category || "",
    data.role || "Tenant Administrator",
    data.contactName || "",
    data.adminEmail || "",
    data.contactMobile || "",
    data.hasReception ? "ENABLED" : "DISABLED (BYPASS TO APPROVER)",
    (data.receptionStaffNames || []).join(", "),
    data.pwaAppDownloadUrl || "https://ais-pre-hgxdor6xlnxz5ckwxcehwu-917492736497.asia-southeast1.run.app",
    data.monthlySpreadsheetUrl || "",
    new Date().toISOString(),
    "ACTIVE"
  ]);
  return jsonOutput({ status: "success", message: "Tenant organization added to Multiple Tenant Register Spreadsheet." });
}

function ensureAuthorizedUsersSheet(ss) {
  var usersSheet = ss.getSheetByName("AuthorizedUsers");
  if (!usersSheet) {
    usersSheet = ss.insertSheet("AuthorizedUsers");
    usersSheet.appendRow(["Email", "Name", "Role", "Status", "RegisteredAt", "ActiveDeviceId", "LastActiveTime"]);
    usersSheet.getRange(1, 1, 1, 7).setFontWeight("bold").setBackground("#0f766e").setFontColor("#FFFFFF");
    usersSheet.appendRow(["shukurhustle@gmail.com", "Syed Shukur", "OWNER", "ACTIVE", new Date().toISOString(), "", ""]);
  } else {
    var lastCol = usersSheet.getLastColumn();
    if (lastCol < 6) {
      usersSheet.getRange(1, 6).setValue("ActiveDeviceId").setFontWeight("bold").setBackground("#0f766e").setFontColor("#FFFFFF");
      usersSheet.getRange(1, 7).setValue("LastActiveTime").setFontWeight("bold").setBackground("#0f766e").setFontColor("#FFFFFF");
    }
  }
  return usersSheet;
}

function handleCheckAuth(email, deviceId) {
  var cleanEmail = (email || "").trim().toLowerCase();
  if (!cleanEmail) {
    return jsonOutput({ status: "error", authorized: false, message: "Email is empty." });
  }

  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var usersSheet = ensureAuthorizedUsersSheet(ss);
  var data = usersSheet.getDataRange().getValues();

  // Master Owner check
  if (cleanEmail === "shukurhustle@gmail.com" || cleanEmail === "syedshukur@gmail.com") {
    if (deviceId) {
      for (var o = 1; o < data.length; o++) {
        if (String(data[o][0] || "").trim().toLowerCase() === cleanEmail) {
          usersSheet.getRange(o + 1, 6).setValue(deviceId);
          usersSheet.getRange(o + 1, 7).setValue(new Date().toISOString());
          break;
        }
      }
    }
    return jsonOutput({
      status: "success",
      authorized: true,
      email: cleanEmail,
      name: "Syed Shukur",
      role: "OWNER",
      sheetName: "SmartCheckPasses",
      driveFolder: "SmartCheckPasses"
    });
  }

  for (var i = 1; i < data.length; i++) {
    var rowEmail = String(data[i][0] || "").trim().toLowerCase();
    var rowName = String(data[i][1] || "");
    var rowRole = String(data[i][2] || "STAFF").toUpperCase();
    var rowStatus = String(data[i][3] || "ACTIVE").toUpperCase();

    if (rowEmail === cleanEmail) {
      if (rowStatus === "ACTIVE") {
        if (deviceId) {
          usersSheet.getRange(i + 1, 6).setValue(deviceId);
          usersSheet.getRange(i + 1, 7).setValue(new Date().toISOString());
        }
        return jsonOutput({
          status: "success",
          authorized: true,
          email: cleanEmail,
          name: rowName,
          role: rowRole,
          sheetName: "SmartCheckPasses",
          driveFolder: "SmartCheckPasses"
        });
      } else {
        return jsonOutput({
          status: "error",
          authorized: false,
          message: "Access Denied: Account is deactivated by Admin."
        });
      }
    }
  }

  return jsonOutput({
    status: "error",
    authorized: false,
    message: "Access Denied: Email '" + cleanEmail + "' is not registered with the Admin. Please contact shukurhustle@gmail.com to be added."
  });
}

function checkSessionValid(email, deviceId) {
  var cleanEmail = (email || "").trim().toLowerCase();
  if (!cleanEmail || !deviceId) return { valid: true };
  if (cleanEmail === "shukurhustle@gmail.com" || cleanEmail === "syedshukur@gmail.com") return { valid: true };

  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var usersSheet = ensureAuthorizedUsersSheet(ss);
  var data = usersSheet.getDataRange().getValues();

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0] || "").trim().toLowerCase() === cleanEmail) {
      var rowStatus = String(data[i][3] || "ACTIVE").toUpperCase();
      if (rowStatus !== "ACTIVE") {
        return { valid: false, message: "Account is inactive." };
      }
      var activeDeviceId = String(data[i][5] || "").trim();
      if (activeDeviceId && activeDeviceId !== deviceId.trim()) {
        return {
          valid: false,
          message: "1 Device 1 User Policy: Your account was signed in on another device. You have been logged out on this device."
        };
      }
      return { valid: true };
    }
  }
  return { valid: false, message: "User not found." };
}

function handleVerifySession(email, deviceId) {
  var check = checkSessionValid(email, deviceId);
  if (!check.valid) {
    return jsonOutput({ status: "SESSION_REVOKED", message: check.message });
  }
  return jsonOutput({ status: "ACTIVE", message: "Session is active." });
}

function handleGetUsers() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var usersSheet = ensureAuthorizedUsersSheet(ss);
  var data = usersSheet.getDataRange().getValues();
  var users = [];

  for (var i = 1; i < data.length; i++) {
    var email = String(data[i][0] || "").trim();
    if (email) {
      users.push({
        email: email,
        name: String(data[i][1] || ""),
        role: String(data[i][2] || "STAFF"),
        status: String(data[i][3] || "ACTIVE"),
        registeredAt: String(data[i][4] || "")
      });
    }
  }

  return jsonOutput({ status: "success", users: users });
}

function handleAddUser(email, name, role) {
  var cleanEmail = (email || "").trim().toLowerCase();
  if (!cleanEmail) {
    return jsonOutput({ status: "error", message: "Invalid email" });
  }

  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var usersSheet = ensureAuthorizedUsersSheet(ss);
  var data = usersSheet.getDataRange().getValues();

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0] || "").trim().toLowerCase() === cleanEmail) {
      usersSheet.getRange(i + 1, 3).setValue(role || "STAFF");
      usersSheet.getRange(i + 1, 4).setValue("ACTIVE");
      return jsonOutput({ status: "success", message: "User status updated to ACTIVE" });
    }
  }

  usersSheet.appendRow([cleanEmail, name || "", role || "STAFF", "ACTIVE", new Date().toISOString(), "", ""]);
  return jsonOutput({ status: "success", message: "User registered successfully" });
}

function handleRemoveUser(email) {
  var cleanEmail = (email || "").trim().toLowerCase();
  if (cleanEmail === "shukurhustle@gmail.com" || cleanEmail === "syedshukur@gmail.com") {
    return jsonOutput({ status: "error", message: "Cannot remove Owner" });
  }

  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var usersSheet = ensureAuthorizedUsersSheet(ss);
  var data = usersSheet.getDataRange().getValues();

  for (var i = 1; i < data.length; i++) {
    if (String(data[i][0] || "").trim().toLowerCase() === cleanEmail) {
      usersSheet.getRange(i + 1, 4).setValue("INACTIVE");
      usersSheet.getRange(i + 1, 6).setValue("");
      return jsonOutput({ status: "success", message: "User deactivated" });
    }
  }

  return jsonOutput({ status: "error", message: "User not found" });
}

function getDriveFolder(folderName) {
  var folders = DriveApp.getFoldersByName(folderName);
  if (folders.hasNext()) {
    return folders.next();
  }
  return DriveApp.createFolder(folderName);
}

function checkOrCreateDriveFolder(folderName) {
  try {
    var folder = getDriveFolder(folderName);
    return "Drive folder '" + folderName + "' is ready (ID: " + folder.getId() + ")";
  } catch(e) {
    return "Drive access error: " + e.toString();
  }
}

function jsonOutput(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
""".trimIndent()
}
