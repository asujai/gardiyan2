package com.gardiyan.app

import com.gardiyan.app.service.AccessibilityHealthMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityHealthMonitorTest {

    @Test
    fun `missing heartbeat has no age`() {
        assertNull(
            AccessibilityHealthMonitor.heartbeatAgeMillis(
                nowWallMillis = 10_000L,
                nowElapsedRealtime = 10_000L,
                lastWallMillis = 0L,
                lastElapsedRealtime = 0L
            )
        )
    }

    @Test
    fun `heartbeat age prefers elapsed realtime on same boot`() {
        val age = AccessibilityHealthMonitor.heartbeatAgeMillis(
            nowWallMillis = 100_000L,
            nowElapsedRealtime = 50_000L,
            lastWallMillis = 80_000L,
            lastElapsedRealtime = 45_000L
        )

        assertEquals(5_000L, age)
    }

    @Test
    fun `heartbeat age falls back to wall clock after reboot`() {
        val age = AccessibilityHealthMonitor.heartbeatAgeMillis(
            nowWallMillis = 100_000L,
            nowElapsedRealtime = 1_000L,
            lastWallMillis = 80_000L,
            lastElapsedRealtime = 45_000L
        )

        assertEquals(20_000L, age)
    }

    @Test
    fun `stale threshold treats old heartbeat as unhealthy`() {
        assertTrue(AccessibilityHealthMonitor.isFreshHeartbeatAge(29_999L, 30_000L))
        assertFalse(AccessibilityHealthMonitor.isFreshHeartbeatAge(30_001L, 30_000L))
        assertFalse(AccessibilityHealthMonitor.isFreshHeartbeatAge(null, 30_000L))
    }

    @Test
    fun `fail safe protection requires active and fresh fallback heartbeat`() {
        val activeStatus = AccessibilityHealthMonitor.Status(
            isPermissionGranted = true,
            isServiceHeartbeatFresh = false,
            isTrackingHeartbeatFresh = false,
            isFallbackProtectionActive = true,
            isFallbackProtectionFresh = true,
            serviceHeartbeatAgeMillis = 45_000L,
            trackingHeartbeatAgeMillis = 45_000L,
            fallbackProtectionHeartbeatAgeMillis = 1_000L,
            isServiceMarkedBound = false
        )
        val staleStatus = activeStatus.copy(isFallbackProtectionFresh = false)

        assertTrue(activeStatus.requiresReenable)
        assertTrue(activeStatus.hasFailSafeProtection)
        assertFalse(staleStatus.hasFailSafeProtection)
    }
}
