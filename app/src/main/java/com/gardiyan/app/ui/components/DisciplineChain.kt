package com.gardiyan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gardiyan.app.data.model.DayStatus
import com.gardiyan.app.ui.theme.CopperAccent
import com.gardiyan.app.ui.theme.SuccessGreen

/**
 * İki disiplin kutusu arasındaki bağlantının türü.
 *
 * - [NONE]  : bağ yok. Zincir burada kopmuştur (ihlal, boş gün veya hafta başı).
 * - [FORGED]: iki başarılı gün arasındaki tamamlanmış halka (yeşil).
 * - [LIVE]  : devam eden güne bağlanan halka (bakır). Zincir hâlâ canlıdır ama
 *             bugünün sonucu henüz kesinleşmemiştir.
 */
enum class ChainLink { NONE, FORGED, LIVE }

/**
 * Disiplin ızgarasındaki zincir kurallarını taşıyan saf (Compose'suz) mantık.
 *
 * Amaç motivasyon: ardışık başarılı günler görsel olarak birbirine bağlanır,
 * kullanıcı "zinciri kırmamak" ister. Bir gün ihlal edilirse ya da hiç hedef
 * yoksa halka çizilmez ve kopuş gözle görünür.
 *
 * Satır sonlarında bağ kurulmaz: her satır bir haftadır, hafta sınırı doğal bir
 * ayraçtır. Bu sayede ızgara sade kalır, çapraz/kıvrımlı çizgi gürültüsü olmaz.
 */
object DisciplineChain {

    /** Bir kutunun zincire katılmaya uygun olup olmadığı. */
    private fun isChainable(status: DayStatus): Boolean =
        status == DayStatus.SUCCESS || status == DayStatus.PROGRESS

    /**
     * Yan yana iki gün arasındaki halkayı belirler.
     *
     * @param left ızgarada solda duran (daha erken) gün
     * @param right ızgarada sağda duran (daha geç) gün
     */
    fun link(left: DayStatus, right: DayStatus): ChainLink {
        if (!isChainable(left) || !isChainable(right)) return ChainLink.NONE
        return if (left == DayStatus.SUCCESS && right == DayStatus.SUCCESS) {
            ChainLink.FORGED
        } else {
            ChainLink.LIVE
        }
    }

    /**
     * Bir satırdaki (hafta) en uzun kesintisiz zincirin uzunluğu; kutu sayısı
     * cinsinden. Tek başına duran başarılı gün 1 sayılır, hiç yoksa 0 döner.
     */
    fun longestRunInRow(row: List<DayStatus>): Int {
        var best = 0
        var current = 0
        for (status in row) {
            if (isChainable(status)) {
                current += 1
                if (current > best) best = current
            } else {
                current = 0
            }
        }
        return best
    }
}

/**
 * İki gün kutusu arasına çizilen ince halka.
 *
 * Kutular arasındaki boşluğun yerini alır; bu yüzden [width] ızgaranın eski
 * boşluk değeriyle aynı verilmelidir, böylece yerleşim ölçüleri değişmez.
 * Bağ yoksa boşluk aynen korunur (görünmez ayırıcı).
 */
@Composable
fun DisciplineChainLink(
    link: ChainLink,
    modifier: Modifier = Modifier,
    width: Dp = 8.dp,
    thickness: Dp = 3.dp
) {
    if (link == ChainLink.NONE) {
        Box(modifier = modifier.width(width))
        return
    }

    val color = if (link == ChainLink.FORGED) SuccessGreen else CopperAccent
    Box(
        modifier = modifier
            .width(width)
            .height(thickness)
            .clip(RoundedCornerShape(percent = 50))
            .background(color)
    )
}
