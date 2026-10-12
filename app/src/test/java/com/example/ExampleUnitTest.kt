package com.example

import com.example.data.SmartCheckRepository
import com.example.model.TenantType
import com.example.model.TicketStatus
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExampleUnitTest {
  @Test
  fun testCreateTenantAddsToMultipleTenantRegister() {
    val repository = SmartCheckRepository()
    val initialRegistrySize = repository.tenantRegistry.value.size

    val newTenant = repository.createTenant(
      name = "Sunrise Hospital",
      code = "SUNHOSP",
      type = TenantType.HOSPITAL,
      customCategory = null,
      assignedRole = "Hospital Facility Director",
      adminEmail = "director@sunrisehosp.org",
      contactName = "Dr. Robert Smith",
      contactMobile = "9845123456",
      hasReception = true,
      receptionStaffNames = listOf("Nurse Mary", "Nurse Clara")
    )

    assertEquals("SUNHOSP", newTenant.code)
    assertEquals("Hospital Facility Director", newTenant.assignedRole)
    assertTrue(newTenant.hasReception)
    assertEquals(2, newTenant.receptionStaffNames.size)

    val updatedRegistry = repository.tenantRegistry.value
    assertEquals(initialRegistrySize + 1, updatedRegistry.size)
    val entry = updatedRegistry.find { it.tenantCode == "SUNHOSP" }
    assertNotNull(entry)
    assertEquals("Hospital", entry!!.category)
    assertEquals("director@sunrisehosp.org", entry.email)
    assertEquals("Dr. Robert Smith", entry.contactPerson)
  }

  @Test
  fun testMonthlySpreadsheetUrlFormat() {
    val repository = SmartCheckRepository()
    val tenant = repository.tenants.value.first()
    val currentYearMonth = SimpleDateFormat("yyyy_MM", Locale.getDefault()).format(Date())

    val monthlyUrl = tenant.getMonthlySpreadsheetUrl(currentYearMonth)
    assertTrue(monthlyUrl.contains(currentYearMonth))
    assertTrue(monthlyUrl.contains(tenant.code))
    assertTrue(monthlyUrl.contains("1VHjoCvT43K4Z8niSppCXj1pjQzE8RkEL"))
  }

  @Test
  fun testReceptionToggleAndAnalytics() {
    val repository = SmartCheckRepository()
    val tenant = repository.tenants.value.first()

    // Test analytics calculation
    val analytics = repository.getTenantAnalytics(tenant.tenantId)
    assertNotNull(analytics.currentDate)
    assertTrue(analytics.monthTotalRequests >= 0)

    // Test reception toggle
    repository.toggleReception(tenant.tenantId, false)
    val updatedTenant = repository.tenants.value.find { it.tenantId == tenant.tenantId }
    assertNotNull(updatedTenant)
    assertFalse(updatedTenant!!.hasReception)
  }
}
