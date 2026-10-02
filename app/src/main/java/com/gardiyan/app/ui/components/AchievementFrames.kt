package com.gardiyan.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.gardiyan.app.R
import com.gardiyan.app.data.achievements.FrameTier
import com.gardiyan.app.ui.theme.BorderGray
import com.gardiyan.app.ui.theme.CopperAccent
import com.gardiyan.app.ui.theme.DarkCharcoal
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Bir çerçevenin malzeme renkleri ve iç madalyon zemini. */
@Immutable
data class FrameLook(
    val metal: List<Color>,
    val accent: Color,
    val innerTop: Color,
    val innerBottom: Color,
    val content: Color
)

fun FrameTier.look(): FrameLook = when (this) {
    FrameTier.SPARK -> FrameLook(
        metal = listOf(Color(0xFFB9703F), Color(0xFFF2B07A), Color(0xFF9A5A31), Color(0xFFE8A06A), Color(0xFFB9703F)),
        accent = Color(0xFFFFC48A),
        innerTop = Color(0xFF3A2416), innerBottom = Color(0xFF1C110A), content = Color(0xFFFBE3CC)
    )
    FrameTier.BRONZE -> FrameLook(
        metal = listOf(Color(0xFF7A4A28), Color(0xFFE0A46C), Color(0xFF6B3F21), Color(0xFFC88A55), Color(0xFF7A4A28)),
        accent = Color(0xFFE9B583),
        innerTop = Color(0xFF3B2618), innerBottom = Color(0xFF170E08), content = Color(0xFFF6DEC6)
    )
    FrameTier.SILVER_LAUREL -> FrameLook(
        metal = listOf(Color(0xFF8E99A6), Color(0xFFF6F8FB), Color(0xFF6E7884), Color(0xFFDDE3EA), Color(0xFF8E99A6)),
        accent = Color(0xFFE9EEF4),
        innerTop = Color(0xFF2B323B), innerBottom = Color(0xFF12161B), content = Color(0xFFF1F4F8)
    )
    FrameTier.GOLD_SEAL -> FrameLook(
        metal = listOf(Color(0xFFA77A26), Color(0xFFFBE29A), Color(0xFF8A6119), Color(0xFFEFC867), Color(0xFFA77A26)),
        accent = Color(0xFFFFE7A6),
        innerTop = Color(0xFF3A2C10), innerBottom = Color(0xFF181105), content = Color(0xFFFCEFC9)
    )
    FrameTier.MOONSTONE -> FrameLook(
        metal = listOf(Color(0xFF9FB4FF), Color(0xFFF3EEFF), Color(0xFF8ED8EC), Color(0xFFD9C8FF), Color(0xFF9FB4FF)),
        accent = Color(0xFFE6EEFF),
        innerTop = Color(0xFF1F2547), innerBottom = Color(0xFF0B0E22), content = Color(0xFFEFF2FF)
    )
    FrameTier.OBSIDIAN_CROWN -> FrameLook(
        metal = listOf(Color(0xFF15151A), Color(0xFF3A3A44), Color(0xFF0E0E12), Color(0xFF2C2C34), Color(0xFF15151A)),
        accent = Color(0xFFE6BA60),
        innerTop = Color(0xFF1C1B20), innerBottom = Color(0xFF060608), content = Color(0xFFF2D59A)
    )
    FrameTier.CENTURY_FLAME -> FrameLook(
        metal = listOf(Color(0xFFC62A1E), Color(0xFFFFB347), Color(0xFFE5481F), Color(0xFFFFD27A), Color(0xFFC62A1E)),
        accent = Color(0xFFFFC266),
        innerTop = Color(0xFF3D130B), innerBottom = Color(0xFF160604), content = Color(0xFFFFE4C7)
    )
    FrameTier.AURORA -> FrameLook(
        metal = listOf(Color(0xFF3CF0B4), Color(0xFF4AA8FF), Color(0xFFA86BFF), Color(0xFFFF7AD9), Color(0xFF3CF0B4)),
        accent = Color(0xFFB8FFE9),
        innerTop = Color(0xFF0F2433), innerBottom = Color(0xFF050B14), content = Color(0xFFE6FFF7)
    )
    FrameTier.STOIC -> FrameLook(
        metal = listOf(Color(0xFFCFC6B4), Color(0xFFFFFBF0), Color(0xFFB59A62), Color(0xFFF1E3BE), Color(0xFFCFC6B4)),
        accent = Color(0xFFF3D99A),
        innerTop = Color(0xFF2E2A22), innerBottom = Color(0xFF0E0C09), content = Color(0xFFFFF6E0)
    )
}

