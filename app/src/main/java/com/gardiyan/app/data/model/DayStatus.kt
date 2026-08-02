package com.gardiyan.app.data.model

enum class DayStatus {
    SUCCESS, FAILURE, PROGRESS, NONE;

    companion object {
        /** Bir günü "başarılı" olarak işaretleyen log türleri. */
        val SUCCESS_EVENT_TYPES = setOf("SUCCESS", "DAILY_SUCCESS", "SUCCESS_DAY")

        /** Bir günü "ihlal/başarısız" olarak işaretleyen log türleri. */
        val FAILURE_EVENT_TYPES = setOf(
            "FAILURE", "VIOLATION", "DAILY_FAILURE", "RESET_HOLD_5S", "CRITICAL_ACTION_COMPLETED",
            "RESTRICTION_REMOVED", "RESTRICTION_DELETED", "RESTRICTIONS_CLEARED"
        )

        /**
         * Bir günün disiplin kutusu durumunu belirler.
         * Hem 21 günlük özet (Dashboard) hem 100 günlük detay ekranı bu ortak mantığı kullanır;
         * böylece kutu renkleri her iki ekranda tutarlıdır.
         *
         * Kurallar:
         * - Gelecek gün                     -> NONE (gri)
         * - O güne ait ihlal logu varsa     -> FAILURE (kırmızı)
         * - O güne ait başarı logu varsa    -> SUCCESS (yeşil)
         * - Bugün:
         *     - aktif hedef yoksa           -> NONE (gri)
         *     - bugün ihlal varsa           -> FAILURE (kırmızı)
         *     - aksi halde                  -> SUCCESS (yeşil)
         *       (korunan uygulamalara hiç girilmese bile bugün temizse yeşil sayılır)
         * - Geçmiş gün:
         *     - aktif hedef varsa ve ihlal kaydı yoksa -> SUCCESS (yeşil)
         *       (uygulama o gün açılmamış olsa bile limit kuralı geçerlidir)
         *     - aktif hedef yoksa                      -> NONE (gri)
         */
        fun evaluate(
            isFuture: Boolean,
            isToday: Boolean,
            hasActiveTargets: Boolean,
            todayHasViolation: Boolean,
            dayHasSuccessLog: Boolean,
            dayHasFailureLog: Boolean
        ): DayStatus = when {
            isFuture -> NONE
            dayHasFailureLog -> FAILURE
            dayHasSuccessLog -> SUCCESS
            isToday -> when {
                !hasActiveTargets -> NONE
                todayHasViolation -> FAILURE
                else -> SUCCESS
            }
            hasActiveTargets -> SUCCESS
            else -> NONE
        }
    }
}
