package com.gardiyan.app.data.achievements

import android.content.Context

/**
 * Kesintisiz başarı serisiyle kazanılan profil çerçeveleri.
 *
 * Seri, mevcut seviye sistemiyle aynı kaynaktan gelir: `UserSessionEntity.consecutiveSuccessDays`
 * (gün sonu başarı değerlendirmesinde +1, ihlal/silmede 0). 3, 7, 15, 30 ve 60 günlük eşikler
 * seviye eşikleriyle örtüşür; 1, 100, 180 ve 365 ek motivasyon basamaklarıdır.
 *
 * Bir çerçeve bir kez kazanıldıktan sonra kalıcıdır: seri kırılsa bile geri alınmaz. Bunun için
 * ulaşılan en uzun seri ayrıca saklanır. Veritabanına dokunulmaz; kayıt yerel tercihlerdedir.
 */
enum class FrameTier(val requiredDays: Int) {
    SPARK(1),
    BRONZE(3),
    SILVER_LAUREL(7),
    GOLD_SEAL(15),
    MOONSTONE(30),
    OBSIDIAN_CROWN(60),
    CENTURY_FLAME(100),
    AURORA(180),
    STOIC(365)
}

data class AchievementProgress(
    val currentStreak: Int,
    val bestStreak: Int,
    val unlocked: List<FrameTier>,
    /** Henüz kazanılmamış ilk çerçeve; hepsi kazanıldıysa null. */
    val next: FrameTier?,
    /** Mevcut seriyle bir sonraki çerçeveye kalan gün. */
    val daysToNext: Int,
    /** Mevcut serinin bir sonraki çerçeveye oranı (0..1). */
    val progressToNext: Float
) {
    fun isUnlocked(tier: FrameTier): Boolean = tier in unlocked
    val highestUnlocked: FrameTier? get() = unlocked.maxByOrNull { it.requiredDays }
}

object Achievements {

    fun progress(currentStreak: Int, storedBestStreak: Int): AchievementProgress {
        val current = currentStreak.coerceAtLeast(0)
        val best = maxOf(current, storedBestStreak.coerceAtLeast(0))
        val unlocked = FrameTier.entries.filter { best >= it.requiredDays }
        val next = FrameTier.entries.firstOrNull { best < it.requiredDays }
        val daysToNext = next?.let { (it.requiredDays - current).coerceAtLeast(0) } ?: 0
        val ratio = next?.let { (current.toFloat() / it.requiredDays).coerceIn(0f, 1f) } ?: 1f
        return AchievementProgress(
            currentStreak = current,
            bestStreak = best,
            unlocked = unlocked,
            next = next,
            daysToNext = daysToNext,
            progressToNext = ratio
        )
    }

    /** Kullanıcı seçmediyse en yüksek kazanılmış çerçeve takılır. */
    fun resolveEquipped(progress: AchievementProgress, chosen: FrameTier?): FrameTier? =
        chosen?.takeIf { progress.isUnlocked(it) } ?: progress.highestUnlocked

    /** Kazanılmış ama kutlaması henüz gösterilmemiş çerçeveler (en yüksek önce). */
    fun pendingCelebrations(progress: AchievementProgress, seen: Set<FrameTier>): List<FrameTier> =
        progress.unlocked.filterNot { it in seen }.sortedByDescending { it.requiredDays }
}

/** Başarı kayıtlarının yerel deposu. */
class AchievementStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val bestStreak: Int get() = prefs.getInt(KEY_BEST_STREAK, 0)

    /** Mevcut seriyi kaydeder; en uzun seri yalnız artar. */
    fun record(currentStreak: Int): AchievementProgress {
        val progress = Achievements.progress(currentStreak, bestStreak)
        if (progress.bestStreak != bestStreak) {
            prefs.edit().putInt(KEY_BEST_STREAK, progress.bestStreak).apply()
        }
        return progress
    }

    var chosenFrame: FrameTier?
        get() = prefs.getString(KEY_EQUIPPED, null)?.let { name ->
            FrameTier.entries.firstOrNull { it.name == name }
        }
        set(value) {
            prefs.edit().putString(KEY_EQUIPPED, value?.name).apply()
        }

    val celebrated: Set<FrameTier>
        get() = prefs.getStringSet(KEY_CELEBRATED, emptySet()).orEmpty()
            .mapNotNull { name -> FrameTier.entries.firstOrNull { it.name == name } }
            .toSet()

    fun markCelebrated(tiers: Collection<FrameTier>) {
        val merged = (celebrated + tiers).map { it.name }.toSet()
        prefs.edit().putStringSet(KEY_CELEBRATED, merged).apply()
    }

    companion object {
        const val PREFS_NAME = "limitra_achievements"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_EQUIPPED = "equipped_frame"
        private const val KEY_CELEBRATED = "celebrated_frames"
    }
}