val FrameTier.nameRes: Int
    get() = when (this) {
        FrameTier.SPARK -> R.string.frame_spark
        FrameTier.BRONZE -> R.string.frame_bronze
        FrameTier.SILVER_LAUREL -> R.string.frame_silver_laurel
        FrameTier.GOLD_SEAL -> R.string.frame_gold_seal
        FrameTier.MOONSTONE -> R.string.frame_moonstone
        FrameTier.OBSIDIAN_CROWN -> R.string.frame_obsidian_crown
        FrameTier.CENTURY_FLAME -> R.string.frame_century_flame
        FrameTier.AURORA -> R.string.frame_aurora
        FrameTier.STOIC -> R.string.frame_stoic
    }

val FrameTier.storyRes: Int
    get() = when (this) {
        FrameTier.SPARK -> R.string.frame_spark_story
        FrameTier.BRONZE -> R.string.frame_bronze_story
        FrameTier.SILVER_LAUREL -> R.string.frame_silver_laurel_story
        FrameTier.GOLD_SEAL -> R.string.frame_gold_seal_story
        FrameTier.MOONSTONE -> R.string.frame_moonstone_story
        FrameTier.OBSIDIAN_CROWN -> R.string.frame_obsidian_crown_story
        FrameTier.CENTURY_FLAME -> R.string.frame_century_flame_story
        FrameTier.AURORA -> R.string.frame_aurora_story
        FrameTier.STOIC -> R.string.frame_stoic_story
    }

/** İç madalyonun çapı / çerçevenin çapı. */
private const val INNER_RATIO = 0.62f

private class FrameMotion(val spin: Float, val shimmer: Float, val pulse: Float, val phase: Float)

private val StillMotion = FrameMotion(spin = 0f, shimmer = 0.15f, pulse = 0.5f, phase = 0f)

/**
 * Başarı çerçevesi. [tier] null ise sade bir halka çizilir (henüz çerçeve kazanılmamış).
 * [locked] durumda çerçeve gri bir silüet olarak görünür; ne kazanılacağını sezdirir.
 * İç madalyona [content] yerleşir (rütbe, seri vb.).
 */
@Composable
fun FrameEmblem(
    tier: FrameTier?,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    animated: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val motion = if (tier != null && !locked && animated) rememberFrameMotion() else StillMotion
    val look = tier?.look()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (tier == null || look == null) {
                drawPlainFrame()
            } else {
                drawTier(tier, look, motion, locked)
            }
        }
        val innerBrush = when {
            look == null -> Brush.verticalGradient(listOf(DarkCharcoal, DarkCharcoal))
            locked -> Brush.verticalGradient(listOf(Color(0xFF2A2A2C), Color(0xFF141415)))
            else -> Brush.radialGradient(listOf(look.innerTop, look.innerBottom))
        }
        Box(
            modifier = Modifier
                .fillMaxSize(INNER_RATIO)
                .clip(CircleShape)
                .background(innerBrush),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/** Çerçeve madalyonu içindeki metin/simge rengi. */
fun frameContentColor(tier: FrameTier?, locked: Boolean): Color = when {
    tier == null -> Color.Unspecified
    locked -> Color(0xFF9A9A9E)
    else -> tier.look().content
}

@Composable
private fun rememberFrameMotion(): FrameMotion {
    val transition = rememberInfiniteTransition(label = "frame")
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing)),
        label = "spin"
    )
    val shimmer by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3_600, easing = LinearEasing)),
        label = "shimmer"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_400), RepeatMode.Reverse),
        label = "pulse"
    )
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1_700, easing = LinearEasing)),
        label = "phase"
    )
    return FrameMotion(spin, shimmer, pulse, phase)
}

