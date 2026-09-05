package com.gardiyan.app

import com.gardiyan.app.service.ForegroundPolicyEvaluator
import com.gardiyan.app.service.OverlayDismissPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kilit ekranının "yalnızca butonla kalkar" kuralının testleri.
 *
 * Düzeltilen hata: kullanıcı alttan yukarı çekip uygulamayı arka plana atmayı
 * yarıda bırakınca kilit ekranı kalkıyor ve geri dönüşte yeniden gelmiyordu;
 * böylece kısıtlama tamamen atlatılabiliyordu.
 */
class OverlayDismissPolicyTest {

    // ------------------------------------------------------------------
    // 1. Yapışkan kilit: ön plan değişimi kilidi kaldıramaz
    // ------------------------------------------------------------------

    @Test
    fun `foreground change cannot dismiss sticky lock`() {
        assertTrue(
            "Yapışkan kilit açıkken yumuşak kaldırma isteği yok sayılmalı",
            OverlayDismissPolicy.shouldIgnoreSoftHide(requiresManualDismiss = true)
        )
        assertFalse(
            "Ana ekrana çekme kilidi kaldırmamalı",
            OverlayDismissPolicy.willDismiss(requiresManualDismiss = true, isForced = false)
        )
    }

    @Test
    fun `return home button dismisses sticky lock`() {
        assertTrue(
            "'Ana sayfaya dön' butonu kilidi kaldırmalı",
            OverlayDismissPolicy.willDismiss(requiresManualDismiss = true, isForced = true)
        )
    }

    @Test
    fun `legitimate paths dismiss sticky lock`() {
        // MainActivity öne gelmesi, kısıtlamanın silinmesi, aktif kısıtlama
        // kalmaması ve verilerin temizlenmesi zorunlu (forced) yollardır.
        assertTrue(
            OverlayDismissPolicy.willDismiss(requiresManualDismiss = true, isForced = true)
        )
    }

    @Test
    fun `soft hide works when lock is not sticky`() {
        assertFalse(OverlayDismissPolicy.shouldIgnoreSoftHide(requiresManualDismiss = false))
        assertTrue(
            OverlayDismissPolicy.willDismiss(requiresManualDismiss = false, isForced = false)
        )
    }

    // ------------------------------------------------------------------
    // 2. Geri dönüş açığı: süresi dolmuş uygulamaya dönüşte kilit geri gelir
    // ------------------------------------------------------------------

    @Test
    fun `exhausted app is re-locked when active window confirms foreground`() {
        val eval = ForegroundPolicyEvaluator.evaluate(
            currentTrackedPackage = null,
            candidatePackage = "com.instagram.android",
            isCandidateRestrictedToday = true,
            isCandidateLimitExhaustedOrFailed = true,
            allowRestrictedEntry = false,
            isForegroundConfirmedByActiveWindow = true
        )

        assertTrue(
            "Canlı pencere teyidi varsa süresi dolmuş uygulama yeniden kilitlenmeli",
            eval.shouldShowLockOverlay
        )
    }

    @Test
    fun `exhausted app is not re-locked without active window confirmation`() {
        val eval = ForegroundPolicyEvaluator.evaluate(
            currentTrackedPackage = null,
            candidatePackage = "com.instagram.android",
            isCandidateRestrictedToday = true,
            isCandidateLimitExhaustedOrFailed = true,
            allowRestrictedEntry = false,
            isForegroundConfirmedByActiveWindow = false
        )

        assertFalse(
            "Teyit yoksa bayat UsageStats verisiyle kilit gösterilmemeli",
            eval.shouldShowLockOverlay
        )
    }

    @Test
    fun `app with remaining time is re-tracked when active window confirms foreground`() {
        // Regresyon: bu kural eskiden "süresi kalan hedef polling ile izlemeye
        // alınmasın" şeklindeydi. Araya sistem arayüzü girip izleme koptuğunda
        // oturum bir daha kurulamıyor, süre düşülmüyor ve sayaç kurulmadığı için
        // kilit hiç gelmiyordu. Canlı pencere teyidi bayat UsageStats verisinden
        // farklıdır; teyit varsa izleme yeniden kurulmalıdır.
        val eval = ForegroundPolicyEvaluator.evaluate(
            currentTrackedPackage = null,
            candidatePackage = "com.instagram.android",
            isCandidateRestrictedToday = true,
            isCandidateLimitExhaustedOrFailed = false,
            allowRestrictedEntry = false,
            isForegroundConfirmedByActiveWindow = true
        )

        assertTrue(
            "Canlı pencere teyidi varsa izleme yeniden kurulmalı",
            eval.isRestrictedEntryAllowed
        )
        assertTrue("Kopan oturum yeniden başlatılmalı", eval.shouldStartSession)
        assertFalse(eval.shouldShowLockOverlay)
    }

    @Test
    fun `unrestricted app is never locked`() {
        val eval = ForegroundPolicyEvaluator.evaluate(
            currentTrackedPackage = "com.instagram.android",
            candidatePackage = "com.android.launcher",
            isCandidateRestrictedToday = false,
            isCandidateLimitExhaustedOrFailed = false,
            allowRestrictedEntry = true,
            isForegroundConfirmedByActiveWindow = true
        )

        assertFalse(eval.shouldShowLockOverlay)
        assertTrue("Oturum kapatılmalı", eval.shouldCloseSession)
    }
}
