package com.gardiyan.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
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
import com.gardiyan.app.ui.theme.CopperAccent

/**
 * Disiplin ızgaralarındaki tek bir gün kutucuğu. Hem ana ekran 21 kutucuklu özet
 * hem 100 kutucuklu detay ekranı bu ortak bileşeni kullanır; böylece "bugün" vurgusu
 * iki ekranda tutarlıdır.
 *
 * - İç dolgu rengi (fillColor) DEĞİŞMEZ: başarılı=yeşil, başarısız=kırmızı, boş=gri.
 * - "Bugün" kutusu, accent renginde yavaşça nefes alan bir halkayla ayırt edilir;
 *   dolgu rengi değişmez.
 * - Erişilebilirlik: bugün kutusuna "Bugün" içerik açıklaması eklenir.
 */
@Composable
fun DisciplineDayBox(
    fillColor: Color,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 7.dp,
    todayContentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val shape = RoundedCornerShape(cornerRadius)
    val highlightModifier = if (isToday && todayContentDescription != null) {
        modifier.semantics { contentDescription = todayContentDescription }
    } else {
        modifier
    }

    if (isToday) {
        // Bugün: dolgu aynı kalır, çevresinde yavaşça nefes alan vurgu halkası belirir.
        val pulse by rememberInfiniteTransition(label = "today").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
            label = "todayPulse"
        )
        Box(
            modifier = highlightModifier
                .border(2.dp, CopperAccent.copy(alpha = pulse), shape)
                .padding(3.dp)
                .clip(RoundedCornerShape((cornerRadius - 2.dp).coerceAtLeast(2.dp)))
                .background(fillColor),
            contentAlignment = Alignment.Center,
            content = content
        )
    } else {
        Box(
            modifier = highlightModifier
                .clip(shape)
                .background(fillColor)
                .border(0.5.dp, DashboardBorder.copy(alpha = 0.35f), shape),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