// ---------------------------------------------------------------------------
// Çizim
// ---------------------------------------------------------------------------

private fun DrawScope.drawPlainFrame() {
    val r = size.minDimension / 2f
    drawCircle(color = BorderGray, radius = r * 0.80f, style = Stroke(width = r * 0.035f))
    drawCircle(color = CopperAccent.copy(alpha = 0.35f), radius = r * 0.70f, style = Stroke(width = r * 0.012f))
}

private fun desaturate(c: Color): Color {
    val l = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
    return Color(l, l, l, c.alpha)
}

private fun DrawScope.drawTier(tier: FrameTier, look: FrameLook, m: FrameMotion, locked: Boolean) {
    val metal = if (locked) look.metal.map { desaturate(it).copy(alpha = 0.42f) } else look.metal
    val accent = if (locked) desaturate(look.accent).copy(alpha = 0.40f) else look.accent
    val r = size.minDimension / 2f
    val ringR = r * 0.74f
    val ringW = r * 0.085f

    if (!locked) drawHalo(tier, look, m, r)

    when (tier) {
        FrameTier.SPARK -> {
            drawMetalRing(ringR, r * 0.05f, metal, m.spin)
            drawCircle(accent.copy(alpha = 0.35f), radius = ringR + r * 0.09f, style = Stroke(r * 0.008f))
            if (!locked) drawOrbitSpark(ringR + r * 0.09f, m.spin * 4f, accent, r)
        }
        FrameTier.BRONZE -> {
            drawMetalRing(ringR, ringW, metal, m.spin)
            drawTicks(ringR + ringW * 0.5f + r * 0.05f, 48, r * 0.045f, r * 0.014f, accent, m.spin * 0.5f, majorEvery = 4)
            drawCircle(accent.copy(alpha = 0.5f), radius = ringR - ringW * 0.5f - r * 0.025f, style = Stroke(r * 0.01f))
        }
        FrameTier.SILVER_LAUREL -> {
            drawLaurel(ringR + r * 0.13f, metal[1], metal[2], leaves = 9, r = r, sway = if (locked) 0f else sin(m.phase) * 1.5f)
            drawMetalRing(ringR, ringW * 0.85f, metal, m.spin)
            if (!locked) drawShimmer(ringR, ringW * 0.85f, m.shimmer, Color.White)
        }
        FrameTier.GOLD_SEAL -> {
            drawScallops(ringR + ringW * 0.5f + r * 0.035f, 28, r * 0.05f, metal[3], metal[0])
            drawMetalRing(ringR, ringW * 1.15f, metal, m.spin)
            drawCircle(accent.copy(alpha = 0.7f), radius = ringR - ringW * 0.6f - r * 0.02f, style = Stroke(r * 0.012f))
            if (!locked) drawShimmer(ringR, ringW * 1.15f, m.shimmer, Color.White)
        }
        FrameTier.MOONSTONE -> {
            drawMetalRing(ringR, ringW * 1.2f, metal, m.spin * 2.5f)
            drawCircle(accent.copy(alpha = 0.55f), radius = ringR + ringW * 0.75f, style = Stroke(r * 0.008f))
            if (!locked) drawOrbitDots(ringR + ringW * 0.75f, 3, accent, m.spin * 1.5f, r * 0.022f)
            if (!locked) drawShimmer(ringR, ringW * 1.2f, m.shimmer, Color.White)
        }
        FrameTier.OBSIDIAN_CROWN -> {
            drawMetalRing(ringR, ringW * 1.35f, metal, 0f)
            drawCircle(accent, radius = ringR + ringW * 0.62f, style = Stroke(r * 0.012f))
            drawCircle(accent, radius = ringR - ringW * 0.62f, style = Stroke(r * 0.012f))
            drawTicks(ringR, 60, ringW * 0.9f, r * 0.010f, accent.copy(alpha = 0.85f), m.spin, majorEvery = 5)
            drawCrown(ringR + ringW * 0.62f, r, accent, metal[1])
        }
        FrameTier.CENTURY_FLAME -> {
            drawFlames(ringR + ringW * 0.5f, r, metal, m.phase, if (locked) 0.6f else 1f)
            drawMetalRing(ringR, ringW * 1.1f, metal, m.spin * 3f)
            drawCircle(accent.copy(alpha = 0.8f), radius = ringR - ringW * 0.6f - r * 0.015f, style = Stroke(r * 0.010f))
        }
        FrameTier.AURORA -> {
            drawMetalRing(ringR, ringW * 1.1f, metal, m.spin * 4f)
            drawMetalRing(ringR + ringW * 0.95f, r * 0.012f, metal.reversed(), -m.spin * 2f)
            if (!locked) drawOrbitDots(ringR + ringW * 0.95f, 5, accent, -m.spin * 2f, r * 0.016f)
        }
        FrameTier.STOIC -> {
            drawLaurel(ringR + r * 0.14f, metal[3], metal[2], leaves = 11, r = r, sway = if (locked) 0f else sin(m.phase) * 1.2f, full = true)
            drawMetalRing(ringR, ringW * 1.05f, metal, m.spin)
            drawCircle(accent, radius = ringR - ringW * 0.62f, style = Stroke(r * 0.010f))
            if (!locked) {
                drawShimmer(ringR, ringW * 1.05f, m.shimmer, Color.White)
                drawStarField(r * 0.93f, 12, accent, m.spin * 0.6f, m.pulse, r)
            }
        }
    }
}

