package com.gardiyan.app.data.model

/**
 * Ana ekrandaki 21 kutucuklu Disiplin Özeti'nin "kayan pencere" matematiği.
 *
 * Kutular, aktif 100 günlük serinin görünen 21 günlük penceresidir:
 * - İlk 21 gün boyunca pencere serinin 1. gününe sabitlenir; bugün 1, 2, 3 ... 21.
 *   kutuda ilerler (sol üst = 1. gün, sağ alt = 21. gün).
 * - 21. günden sonra pencere kayar ("son 21 gün"); bugün artık sağ alttaki
 *   son kutuda (index 20) kalır, eski günler geride kalır.
 *
 * Saf (Android bağımsız) olduğu için birim testi yazılabilir.
 */
object DisciplineWindow {

    const val SUMMARY_DAYS = 21

    /**
     * Görünen pencerenin ilk gün numarası (1 tabanlı seri günü).
     * @param todayDayNumber bugünün seri içindeki gün numarası (1 = serinin ilk günü).
     */
    fun firstVisibleDayNumber(todayDayNumber: Int, windowSize: Int = SUMMARY_DAYS): Int =
        maxOf(1, todayDayNumber - (windowSize - 1))

    /**
     * Bugünün pencere içindeki kutu indeksi (0 tabanlı; 0 = sol üst).
     * İlk 21 günde todayDayNumber-1, sonrasında windowSize-1 (sağ alt) olur.
     */
    fun todayCellIndex(todayDayNumber: Int, windowSize: Int = SUMMARY_DAYS): Int =
        minOf(windowSize - 1, todayDayNumber - 1)

    /**
     * Belirli bir kutu indeksine düşen seri gün numarası (1 tabanlı).
     * Bu numara > todayDayNumber ise o kutu gelecektir (henüz gelmemiş gün).
     */
    fun dayNumberForCell(cellIndex: Int, todayDayNumber: Int, windowSize: Int = SUMMARY_DAYS): Int =
        firstVisibleDayNumber(todayDayNumber, windowSize) + cellIndex
}
