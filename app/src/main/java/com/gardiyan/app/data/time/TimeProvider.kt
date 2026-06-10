package com.gardiyan.app.data.time

interface TimeProvider {
    fun currentTimeMillis(): Long
    fun elapsedRealtime(): Long
    fun localDateString(): String
    fun todayDayLabel(): String
    fun timezoneId(): String
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
    
    override fun localDateString(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return dateFormat.format(java.util.Date(currentTimeMillis()))
    }
    
    override fun todayDayLabel(): String {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = currentTimeMillis()
        return when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "Pzt"
            java.util.Calendar.TUESDAY -> "Sal"
            java.util.Calendar.WEDNESDAY -> "Çar"
            java.util.Calendar.THURSDAY -> "Per"
            java.util.Calendar.FRIDAY -> "Cum"
            java.util.Calendar.SATURDAY -> "Cmt"
            java.util.Calendar.SUNDAY -> "Paz"
            else -> ""
        }
    }
    
    override fun timezoneId(): String = java.util.TimeZone.getDefault().id
}