/** Arkadan yayılan yumuşak ışık. */
private fun DrawScope.drawHalo(tier: FrameTier, look: FrameLook, m: FrameMotion, r: Float) {
    val strength = when (tier) {
        FrameTier.SPARK, FrameTier.BRONZE -> 0.18f
        FrameTier.SILVER_LAUREL, FrameTier.GOLD_SEAL -> 0.24f
        FrameTier.MOONSTONE, FrameTier.OBSIDIAN_CROWN -> 0.30f
        FrameTier.CENTURY_FLAME, FrameTier.AURORA, FrameTier.STOIC -> 0.40f
    }
    val alpha = strength * (0.75f + 0.25f * m.pulse)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(look.accent.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = r
        ),
        radius = r
    )
}

/** Metalik halka: dönen sweep gradyan. */
private fun DrawScope.drawMetalRing(radius: Float, width: Float, colors: List<Color>, rotation: Float) {
    rotate(rotation) {
        drawCircle(
            brush = Brush.sweepGradient(colors, center),
            radius = radius,
            style = Stroke(width = width)
        )
    }
    // İnce iç ve dış kenar parlaması: metale hacim verir.
    drawCircle(Color.White.copy(alpha = 0.18f), radius = radius + width / 2f, style = Stroke(width * 0.08f))
    drawCircle(Color.Black.copy(alpha = 0.25f), radius = radius - width / 2f, style = Stroke(width * 0.08f))
}

/** Halka üzerinde dolaşan parlak bir ışık lekesi. */
private fun DrawScope.drawShimmer(radius: Float, width: Float, t: Float, color: Color) {
    val start = t * 360f - 90f
    val sweep = 38f
    val steps = 8
    val box = Size(radius * 2, radius * 2)
    val topLeft = Offset(center.x - radius, center.y - radius)
    for (i in 0 until steps) {
        // Ortası en parlak, kenarlara doğru sönen kısa yay.
        val a = (1f - kotlin.math.abs(i - (steps - 1) / 2f) / (steps / 2f)).coerceIn(0f, 1f)
        drawArc(
            color = color.copy(alpha = 0.16f * a),
            startAngle = start + i * (sweep / steps),
            sweepAngle = sweep / steps + 0.6f,
            useCenter = false,
            topLeft = topLeft,
            size = box,
            style = Stroke(width = width * 0.7f)
        )
    }
}

