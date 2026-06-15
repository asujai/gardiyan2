package com.gardiyan.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gardiyan.app.ui.theme.DashboardBorder
import com.gardiyan.app.ui.theme.DashboardInk

/**
 * Disiplin ızgaralarındaki tek bir gün kutucuğu. Hem ana ekran 21 kutucuklu özet
 * hem 100 kutucuklu detay ekranı bu ortak bileşeni kullanır; böylece "bugün" vurgusu
 * iki ekranda tutarlıdır.
 *
 * - İç dolgu rengi (fillColor) DEĞİŞMEZ: başarılı=yeşil, başarısız=kırmızı, boş=gri.
 * - "Bugün" kutusu yalnızca biraz daha belirgin, temaya uyumlu ince bir çerçeveyle
 *   ayırt edilir (DashboardInk açık temada koyu, koyu temada açık olduğu için her iki
 *   temada da kontrast verir). Parlama/animasyon yoktur, sade kalır.
 * - Erişilebilirlik: bugün kutusuna "Bugün" içerik açıklaması eklenir.
 */
@Composable
fun DisciplineDayBox(
    fillColor: Color,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 6.dp,
    todayContentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isToday) DashboardInk.copy(alpha = 0.6f) else DashboardBorder.copy(alpha = 0.4f)
    val borderWidth = if (isToday) 1.5.dp else 0.5.dp

    val highlightModifier = if (isToday && todayContentDescription != null) {
        modifier.semantics { contentDescription = todayContentDescription }
    } else {
        modifier
    }

    Box(
        modifier = highlightModifier
            .clip(shape)
            .background(fillColor)
            .border(borderWidth, borderColor, shape),
        contentAlignment = Alignment.Center,
        content = content
    )
}
