package com.gardiyan.app

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import com.gardiyan.app.viewmodel.ACTION_ACCESSIBILITY_DETAILS_SETTINGS
import com.gardiyan.app.viewmodel.accessibilitySettingsIntents
import com.gardiyan.app.viewmodel.batteryOptimizationSettingsIntents
import com.gardiyan.app.viewmodel.notificationSettingsIntents
import com.gardiyan.app.viewmodel.overlaySettingsIntents
import com.gardiyan.app.viewmodel.usageAccessSettingsIntents
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PermissionSettingsIntentsTest {

    @Test
    fun `usage access starts with package targeted screen and keeps safe fallbacks`() {
        val intents = usageAccessSettingsIntents("com.gardiyan.app")

        assertEquals(Settings.ACTION_USAGE_ACCESS_SETTINGS, intents.first().action)
        assertEquals("package:com.gardiyan.app", intents.first().dataString)
        assertEquals("com.gardiyan.app", intents.first().getStringExtra(Intent.EXTRA_PACKAGE_NAME))
        assertEquals(Settings.ACTION_SETTINGS, intents.last().action)
    }

    @Test
    fun `accessibility starts with service detail and falls back to highlighted list`() {
        val component = ComponentName("com.gardiyan.app", "com.gardiyan.app.TestService")
        val intents = accessibilitySettingsIntents(component)

        assertEquals(ACTION_ACCESSIBILITY_DETAILS_SETTINGS, intents[0].action)
        assertEquals(component, intents[0].getParcelableExtra(Intent.EXTRA_COMPONENT_NAME))
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, intents[1].action)
        assertEquals(component.flattenToString(), intents[1].getStringExtra(":settings:fragment_args_key"))
        assertEquals(Settings.ACTION_SETTINGS, intents.last().action)
    }

    @Test
    fun `overlay starts package targeted and battery starts with direct confirmation`() {
        val overlay = overlaySettingsIntents("com.gardiyan.app")
        val battery = batteryOptimizationSettingsIntents("com.gardiyan.app")

        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, overlay.first().action)
        assertEquals("package:com.gardiyan.app", overlay.first().dataString)
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, battery.first().action)
        assertEquals("package:com.gardiyan.app", battery.first().dataString)
        assertEquals(Settings.ACTION_SETTINGS, battery.last().action)
    }

    @Test
    fun `notification settings target Limitra and retain app details fallback`() {
        val intents = notificationSettingsIntents("com.gardiyan.app", 12345)

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intents.first().action)
        assertEquals("com.gardiyan.app", intents.first().getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertEquals(12345, intents.first().getIntExtra("app_uid", -1))
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intents[1].action)
    }
}