private fun DrawScope.drawTicks(
    radius: Float, count: Int, length: Float, width: Float, color: Color, rotation: Float, majorEvery: Int
) {
    rotate(rotation) {
        for (i in 0 until count) {
            val angle = (i * 360f / count) * (PI / 180f).toFloat()
            val major = i % majorEvery == 0
            val len = if (major) length else length * 0.55f
            val start = Offset(center.x + cos(angle) * (radius - len / 2f), center.y + sin(angle) * (radius - len / 2f))
            val end = Offset(center.x + cos(angle) * (radius + len / 2f), center.y + sin(angle) * (radius + len / 2f))
            drawLine(color.copy(alpha = if (major) color.alpha else color.alpha * 0.6f), start, end, strokeWidth = width, cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawScallops(radius: Float, count: Int, bead: Float, light: Color, dark: Color) {
    for (i in 0 until count) {
        val angle = (i * 360f / count) * (PI / 180f).toFloat()
        val c = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
        drawCircle(
            brush = Brush.radialGradient(listOf(light, dark), center = c - Offset(bead * 0.3f, bead * 0.3f), radius = bead * 1.4f),
            radius = bead,
            center = c
        )
    }
}

private fun DrawScope.drawOrbitDots(radius: Float, count: Int, color: Color, rotation: Float, dot: Float) {
    rotate(rotation) {
        for (i in 0 until count) {
            val angle = (i * 360f / count) * (PI / 180f).toFloat()
            val c = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
            drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = c, radius = dot * 3f), radius = dot * 3f, center = c)
            drawCircle(color, radius = dot, center = c)
        }
    }
}

private fun DrawScope.drawOrbitSpark(radius: Float, rotation: Float, color: Color, r: Float) {
    val angle = (rotation - 90f) * (PI / 180f).toFloat()
    val c = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
    // Kuyruk
    for (i in 1..10) {
        val a = (rotation - 90f - i * 3.2f) * (PI / 180f).toFloat()
        val p = Offset(center.x + cos(a) * radius, center.y + sin(a) * radius)
        drawCircle(color.copy(alpha = 0.32f * (1f - i / 11f)), radius = r * 0.018f * (1f - i / 14f), center = p)
    }
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = c, radius = r * 0.09f), radius = r * 0.09f, center = c)
    drawCircle(Color.White, radius = r * 0.02f, center = c)
}

/**
 * Defne dalı: alttan başlayıp her iki yandan yukarı tırmanan yapraklar. [full] ise
 * tepede de kapanan tam çelenk.
 */
private fun DrawScope.drawLaurel(
    radius: Float, light: Color, dark: Color, leaves: Int, r: Float, sway: Float, full: Boolean = false
) {
    val leafLen = r * 0.17f
    val leafW = r * 0.065f
    val endAngle = if (full) 262f else 240f
    for (side in listOf(-1f, 1f)) {
        for (i in 0 until leaves) {
            val t = i / (leaves - 1f)
            // Sol dal: 100°→endAngle, sağ dal ayna görüntüsü.
            val theta = 100f + (endAngle - 100f) * t
            val deg = if (side < 0) theta else 180f - theta
            val rad = deg * (PI / 180f).toFloat()
            val p = Offset(center.x + cos(rad) * radius, center.y + sin(rad) * radius)
            val tangent = if (side < 0) deg + 90f else deg - 90f
            for (k in listOf(-1f, 1f)) {
                val tilt = tangent + k * 32f + sway
                val off = r * 0.035f * k
                val pc = Offset(p.x + cos(rad) * off, p.y + sin(rad) * off)
                withTransform({
                    rotate(tilt, pc)
                }) {
                    drawOval(
                        brush = Brush.linearGradient(
                            listOf(light, dark),
                            start = Offset(pc.x - leafLen / 2f, pc.y),
                            end = Offset(pc.x + leafLen / 2f, pc.y)
                        ),
                        topLeft = Offset(pc.x - leafLen / 2f, pc.y - leafW / 2f),
                        size = Size(leafLen, leafW)
                    )
                }
            }
        }
    }
    // Dal sapı alt ortada birleşir.
    drawCircle(dark, radius = r * 0.03f, center = Offset(center.x, center.y + radius))
}

