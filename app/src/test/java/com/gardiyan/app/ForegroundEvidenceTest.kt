package com.gardiyan.app

import com.gardiyan.app.service.ForegroundPolicyEvaluator
import com.gardiyan.app.service.UsageEventRecord
import com.gardiyan.app.service.UsageStatsForegroundResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focus tests for foreground evidence rules:
 * 1. Polling/UsageStats cannot enter restricted target from null/unrelated state.
 * 2. Accessibility window change event can enter restricted target.
 * 3. Latest target PAUSED/STOPPED/MOVE_TO_BACKGROUND after RESUMED is not foreground.
 * 4. Later RESUMED event is foreground.
 */
class ForegroundEvidenceTest {

    @Test
    fun `polling cannot enter restricted target from null state`() {
        val eval = ForegroundPolicyEvaluator.evaluate(
            currentTrackedPackage = null,
            candidatePackage = "com.instagram.android",
            isCandidateRestrictedToday = true,
            isCandidateLimitExhaustedOrFailed = true,
            allowRestrictedEntry = false
        )

        assertFalse("Restricted entry must not be allowed from polling", eval.isRestrictedEntryAllowed)
        assertNull("Tracked package must remain null", eval.nextTrackedPackage)
        assertFalse("Must not start session", eval.shouldStartSession)
        assertFalse("Must not show lock overlay", eval.shouldShowLockOverlay)
    }

    @Test
    fun `polling cannot enter restricted target from unrelated state`() {
        val eval = ForegroundPolicyEvaluator.evaluate(
            currentTrackedPackage = "com.unrelated.app",
            candidatePackage = "com.instagram.android",
            isCandidateRestrictedToday = true,
            isCandidateLimitExhaustedOrFailed = false,
            allowRestrictedEntry = false
        )

        assertFalse("Restricted entry must not be allowed from polling", eval.isRestrictedEntryAllowed)
        assertNull("Tracked package must be reset to null instead of entering target", eval.nextTrackedPackage)
        assertTrue("Previous tracked session must be closed", eval.shouldCloseSession)
        assertFalse("Must not start session for new target", eval.shouldStartSession)
    }

    @Test
    fun `accessibility source can enter restricted target from null state`() {
        val eval = ForegroundPolicyEvaluator.evaluate(
            currentTrackedPackage = null,
            candidatePackage = "com.instagram.android",
            isCandidateRestrictedToday = true,
            isCandidateLimitExhaustedOrFailed = false,
            allowRestrictedEntry = true
        )

        assertTrue("Accessibility event must allow restricted entry", eval.isRestrictedEntryAllowed)
        assertEquals("com.instagram.android", eval.nextTrackedPackage)
        assertTrue("Must start new session", eval.shouldStartSession)
        assertFalse("Must not show overlay when limit remains", eval.shouldShowLockOverlay)
    }

    @Test
    fun `accessibility source immediately shows lock overlay when limit exhausted`() {
        val eval = ForegroundPolicyEvaluator.evaluate(
            currentTrackedPackage = null,
            candidatePackage = "com.instagram.android",
            isCandidateRestrictedToday = true,
            isCandidateLimitExhaustedOrFailed = true,
            allowRestrictedEntry = true
        )

        assertTrue("Accessibility event must allow restricted entry", eval.isRestrictedEntryAllowed)
        assertEquals("com.instagram.android", eval.nextTrackedPackage)
        assertTrue("Must start new session", eval.shouldStartSession)
        assertTrue("Must trigger lock overlay immediately", eval.shouldShowLockOverlay)
    }

    @Test
    fun `polling maintains already tracked exact package`() {
        val eval = ForegroundPolicyEvaluator.evaluate(
            currentTrackedPackage = "com.instagram.android",
            candidatePackage = "com.instagram.android",
            isCandidateRestrictedToday = true,
            isCandidateLimitExhaustedOrFailed = false,
            allowRestrictedEntry = false
        )

        assertTrue("Maintaining existing tracked package is allowed", eval.isRestrictedEntryAllowed)
        assertEquals("com.instagram.android", eval.nextTrackedPackage)
        assertFalse("Must not start duplicate new session", eval.shouldStartSession)
        assertTrue("Must update existing session", eval.shouldUpdateSession)
    }

    @Test
    fun `transition to unrestricted package closes tracked session and hides overlay`() {
        val eval = ForegroundPolicyEvaluator.evaluate(
            currentTrackedPackage = "com.instagram.android",
            candidatePackage = "com.miui.home",
            isCandidateRestrictedToday = false,
            isCandidateLimitExhaustedOrFailed = false,
            allowRestrictedEntry = false
        )

        assertNull("Next tracked package must be null", eval.nextTrackedPackage)
        assertTrue("Tracked session must be closed", eval.shouldCloseSession)
        assertTrue("Lock overlay must be hidden", eval.shouldHideLockOverlay)
    }

    @Test
    fun `latest target PAUSED after RESUMED is not foreground`() {
        val target = "com.instagram.android"
        val events = listOf(
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_ACTIVITY_RESUMED, 1000L),
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_ACTIVITY_PAUSED, 2000L)
        )

        val foreground = UsageStatsForegroundResolver.resolveForegroundPackage(events)
        assertNull("Target paused after resumed must yield no foreground target", foreground)
    }

    @Test
    fun `latest target STOPPED after RESUMED is not foreground`() {
        val target = "com.instagram.android"
        val events = listOf(
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_ACTIVITY_RESUMED, 1000L),
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_ACTIVITY_PAUSED, 1500L),
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_ACTIVITY_STOPPED, 2000L)
        )

        val foreground = UsageStatsForegroundResolver.resolveForegroundPackage(events)
        assertNull("Target stopped after resumed must yield no foreground target", foreground)
    }

    @Test
    fun `latest target MOVE_TO_BACKGROUND after MOVE_TO_FOREGROUND is not foreground`() {
        val target = "com.instagram.android"
        val events = listOf(
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_MOVE_TO_FOREGROUND, 1000L),
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_MOVE_TO_BACKGROUND, 2000L)
        )

        val foreground = UsageStatsForegroundResolver.resolveForegroundPackage(events)
        assertNull("Target moved to background must yield no foreground target", foreground)
    }

    @Test
    fun `later RESUMED is foreground`() {
        val target = "com.instagram.android"
        val events = listOf(
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_ACTIVITY_RESUMED, 1000L),
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_ACTIVITY_PAUSED, 1500L),
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_ACTIVITY_RESUMED, 3000L)
        )

        val foreground = UsageStatsForegroundResolver.resolveForegroundPackage(events)
        assertEquals("Target re-resumed later must be recognized as foreground", target, foreground)
    }

    @Test
    fun `unrelated app resumed after target paused resolves to unrelated app`() {
        val target = "com.instagram.android"
        val unrelated = "com.example.other"
        val events = listOf(
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_ACTIVITY_RESUMED, 1000L),
            UsageEventRecord(target, UsageStatsForegroundResolver.TYPE_ACTIVITY_PAUSED, 1500L),
            UsageEventRecord(unrelated, UsageStatsForegroundResolver.TYPE_ACTIVITY_RESUMED, 2000L)
        )

        val foreground = UsageStatsForegroundResolver.resolveForegroundPackage(events)
        assertEquals(unrelated, foreground)
    }

    @Test
    fun `empty events list resolves to null`() {
        val foreground = UsageStatsForegroundResolver.resolveForegroundPackage(emptyList())
        assertNull(foreground)
    }
}
