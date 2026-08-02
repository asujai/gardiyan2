package com.gardiyan.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionGateTest {

    @Test
    fun `first setup blocks main app until non accessibility requirements are granted`() {
        val hasRequiredPermissions = hasRequiredSetupPermissions(
            isOverlayEnabled = false,
            isUsageEnabled = true,
            isAccessibilityEnabled = false,
            isBatteryExempted = true
        )

        assertFalse(hasRequiredPermissions)
        assertFalse(
            canEnterMainApp(
                hasCompletedInitialPermissionGate = false,
                hasRequiredSetupPermissions = hasRequiredPermissions
            )
        )
        assertTrue(
            shouldShowInitialPermissionGate(
                hasCompletedInitialPermissionGate = false,
                hasRequiredSetupPermissions = hasRequiredPermissions
            )
        )
    }

    @Test
    fun `first setup lets user enter when only accessibility is inactive`() {
        val hasRequiredPermissions = hasRequiredSetupPermissions(
            isOverlayEnabled = true,
            isUsageEnabled = true,
            isAccessibilityEnabled = false,
            isBatteryExempted = true
        )

        assertTrue(hasRequiredPermissions)
        assertTrue(
            canEnterMainApp(
                hasCompletedInitialPermissionGate = false,
                hasRequiredSetupPermissions = hasRequiredPermissions
            )
        )
        assertFalse(
            shouldShowInitialPermissionGate(
                hasCompletedInitialPermissionGate = false,
                hasRequiredSetupPermissions = hasRequiredPermissions
            )
        )
    }

    @Test
    fun `completed setup lets user enter even if accessibility is later inactive`() {
        val hasRequiredPermissions = hasRequiredSetupPermissions(
            isOverlayEnabled = false,
            isUsageEnabled = true,
            isAccessibilityEnabled = false,
            isBatteryExempted = true
        )

        assertFalse(hasRequiredPermissions)
        assertTrue(
            canEnterMainApp(
                hasCompletedInitialPermissionGate = true,
                hasRequiredSetupPermissions = hasRequiredPermissions
            )
        )
        assertFalse(
            shouldShowInitialPermissionGate(
                hasCompletedInitialPermissionGate = true,
                hasRequiredSetupPermissions = hasRequiredPermissions
            )
        )
    }

    @Test
    fun `required permissions complete first setup gate`() {
        val hasRequiredPermissions = hasRequiredSetupPermissions(
            isOverlayEnabled = true,
            isUsageEnabled = true,
            isAccessibilityEnabled = true,
            isBatteryExempted = true
        )

        assertTrue(hasRequiredPermissions)
        assertTrue(
            canEnterMainApp(
                hasCompletedInitialPermissionGate = false,
                hasRequiredSetupPermissions = hasRequiredPermissions
            )
        )
        assertFalse(
            shouldShowInitialPermissionGate(
                hasCompletedInitialPermissionGate = false,
                hasRequiredSetupPermissions = hasRequiredPermissions
            )
        )
    }
}
