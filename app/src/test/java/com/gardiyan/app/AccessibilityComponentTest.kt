package com.gardiyan.app

import com.gardiyan.app.viewmodel.isAccessibilityComponentEnabled
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * enabled_accessibility_services dizesinin ayrıştırılmasının birim testleri.
 * Daha önce erişilebilirlik durumu runtime heartbeat'ten okunuyordu ve process
 * yeni başladığında yanlış-negatif (sistemde AÇIK ama uygulamada KAPALI) veriyordu.
 */
class AccessibilityComponentTest {

    private val pkg = "com.gardiyan.app"
    private val cls = "com.gardiyan.app.service.AppBlockAccessibilityService"
    private val full = "$pkg/$cls"

    @Test
    fun `null or empty setting means disabled`() {
        assertFalse(isAccessibilityComponentEnabled(null, pkg, cls))
        assertFalse(isAccessibilityComponentEnabled("", pkg, cls))
        assertFalse(isAccessibilityComponentEnabled("   ", pkg, cls))
    }

    @Test
    fun `exact full component is detected as enabled`() {
        assertTrue(isAccessibilityComponentEnabled(full, pkg, cls))
    }

    @Test
    fun `component within a colon separated list of other services is detected`() {
        val setting = "com.other.app/com.other.app.Service:$full:com.third/com.third.A11y"
        assertTrue(isAccessibilityComponentEnabled(setting, pkg, cls))
    }

    @Test
    fun `short class form is also detected`() {
        // Bazı cihazlar kısa biçim saklayabilir: pkg/.Service
        val shortForm = "$pkg/.service.AppBlockAccessibilityService"
        assertTrue(isAccessibilityComponentEnabled(shortForm, pkg, cls))
    }

    @Test
    fun `unrelated services do not count as enabled`() {
        val setting = "com.other.app/com.other.app.Service:com.third/com.third.A11y"
        assertFalse(isAccessibilityComponentEnabled(setting, pkg, cls))
    }

    @Test
    fun `different package with same class name is not a false match`() {
        val setting = "com.evil.app/$cls"
        assertFalse(isAccessibilityComponentEnabled(setting, pkg, cls))
    }
}
