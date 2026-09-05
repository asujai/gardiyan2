package com.gardiyan.app

import com.gardiyan.app.service.CountdownPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regresyon: kısıtlı uygulamanın kendi içindeki gezinme (video değişimi, tam
 * ekran, dialog) da TYPE_WINDOW_STATE_CHANGED üretir. Bu olayların her birinde
 * geri sayım kalan sürenin tamamıyla yeniden kurulursa sayaç hiç dolmaz ve
 * kilit ekranı yalnız Limitra açılıp kapandıktan sonra gelir.
 */
class CountdownRestartTest {

    @Test
    fun `yeni girişte sayaç kurulur`() {
        assertTrue(
            CountdownPolicy.shouldRestartCountdown(isNewEntry = true, isCountdownRunning = false)
        )
    }

    @Test
    fun `aynı uygulama içindeki pencere olayı çalışan sayacı sıfırlamaz`() {
        assertFalse(
            "Çalışan sayaç aynı uygulamadaki pencere olayında yeniden kurulmamalı",
            CountdownPolicy.shouldRestartCountdown(isNewEntry = false, isCountdownRunning = true)
        )
    }

    @Test
    fun `sayaç düşmüşse aynı uygulamada yeniden kurulur`() {
        assertTrue(
            "Sayaç yoksa kilit hiç tetiklenmez, yeniden kurulmalı",
            CountdownPolicy.shouldRestartCountdown(isNewEntry = false, isCountdownRunning = false)
        )
    }

    @Test
    fun `yeni giriş çalışan sayacı devralır`() {
        assertTrue(
            "Başka bir hedeften geçişte sayaç yeni uygulamaya göre kurulmalı",
            CountdownPolicy.shouldRestartCountdown(isNewEntry = true, isCountdownRunning = true)
        )
    }
}
