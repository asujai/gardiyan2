package com.gardiyan.app.viewmodel

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings

internal const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS =
    "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"

private fun packageUri(packageName: String): Uri = Uri.parse("package:$packageName")

internal fun usageAccessSettingsIntents(packageName: String): List<Intent> = listOf(
    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        data = packageUri(packageName)
        putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
    },
    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(packageName)),
    Intent(Settings.ACTION_SETTINGS)
)

internal fun overlaySettingsIntents(packageName: String): List<Intent> = listOf(
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(packageName)).apply {
        putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
    },
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(packageName)),
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
    Intent(Settings.ACTION_SETTINGS)
)

internal fun accessibilitySettingsIntents(componentName: ComponentName): List<Intent> {
    val flattenedComponent = componentName.flattenToString()
    val fragmentArgs = android.os.Bundle().apply {
        putString(":settings:fragment_args_key", flattenedComponent)
    }

    return listOf(
        Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
            putExtra(Intent.EXTRA_COMPONENT_NAME, componentName)
        },
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            putExtra(":settings:fragment_args_key", flattenedComponent)
            putExtra(":settings:show_fragment_args", fragmentArgs)
        },
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(componentName.packageName)),
        Intent(Settings.ACTION_SETTINGS)
    )
}

internal fun batteryOptimizationSettingsIntents(packageName: String): List<Intent> = listOf(
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(packageName)),
    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(packageName)),
    Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
    Intent(Settings.ACTION_SETTINGS)
)

internal fun notificationSettingsIntents(packageName: String, uid: Int): List<Intent> = listOf(
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        putExtra("app_package", packageName)
        putExtra("app_uid", uid)
    },
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(packageName)),
    Intent(Settings.ACTION_SETTINGS)
)
