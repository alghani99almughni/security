package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.Student
import com.example.model.User
import com.example.model.UserRole
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object CsvUtils {

  const val STUDENTS_PARENTS_HEADER = "Student Name,Class,Section,Parent Name,Mobile,Bus Number,Email"
  const val FLAT_OWNERS_HEADER = "Owner Name,Flat Number,Block/Tower,Mobile,Email,Intercom/Designation"

  val STUDENTS_PARENTS_TEMPLATE = """
Student Name,Class,Section,Parent Name,Mobile,Bus Number,Email
Aarav Sharma,5,A,Sunil Sharma,9876543210,BUS-12,sunil.sharma@example.com
Diya Patel,4,B,Meena Patel,9876543211,BUS-08,meena.patel@example.com
Rohan Gupta,6,C,Rajesh Gupta,9876543212,BUS-14,rajesh.gupta@example.com
Ananya Iyer,3,A,Venkatesh Iyer,9876543213,SELF,v.iyer@example.com
Kabir Verma,8,B,Suresh Verma,9876543214,BUS-03,suresh.v@example.com
""".trimIndent()

  val FLAT_OWNERS_TEMPLATE = """
Owner Name,Flat Number,Block/Tower,Mobile,Email,Intercom/Designation
Dr. Robert Smith,402,Tower A,9876543220,robert.smith@example.com,Ext 1402
Vikram Malhotra,805,Tower B,9876543221,vikram.m@example.com,Ext 1805
Priya Sundaram,1201,Tower C,9876543222,priya.s@example.com,Ext 2201
Arjun Kapoor,304,Tower A,9876543223,arjun.k@example.com,Ext 1304
Kavita Desai,501,Tower B,9876543224,kavita.d@example.com,Ext 1501
""".trimIndent()

  data class CsvParseResult<T>(
    val success: Boolean,
    val items: List<T>,
    val parsedCount: Int,
    val errorCount: Int,
    val errors: List<String>
  )

  /**
   * Robust CSV line splitter supporting quoted tokens.
   */
  fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false

    var i = 0
    while (i < line.length) {
      val c = line[i]
      if (c == '\"') {
        if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
          current.append('\"')
          i++
        } else {
          inQuotes = !inQuotes
        }
      } else if (c == ',' && !inQuotes) {
        result.add(current.toString().trim())
        current = StringBuilder()
      } else {
        current.append(c)
      }
      i++
    }
    result.add(current.toString().trim())
    return result
  }

  /**
   * Parses Parents & Students CSV into Student domain models.
   */
  fun parseStudentsParentsCsv(
    csvContent: String,
    tenantId: String
  ): CsvParseResult<Student> {
    val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) {
      return CsvParseResult(false, emptyList(), 0, 0, listOf("CSV content is empty."))
    }

    val students = mutableListOf<Student>()
    val errors = mutableListOf<String>()

    // Identify header row index
    val startIndex = if (lines[0].contains("Student Name", ignoreCase = true)) 1 else 0

    for (lineIndex in startIndex until lines.size) {
      val line = lines[lineIndex]
      val tokens = parseCsvLine(line)

      if (tokens.size < 4) {
        errors.add("Row ${lineIndex + 1}: Insufficient columns (expected at least 4: Student Name, Class, Section, Parent Name)")
        continue
      }

      val studentName = tokens.getOrNull(0)?.takeIf { it.isNotBlank() }
      val className = tokens.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "1"
      val section = tokens.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "A"
      val parentName = tokens.getOrNull(3)?.takeIf { it.isNotBlank() }
      val mobile = tokens.getOrNull(4)?.takeIf { it.isNotBlank() } ?: "9876543210"
      val busNumber = tokens.getOrNull(5)?.takeIf { it.isNotBlank() } ?: "N/A"
      val email = tokens.getOrNull(6)?.takeIf { it.isNotBlank() } ?: "${studentName?.lowercase()?.replace(" ", "") ?: "student"}@parent.com"

      if (studentName.isNullOrBlank() || parentName.isNullOrBlank()) {
        errors.add("Row ${lineIndex + 1}: Missing student name or parent name")
        continue
      }

      students.add(
        Student(
          studentId = "STU-" + UUID.randomUUID().toString().take(6).uppercase(),
          tenantId = tenantId,
          studentName = studentName,
          className = "Grade $className",
          section = section,
          fatherName = parentName,
          motherName = "Mother of $studentName",
          fatherMobile = mobile,
          motherMobile = mobile,
          busNumber = busNumber
        )
      )
    }

    return CsvParseResult(
      success = students.isNotEmpty(),
      items = students,
      parsedCount = students.size,
      errorCount = errors.size,
      errors = errors
    )
  }

  /**
   * Parses Flat Owners / Approvers CSV into User domain models with APPROVER role.
   */
  fun parseFlatOwnersCsv(
    csvContent: String,
    tenantId: String
  ): CsvParseResult<User> {
    val lines = csvContent.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) {
      return CsvParseResult(false, emptyList(), 0, 0, listOf("CSV content is empty."))
    }

    val approvers = mutableListOf<User>()
    val errors = mutableListOf<String>()

    // Identify header row index
    val startIndex = if (lines[0].contains("Owner Name", ignoreCase = true) || lines[0].contains("Flat", ignoreCase = true)) 1 else 0

    for (lineIndex in startIndex until lines.size) {
      val line = lines[lineIndex]
      val tokens = parseCsvLine(line)

      if (tokens.size < 3) {
        errors.add("Row ${lineIndex + 1}: Insufficient columns (expected at least 3: Owner Name, Flat Number, Block)")
        continue
      }

      val ownerName = tokens.getOrNull(0)?.takeIf { it.isNotBlank() }
      val flatNumber = tokens.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "101"
      val block = tokens.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Tower A"
      val mobile = tokens.getOrNull(3)?.takeIf { it.isNotBlank() } ?: "9876543220"
      val email = tokens.getOrNull(4)?.takeIf { it.isNotBlank() } ?: "${ownerName?.lowercase()?.replace(" ", "") ?: "owner"}@apartment.com"
      val designation = tokens.getOrNull(5)?.takeIf { it.isNotBlank() } ?: "$flatNumber, $block"

      if (ownerName.isNullOrBlank()) {
        errors.add("Row ${lineIndex + 1}: Missing owner name")
        continue
      }

      approvers.add(
        User(
          userId = "APP-" + UUID.randomUUID().toString().take(6).uppercase(),
          tenantId = tenantId,
          name = ownerName,
          email = email,
          username = ownerName.lowercase().replace(" ", "."),
          mobile = mobile,
          role = UserRole.APPROVER,
          designationOrFlat = designation
        )
      )
    }

    return CsvParseResult(
      success = approvers.isNotEmpty(),
      items = approvers,
      parsedCount = approvers.size,
      errorCount = errors.size,
      errors = errors
    )
  }

  /**
   * Generates a template file in cache and returns its content Uri for sharing or downloading.
   */
  fun createTemplateFile(context: Context, fileName: String, content: String): Uri? {
    return try {
      val exportDir = File(context.cacheDir, "csv_exports")
      if (!exportDir.exists()) exportDir.mkdirs()
      val file = File(exportDir, fileName)
      FileOutputStream(file).use { out ->
        out.write(content.toByteArray(Charsets.UTF_8))
      }
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
      null
    }
  }
}
