package com.gardiyan.app

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.gardiyan.app.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ar-rSA", sdk = [36])
class ProfileRtlTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `application theme preserves natural Arabic RTL direction`() {
        var observedDirection: LayoutDirection? = null

        composeRule.setContent {
            MyApplicationTheme {
                val direction = LocalLayoutDirection.current
                SideEffect { observedDirection = direction }
            }
        }
        composeRule.waitForIdle()

        assertEquals(LayoutDirection.Rtl, observedDirection)
    }
}