private fun DrawScope.drawCrown(radius: Float, r: Float, gold: Color, shine: Color) {
    val baseY = center.y - radius
    val w = r * 0.44f
    val h = r * 0.2f
    val path = Path().apply {
        moveTo(center.x - w / 2f, baseY + h * 0.15f)
        lineTo(center.x - w / 2f, baseY - h * 0.55f)
        lineTo(center.x - w / 4f, baseY - h * 0.15f)
        lineTo(center.x, baseY - h)
        lineTo(center.x + w / 4f, baseY - h * 0.15f)
        lineTo(center.x + w / 2f, baseY - h * 0.55f)
        lineTo(center.x + w / 2f, baseY + h * 0.15f)
        close()
    }
    drawPath(path, brush = Brush.verticalGradient(listOf(lerp(gold, Color.White, 0.35f), gold, lerp(gold, Color.Black, 0.3f)), startY = baseY - h, endY = baseY + h * 0.2f))
    drawPath(path, color = Color.Black.copy(alpha = 0.35f), style = Stroke(r * 0.008f))
    for (x in listOf(-w / 2f, 0f, w / 2f)) {
        val y = if (x == 0f) baseY - h else baseY - h * 0.55f
        drawCircle(shine, radius = r * 0.022f, center = Offset(center.x + x, y))
    }
}

/** Halka çevresinde titreyen alev dilleri. */
private fun DrawScope.drawFlames(radius: Float, r: Float, metal: List<Color>, phase: Float, strength: Float) {
    val count = 22
    for (i in 0 until count) {
        val base = i * 360f / count
        val flick = sin(phase * 2f + i * 1.7f) * 0.5f + 0.5f
        val len = r * (0.09f + 0.07f * flick)
        val halfWidth = 360f / count * 0.42f
        val a0 = (base - halfWidth) * (PI / 180f).toFloat()
        val a1 = (base + halfWidth) * (PI / 180f).toFloat()
        val am = (base + sin(phase + i) * 4f) * (PI / 180f).toFloat()
        val p0 = Offset(center.x + cos(a0) * radius, center.y + sin(a0) * radius)
        val p1 = Offset(center.x + cos(a1) * radius, center.y + sin(a1) * radius)
        val tip = Offset(center.x + cos(am) * (radius + len), center.y + sin(am) * (radius + len))
        val path = Path().apply {
            moveTo(p0.x, p0.y)
            quadraticTo(
                center.x + cos(a0) * (radius + len * 0.6f), center.y + sin(a0) * (radius + len * 0.6f),
                tip.x, tip.y
            )
            quadraticTo(
                center.x + cos(a1) * (radius + len * 0.6f), center.y + sin(a1) * (radius + len * 0.6f),
                p1.x, p1.y
            )
            close()
        }
        drawPath(
            path,
            brush = Brush.radialGradient(
                listOf(metal[3], metal[2].copy(alpha = metal[2].alpha * 0.9f), metal[0].copy(alpha = 0f)),
                center = center,
                radius = radius + len
            ),
            alpha = (0.75f + 0.25f * flick) * strength
        )
    }
}

private fun DrawScope.drawStarField(radius: Float, count: Int, color: Color, rotation: Float, pulse: Float, r: Float) {
    rotate(rotation) {
        for (i in 0 until count) {
            val angle = (i * 360f / count + (i % 2) * 9f) * (PI / 180f).toFloat()
            val rr = radius * (if (i % 2 == 0) 1f else 0.94f)
            val c = Offset(center.x + cos(angle) * rr, center.y + sin(angle) * rr)
            val twinkle = 0.45f + 0.55f * ((sin(pulse * PI.toFloat() * 2f + i) + 1f) / 2f)
            val s = r * 0.022f * twinkle
            drawLine(color.copy(alpha = twinkle), Offset(c.x - s * 2f, c.y), Offset(c.x + s * 2f, c.y), strokeWidth = s * 0.5f, cap = StrokeCap.Round)
            drawLine(color.copy(alpha = twinkle), Offset(c.x, c.y - s * 2f), Offset(c.x, c.y + s * 2f), strokeWidth = s * 0.5f, cap = StrokeCap.Round)
            drawCircle(Color.White.copy(alpha = twinkle), radius = s * 0.6f, center = c)
        }
    }
}
