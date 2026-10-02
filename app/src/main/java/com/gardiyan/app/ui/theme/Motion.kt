package com.gardiyan.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Uygulama genelinde ortak hareket eğrileri. */
object LimitraMotion {
    /** Basma/bırakma: hızlı, hafif yaylı. */
    fun <T> press() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)

    /** Kart, sayfa ve içerik girişleri. */
    fun <T> settle() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)

    /** İlerleme halkaları ve sayaçlar. */
    fun <T> progress() = tween<T>(durationMillis = 900, easing = androidx.compose.animation.core.FastOutSlowInEasing)
}

/**
 * Dokunma geri bildirimi: basılan öğe birkaç piksel küçülür ve üzerine çok hafif bir
 * mürekkep tonu düşer. Material dalgası yerine `.clickable {}` kullanan tüm satırlara
 * tema üzerinden uygulanır.
 */
object PressScaleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressScaleNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = javaClass.hashCode()
}

private class PressScaleNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {

    private val pressed = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                val target = when (interaction) {
                    is PressInteraction.Press -> 1f
                    is PressInteraction.Release, is PressInteraction.Cancel -> 0f
                    else -> return@collect
                }
                launch { pressed.animateTo(target, LimitraMotion.press()) }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val p = pressed.value
        if (p <= 0.001f) {
            drawContent()
            return
        }
        // Büyük satırlar da küçükler de yaklaşık aynı piksel kadar içe çekilir.
        val shrinkPx = 5.dp.toPx() * p
        val longest = maxOf(size.width, size.height).coerceAtLeast(1f)
        val factor = (1f - shrinkPx / longest).coerceIn(0.94f, 1f)
        scale(factor) {
            this@draw.drawContent()
        }
        drawRect(color = PureBlack.copy(alpha = 0.045f * p))
    }
}
