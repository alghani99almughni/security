package com.example

import com.example.data.SmartCheckRepository
import com.example.model.TicketStatus
import com.example.model.UserRole
import com.example.util.CsvUtils
import com.example.util.PlateOcrProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import kotlin.system.measureTimeMillis

class RegressionAndPerformanceTest {

  // ==========================================
  // REGRESSION TESTS: CSV PARSING & TEMPLATES
  // ==========================================

  @Test
  fun testStudentsParentsTemplateGeneration() {
    val template = CsvUtils.STUDENTS_PARENTS_TEMPLATE
    assertTrue("Header must contain Student Name", template.contains("Student Name"))
    assertTrue("Header must contain Class", template.contains("Class"))
    assertTrue("Header must contain Mobile", template.contains("Mobile"))
    assertTrue("Header must contain Bus Number", template.contains("Bus Number"))
    val lines = template.trim().split("\n")
    assertTrue("Template must contain header and sample rows", lines.size >= 3)
  }

  @Test
  fun testFlatOwnersTemplateGeneration() {
    val template = CsvUtils.FLAT_OWNERS_TEMPLATE
    assertTrue("Header must contain Owner Name", template.contains("Owner Name"))
    assertTrue("Header must contain Flat Number", template.contains("Flat Number"))
    assertTrue("Header must contain Mobile", template.contains("Mobile"))
    assertTrue("Header must contain Email", template.contains("Email"))
    val lines = template.trim().split("\n")
    assertTrue("Template must contain header and sample rows", lines.size >= 3)
  }

  @Test
  fun testParseStudentsParentsCsvValidData() {
    val csv = """
      Student Name,Class,Section,Parent Name,Mobile,Bus Number,Email
      "Aarav Sharma",4,A,"Rajesh Sharma",9876543210,BUS-12,rajesh@example.com
      "Diya Patel",5,B,"Pooja Patel",9123456780,BUS-05,pooja@example.com
      "Rohan Verma",3,C,"Kavita Verma",9988776655,,kavita@example.com
    """.trimIndent()

    val result = CsvUtils.parseStudentsParentsCsv(csv, "TEST-TENANT-01")
    assertEquals(3, result.parsedCount)
    assertEquals(0, result.errors.size)
    assertEquals(3, result.items.size)

    val first = result.items[0]
    assertEquals("Aarav Sharma", first.studentName)
    assertEquals("Grade 4", first.className)
    assertEquals("A", first.section)
    assertEquals("Rajesh Sharma", first.fatherName)
    assertEquals("9876543210", first.fatherMobile)
    assertEquals("BUS-12", first.busNumber)
    assertEquals("TEST-TENANT-01", first.tenantId)

    val second = result.items[1]
    assertEquals("Diya Patel", second.studentName)
    assertEquals("BUS-05", second.busNumber)

    val third = result.items[2]
    assertEquals("N/A", third.busNumber)
  }

  @Test
  fun testParseFlatOwnersCsvValidData() {
    val csv = """
      Owner Name,Flat Number,Block/Tower,Mobile,Email,Intercom/Designation
      "Vikram Malhotra",402,Tower A,9876541234,vikram@skyline.com,Ext 1402
      "Ananya Sen",104,Tower B,9812345678,ananya@gmail.com,Ext 1104
    """.trimIndent()

    val result = CsvUtils.parseFlatOwnersCsv(csv, "TENANT-APARTMENT-99")
    assertEquals(2, result.parsedCount)
    assertEquals(0, result.errors.size)
    assertEquals(2, result.items.size)

    val owner1 = result.items[0]
    assertEquals("Vikram Malhotra", owner1.name)
    assertEquals(UserRole.APPROVER, owner1.role)
    assertEquals("Ext 1402", owner1.designationOrFlat)
    assertEquals("9876541234", owner1.mobile)
    assertEquals("vikram@skyline.com", owner1.email)
    assertEquals("TENANT-APARTMENT-99", owner1.tenantId)
  }

  @Test
  fun testCsvParsingHandlesMalformedLinesGracefully() {
    val csvWithMissingRequired = """
      Student Name,Class,Section,Parent Name,Mobile,Bus Number,Email
      ,2,A,Parent Name,9898989898,BUS-1,
      Valid Student,3,B,Valid Parent,9191919191,BUS-2,
      Another Student,4,C,,,BUS-3,
    """.trimIndent()

    val result = CsvUtils.parseStudentsParentsCsv(csvWithMissingRequired, "TENANT-01")
    assertEquals(1, result.parsedCount)
    assertEquals(2, result.errors.size)
  }

  // ==========================================
  // REGRESSION TESTS: OCR PLATE EXTRACTION
  // ==========================================

  @Test
  fun testIndianVehiclePlateRegexAndExtraction() {
    val plateCandidates = listOf(
      "KA 01 AB 1234" to "KA-01-AB-1234",
      "DL-04C-9876" to "DL-04-C-9876",
      "MH12DE1432" to "MH-12-DE-1432",
      "TN 09 BZ 0001" to "TN-09-BZ-0001",
      "UP 16 AB 5678" to "UP-16-AB-5678",
      "HR26DQ5555" to "HR-26-DQ-5555"
    )

    for (pair in plateCandidates) {
      val raw = pair.first
      val expectedFormatted = pair.second
      val extracted = PlateOcrProcessor.extractPlateNumber(raw)
      assertNotNull("Plate '$raw' should be extracted", extracted)
      assertEquals(expectedFormatted, extracted)
    }
  }

