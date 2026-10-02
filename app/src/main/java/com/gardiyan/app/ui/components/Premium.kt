package com.gardiyan.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.R
import com.gardiyan.app.ui.theme.BorderGray
import com.gardiyan.app.ui.theme.CopperAccent
import com.gardiyan.app.ui.theme.DarkCharcoal
import com.gardiyan.app.ui.theme.IsDarkUi
import com.gardiyan.app.ui.theme.LimitraDisplay
import com.gardiyan.app.ui.theme.LimitraMotion
import com.gardiyan.app.ui.theme.MatteSurface
import com.gardiyan.app.ui.theme.MutedGray
import com.gardiyan.app.ui.theme.OnAccent
import com.gardiyan.app.ui.theme.PureBlack
import com.gardiyan.app.ui.theme.onColorFor

/**
 * Basılınca yaylı biçimde küçülen bileşenler için ölçek. Material düğmelerinde kendi
 * InteractionSource'larıyla birlikte kullanılır.
 */
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = LimitraMotion.press(),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Tıklanabilir yüzey: yaylı küçülme + hafif dokunsal titreşim. Dalga efekti yoktur;
 * premium uygulamalardaki sakin geri bildirim hissi için.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    haptic: Boolean = true,
    pressedScale: Float = 0.97f,
    role: Role = Role.Button,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current
    this
        .pressScale(interactionSource, pressedScale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role
        ) {
            if (haptic) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

/**
 * Bölüm kartı. Açık temada yumuşak, renkli bir gölgeyle zeminden ayrılır; koyu temada
 * yalnız ince kenar çizgisi kullanılır (koyu zeminde gölge görünmez, kirli durur).
 */
@Composable
fun LimitraCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = DarkCharcoal,
    borderColor: Color = BorderGray,
    elevated: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = if (elevated && !IsDarkUi) {
        modifier.shadow(
            elevation = 14.dp,
            shape = shape,
            ambientColor = PureBlack.copy(alpha = 0.05f),
            spotColor = PureBlack.copy(alpha = 0.10f)
        )
    } else {
        modifier
    }
    val clickModifier = if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier
    Column(
        modifier = base
            .then(clickModifier)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor.copy(alpha = if (IsDarkUi) 1f else 0.7f), shape),
        content = content
    )
}

/** Birincil eylem düğmesi: tek vurgu rengi, yaylı basma, titreşim. */
@Composable
fun LimitraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    containerColor: Color = CopperAccent,
    contentColor: Color = onColorFor(containerColor),
    height: Dp = 56.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hapticFeedback = LocalHapticFeedback.current
    val shape = RoundedCornerShape(18.dp)
    val bg = if (enabled) containerColor else BorderGray
    val fg = if (enabled) contentColor else MutedGray
    Row(
        modifier = modifier
            .heightIn(min = height)
            .pressScale(interactionSource)
            .then(
                if (enabled && !IsDarkUi) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = shape,
                        ambientColor = containerColor.copy(alpha = 0.25f),
                        spotColor = containerColor.copy(alpha = 0.45f)
                    )
                } else Modifier
            )
            .clip(shape)
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button
            ) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            color = fg,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.1.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** İkincil (çerçeveli) düğme. */
@Composable
fun LimitraSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color = PureBlack
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .border(1.dp, BorderGray, shape)
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = if (enabled) contentColor else MutedGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/** Ekran başlığı: serif büyük başlık + isteğe bağlı açıklama. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (onBack != null) {
            BackChip(onClick = onBack)
            Spacer(modifier = Modifier.height(14.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontFamily = LimitraDisplay,
                fontWeight = FontWeight.Normal,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.6).sp,
                color = PureBlack
            )
            trailing?.invoke(this)
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MutedGray
            )
        }
    }
}

/** Geri düğmesi: yuvarlak, ince çerçeveli. */
@Composable
fun BackChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(DarkCharcoal)
            .border(1.dp, BorderGray, CircleShape)
            .pressable(haptic = false, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.btn_back_desc),
            tint = PureBlack,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Bölüm başlığı: solda başlık, sağda isteğe bağlı eylem bağlantısı. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontFamily = LimitraDisplay,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.2).sp,
            color = PureBlack
        )
        if (actionText != null && onAction != null) {
            Text(
                text = actionText,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                color = CopperAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Küçük durum hapı (aktif / planlandı / limit doldu gibi). */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier, showDot: Boolean = true) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * İlk görünüşte içerik hafifçe yukarı kayarak belirir. [index] sıralı gecikme verir.
 * Aynı ekranda kaydırıp geri dönüldüğünde tekrar oynamaması için [played] kümesi
 * ekran düzeyinde hatırlanır.
 */
fun Modifier.entrance(index: Int, played: MutableSet<Int>? = null): Modifier = composed {
    val alreadyPlayed = played?.contains(index) == true
    val progress = remember { Animatable(if (alreadyPlayed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            kotlinx.coroutines.delay((index.coerceAtMost(8) * 55L))
            progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 220f))
            played?.add(index)
        }
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 28.dp.toPx()
    }
}

/**
 * Alttan kayarak açılan sayfa (scrim ile). Kapanış animasyonu için içerik görünürlük
 * bittikten sonra kaldırılır. Geri tuşu sayfayı kapatır.
 */
@Composable
fun BoxScope.AnimatedSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    heightFraction: Float = 0.88f,
    content: @Composable ColumnScope.() -> Unit
) {
    if (visible) {
        BackHandler(onBack = onDismiss)
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.matchParentSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (IsDarkUi) 0.6f else 0.38f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 380f)) { it } + fadeIn(),
        exit = slideOutVertically(spring(stiffness = 600f)) { it } + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(heightFraction)
                .clip(shape)
                .background(DarkCharcoal)
                .border(1.dp, BorderGray, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(BorderGray)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

/** Sayfa içi hafif bölücü. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BorderGray)
    )
}

/** Simge kabı: yumuşak accent zemin üzerinde ince simge. Emoji yerine kullanılır. */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}
