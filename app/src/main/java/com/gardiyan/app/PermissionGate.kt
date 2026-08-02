package com.gardiyan.app

import android.content.Context

private const val SETTINGS_PREFS_NAME = "gardiyan_settings"
private const val KEY_INITIAL_PERMISSION_GATE_COMPLETED = "initial_permission_gate_completed"
private const val KEY_INITIAL_PERMISSION_GATE_LEGACY_MIGRATED = "initial_permission_gate_legacy_migrated"
private const val KEY_LEGACY_ACCESSIBILITY_APPROVED = "accessibility_approved"

internal fun hasRequiredSetupPermissions(
    isOverlayEnabled: Boolean,
    isUsageEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    isBatteryExempted: Boolean
): Boolean {
    // Accessibility is shown as a health/status warning, but it must not block app entry.
    return isOverlayEnabled && isUsageEnabled && isBatteryExempted
}

internal fun canEnterMainApp(
    hasCompletedInitialPermissionGate: Boolean,
    hasRequiredSetupPermissions: Boolean
): Boolean = hasCompletedInitialPermissionGate || hasRequiredSetupPermissions

internal fun shouldShowInitialPermissionGate(
    hasCompletedInitialPermissionGate: Boolean,
    hasRequiredSetupPermissions: Boolean
): Boolean = !canEnterMainApp(
    hasCompletedInitialPermissionGate = hasCompletedInitialPermissionGate,
    hasRequiredSetupPermissions = hasRequiredSetupPermissions
)

internal fun isInitialPermissionGateCompleted(context: Context): Boolean {
    val prefs = context.applicationContext.getSharedPreferences(
        SETTINGS_PREFS_NAME,
        Context.MODE_PRIVATE
    )
    if (prefs.getBoolean(KEY_INITIAL_PERMISSION_GATE_COMPLETED, false)) {
        return true
    }

    if (!prefs.getBoolean(KEY_INITIAL_PERMISSION_GATE_LEGACY_MIGRATED, false)) {
        val legacyCompleted = prefs.getBoolean(KEY_LEGACY_ACCESSIBILITY_APPROVED, false)
        prefs.edit()
            .putBoolean(KEY_INITIAL_PERMISSION_GATE_LEGACY_MIGRATED, true)
            .putBoolean(KEY_INITIAL_PERMISSION_GATE_COMPLETED, legacyCompleted)
            .apply()
        return legacyCompleted
    }

    return false
}

internal fun markInitialPermissionGateCompleted(context: Context) {
    context.applicationContext.getSharedPreferences(
        SETTINGS_PREFS_NAME,
        Context.MODE_PRIVATE
    ).edit()
        .putBoolean(KEY_INITIAL_PERMISSION_GATE_COMPLETED, true)
        .putBoolean(KEY_INITIAL_PERMISSION_GATE_LEGACY_MIGRATED, true)
        .apply()
}