  // ==========================================
  // REGRESSION TESTS: QR SCAN & APPROVAL FLOW
  // ==========================================

  @Test
  fun testQrScanAndGateAdmissionFlow() = runBlocking {
    val repository = SmartCheckRepository()

    // 1. Generate gate pass
    val ticket = repository.generateGatePassAtSecurity(
      visitorName = "Sunil Kumar",
      visitorMobile = "9876000000",
      purpose = "Official Meeting",
      targetDestination = "Flat 502",
      vehicleNumber = "KA05MN3456",
      overrideReceptionBypass = true
    )
    assertNotNull(ticket)
    assertEquals(TicketStatus.PENDING_APPROVAL, ticket.status)

    // 2. Approver or Gate Officer clears pass ("Actual QR code scanned or approve it")
    repository.approverSignOff(
      ticketId = ticket.ticketId,
      approverName = "Flat Owner 502",
      isApproved = true,
      remarks = "Authorized entry"
    )

    val approvedTicket = repository.tickets.value.find { it.ticketId == ticket.ticketId }
    assertNotNull(approvedTicket)
    assertEquals(TicketStatus.APPROVED, approvedTicket!!.status)
    assertNotNull(approvedTicket.qrToken)

    // 3. Security Guard scans actual QR at gate -> Grants Entry
    repository.verifyPreRegisteredQrAtGate(ticket.ticketId, "Officer Vikram S")
    val scannedTicket = repository.tickets.value.find { it.ticketId == ticket.ticketId }
    assertNotNull(scannedTicket)
    assertEquals(TicketStatus.INSIDE_PREMISES, scannedTicket!!.status)
    assertNotNull(scannedTicket.gateEntryTime)

    // 4. Exit Checkout
    repository.closeGatePassAtExit(ticket.ticketId, "Officer Vikram S")
    val closedTicket = repository.tickets.value.find { it.ticketId == ticket.ticketId }
    assertNotNull(closedTicket)
    assertEquals(TicketStatus.CLOSED, closedTicket!!.status)
    assertNotNull(closedTicket.gateExitTime)
  }

  // ==========================================
  // PERFORMANCE TESTS: HIGH VOLUME BENCHMARKS
  // ==========================================

  @Test
  fun testCsvParsingPerformanceOneThousandRows() {
    val sb = StringBuilder()
    sb.append("Student Name,Class,Section,Parent Name,Mobile,Bus Number,Email\n")
    for (i in 1..1000) {
      sb.append("Student $i,${(i % 12) + 1},${('A' + (i % 4))},Parent $i,98765${String.format("%05d", i)},BUS-${(i % 20) + 1},parent$i@mail.com\n")
    }
    val largeCsv = sb.toString()

    val durationMs = measureTimeMillis {
      val result = CsvUtils.parseStudentsParentsCsv(largeCsv, "BENCHMARK-TENANT")
      assertEquals(1000, result.parsedCount)
      assertEquals(0, result.errors.size)
    }

    println("PERFORMANCE: 1,000 Students CSV parsed in ${durationMs}ms")
    assertTrue("Parsing 1,000 rows must complete in under 500ms (took ${durationMs}ms)", durationMs < 500)
  }

  @Test
  fun testFlatOwnersCsvParsingPerformanceOneThousandRows() {
    val sb = StringBuilder()
    sb.append("Owner Name,Flat Number,Block/Tower,Mobile,Email,Intercom/Designation\n")
    for (i in 1..1000) {
      sb.append("Owner $i,${100 + (i % 800)},Tower ${('A' + (i % 5))},98111${String.format("%05d", i)},owner$i@community.org,Ext ${1000 + i}\n")
    }
    val largeCsv = sb.toString()

    val durationMs = measureTimeMillis {
      val result = CsvUtils.parseFlatOwnersCsv(largeCsv, "BENCHMARK-TENANT-APT")
      assertEquals(1000, result.parsedCount)
      assertEquals(0, result.errors.size)
    }

    println("PERFORMANCE: 1,000 Flat Owners CSV parsed in ${durationMs}ms")
    assertTrue("Parsing 1,000 rows must complete in under 500ms (took ${durationMs}ms)", durationMs < 500)
  }

  @Test
  fun testRepositoryBulkImportPerformance() {
    val repository = SmartCheckRepository()

    val sb = StringBuilder()
    sb.append("Student Name,Class,Section,Parent Name,Mobile,Bus Number,Email\n")
    for (i in 1..500) {
      sb.append("BulkStudent $i,6,A,BulkParent $i,98765${String.format("%05d", i)},BUS-1,p$i@mail.com\n")
    }

    val parsed = CsvUtils.parseStudentsParentsCsv(sb.toString(), "PERF-TENANT")
    val durationMs = measureTimeMillis {
      repository.addStudents(parsed.items)
    }

    println("PERFORMANCE: Bulk importing 500 students into repository took ${durationMs}ms")
    val currentStudents = repository.students.value
    assertTrue(currentStudents.any { it.studentName == "BulkStudent 1" })
    assertTrue("Bulk import must complete in under 200ms (took ${durationMs}ms)", durationMs < 200)
  }
}
