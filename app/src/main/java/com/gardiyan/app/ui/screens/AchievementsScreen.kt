package com.gardiyan.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.R
import com.gardiyan.app.data.achievements.AchievementProgress
import com.gardiyan.app.data.achievements.AchievementStore
import com.gardiyan.app.data.achievements.Achievements
import com.gardiyan.app.data.achievements.FrameTier
import com.gardiyan.app.ui.components.AnimatedSheet
import com.gardiyan.app.ui.components.FrameEmblem
import com.gardiyan.app.ui.components.LimitraCard
import com.gardiyan.app.ui.components.LimitraPrimaryButton
import com.gardiyan.app.ui.components.ScreenHeader
import com.gardiyan.app.ui.components.StatusPill
import com.gardiyan.app.ui.components.entrance
import com.gardiyan.app.ui.components.frameContentColor
import com.gardiyan.app.ui.components.look
import com.gardiyan.app.ui.components.nameRes
import com.gardiyan.app.ui.components.storyRes
import com.gardiyan.app.ui.theme.BorderGray
import com.gardiyan.app.ui.theme.CopperAccent
import com.gardiyan.app.ui.theme.LimitraDisplay
import com.gardiyan.app.ui.theme.MatteSurface
import com.gardiyan.app.ui.theme.MutedGray
import com.gardiyan.app.ui.theme.PureBlack
import com.gardiyan.app.ui.theme.SuccessGreen
import com.gardiyan.app.viewmodel.GuardianViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Başarı durumunu ekranlar arasında tutarlı tutan küçük denetleyici. */
@Stable
class AchievementController internal constructor(private val store: AchievementStore) {
    var progress by mutableStateOf(Achievements.progress(0, store.bestStreak))
        private set
    private var chosen by mutableStateOf(store.chosenFrame)
    private var celebrated by mutableStateOf(store.celebrated)

    val equipped: FrameTier? get() = Achievements.resolveEquipped(progress, chosen)

    /** Gösterilecek tek kutlama: en yüksek yeni çerçeve. */
    val pendingCelebration: FrameTier?
        get() = Achievements.pendingCelebrations(progress, celebrated).firstOrNull()

    internal fun record(streak: Int) {
        progress = store.record(streak)
    }

    fun equip(tier: FrameTier) {
        if (!progress.isUnlocked(tier)) return
        store.chosenFrame = tier
        chosen = tier
    }

    /** Kazanılmış tüm çerçeveleri kutlanmış sayar; ilk kurulumda geçmiş çerçeveler tek kutlamada toplanır. */
    fun dismissCelebrations() {
        store.markCelebrated(progress.unlocked)
        celebrated = store.celebrated
    }
}

/** Uygulama kökünde bir kez oluşturulan ortak denetleyici; ekranlar buradan okur. */
val LocalAchievements = staticCompositionLocalOf<AchievementController?> { null }

/** Kökte sağlanmışsa ortak denetleyiciyi, yoksa (ör. önizleme) yerel bir tane döner. */
@Composable
fun achievementController(viewModel: GuardianViewModel): AchievementController =
    LocalAchievements.current ?: rememberAchievementController(viewModel)

/**
 * Seriyi [GuardianViewModel.userSession] içinden okur. Seri yüklenmeden önce 0 gelir;
 * kayıt yalnız artırdığı için bu güvenlidir.
 */
@Composable
fun rememberAchievementController(viewModel: GuardianViewModel): AchievementController {
    val context = LocalContext.current
    val controller = remember { AchievementController(AchievementStore(context)) }
    val session by viewModel.userSession.collectAsState()
    val loaded = session != null
    val streak = session?.consecutiveSuccessDays ?: 0
    LaunchedEffect(streak, loaded) {
        if (loaded) controller.record(streak)
    }
    return controller
}

@Composable
fun AchievementsScreen(
    viewModel: GuardianViewModel,
    onBack: () -> Unit
) {
    val controller = achievementController(viewModel)
    AchievementsContent(
        progress = controller.progress,
        equipped = controller.equipped,
        onEquip = controller::equip,
        onBack = onBack
    )
}

@Composable
fun AchievementsContent(
    progress: AchievementProgress,
    equipped: FrameTier?,
    onEquip: (FrameTier) -> Unit,
    onBack: () -> Unit,
    animated: Boolean = true
) {
    var selected by remember { mutableStateOf<FrameTier?>(null) }
    var lastSelected by remember { mutableStateOf<FrameTier?>(null) }
    if (selected != null) lastSelected = selected
    val played = remember { mutableSetOf<Int>() }

    Box(modifier = Modifier.fillMaxSize().background(MatteSurface)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                ScreenHeader(
                    title = stringResource(R.string.achievements_title),
                    subtitle = stringResource(R.string.achievements_subtitle),
                    onBack = onBack
                )
            }
            item {
                AchievementHero(
                    progress = progress,
                    equipped = equipped,
                    animated = animated,
                    modifier = Modifier.entrance(0, played)
                )
            }
            item {
                Text(
                    text = stringResource(R.string.achievements_count, progress.unlocked.size, FrameTier.entries.size),
                    modifier = Modifier.padding(top = 6.dp, start = 2.dp),
                    fontFamily = LimitraDisplay,
                    fontSize = 22.sp,
                    color = PureBlack
                )
            }
            FrameTier.entries.chunked(3).forEachIndexed { rowIndex, row ->
                item(key = "row$rowIndex") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .entrance(rowIndex + 1, played),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { tier ->
                            FrameTile(
                                tier = tier,
                                unlocked = progress.isUnlocked(tier),
                                isEquipped = tier == equipped,
                                animated = animated,
                                onClick = { selected = tier },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            item {
                LimitraCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.achievements_rule_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PureBlack
                        )
                        Text(
                            text = stringResource(R.string.achievements_rule_desc),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MutedGray
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(28.dp)) }
        }

        AnimatedSheet(
            visible = selected != null,
            onDismiss = { selected = null },
            heightFraction = 0.72f
        ) {
            lastSelected?.let { tier ->
                FrameDetail(
                    tier = tier,
                    unlocked = progress.isUnlocked(tier),
                    isEquipped = tier == equipped,
                    currentStreak = progress.currentStreak,
                    animated = animated,
                    onEquip = {
                        onEquip(tier)
                        selected = null
                    }
                )
            }
        }
    }
}

/** Takılı çerçevenin sahnelendiği koyu vitrin. Tema ne olursa olsun koyudur. */
@Composable
private fun AchievementHero(
    progress: AchievementProgress,
    equipped: FrameTier?,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    val look = equipped?.look()
    val stageTop = look?.innerTop ?: Color(0xFF1B1D24)
    val shape = RoundedCornerShape(30.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(stageTop, Color(0xFF0B0B0D))))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .padding(horizontal = 20.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FrameEmblem(
            tier = equipped,
            animated = animated,
            modifier = Modifier.size(212.dp)
        ) {
            StreakReadout(
                streak = progress.currentStreak,
                color = if (equipped != null) frameContentColor(equipped, false) else Color(0xFFEDE6D8),
                big = true
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = equipped?.let { stringResource(it.nameRes) } ?: stringResource(R.string.achievements_title),
            fontFamily = LimitraDisplay,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            color = Color(0xFFF4EFE6),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.achievements_best_streak) + " · " +
                pluralStringResource(R.plurals.achievements_days, progress.bestStreak, progress.bestStreak),
            fontSize = 13.sp,
            color = Color(0xFFB9B2A6)
        )
        Spacer(modifier = Modifier.height(20.dp))
        NextMilestone(progress = progress)
    }
}

@Composable
private fun StreakReadout(streak: Int, color: Color, big: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = streak.toString(),
            fontFamily = LimitraDisplay,
            fontSize = if (big) 52.sp else 30.sp,
            lineHeight = if (big) 54.sp else 32.sp,
            color = color
        )
        Text(
            text = pluralStringResource(R.plurals.achievements_days, streak, streak)
                .replace(streak.toString(), "").trim(),
            fontSize = if (big) 12.sp else 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = color.copy(alpha = 0.75f)
        )
    }
}

@Composable
private fun NextMilestone(progress: AchievementProgress) {
    val next = progress.next
    val target by animateFloatAsState(
        targetValue = progress.progressToNext,
        animationSpec = tween(1100),
        label = "nextProgress"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (next != null) {
                    stringResource(R.string.achievements_next, stringResource(next.nameRes))
                } else {
                    stringResource(R.string.achievements_all_unlocked)
                },
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF4EFE6)
            )
            if (next != null) {
                Text(
                    text = pluralStringResource(R.plurals.achievements_days_left, progress.daysToNext, progress.daysToNext),
                    fontSize = 12.sp,
                    color = Color(0xFFB9B2A6)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        val barColor = next?.look()?.accent ?: Color(0xFFF3D99A)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.10f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(target.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.6f), barColor)))
            )
        }
    }
}

@Composable
private fun FrameTile(
    tier: FrameTier,
    unlocked: Boolean,
    isEquipped: Boolean,
    animated: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (isEquipped) CopperAccent.copy(alpha = 0.10f) else Color.Transparent)
            .border(1.dp, if (isEquipped) CopperAccent.copy(alpha = 0.6f) else BorderGray, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FrameEmblem(
            tier = tier,
            locked = !unlocked,
            animated = animated,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(1f)
        ) {
            if (unlocked) {
                Text(
                    text = tier.requiredDays.toString(),
                    fontFamily = LimitraDisplay,
                    fontSize = 20.sp,
                    color = frameContentColor(tier, false)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.achievements_locked),
                    tint = Color(0xFF9A9A9E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(tier.nameRes),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (unlocked) PureBlack else MutedGray,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = when {
                isEquipped -> stringResource(R.string.achievements_equipped)
                unlocked -> stringResource(R.string.achievements_unlocked)
                else -> pluralStringResource(R.plurals.achievements_days, tier.requiredDays, tier.requiredDays)
            },
            fontSize = 11.sp,
            color = if (isEquipped) CopperAccent else MutedGray,
            fontWeight = if (isEquipped) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FrameDetail(
    tier: FrameTier,
    unlocked: Boolean,
    isEquipped: Boolean,
    currentStreak: Int,
    animated: Boolean,
    onEquip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(tier.look().innerTop.copy(alpha = 0.55f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            FrameEmblem(
                tier = tier,
                locked = !unlocked,
                animated = animated,
                modifier = Modifier.size(200.dp)
            ) {
                if (unlocked) {
                    Text(
                        text = tier.requiredDays.toString(),
                        fontFamily = LimitraDisplay,
                        fontSize = 40.sp,
                        color = frameContentColor(tier, false)
                    )
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF9A9A9E), modifier = Modifier.size(28.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(tier.nameRes),
            fontFamily = LimitraDisplay,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            color = PureBlack,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatusPill(
            text = when {
                isEquipped -> stringResource(R.string.achievements_equipped)
                unlocked -> stringResource(R.string.achievements_unlocked)
                else -> pluralStringResource(R.plurals.achievements_unlock_at, tier.requiredDays, tier.requiredDays)
            },
            color = if (unlocked) SuccessGreen else MutedGray
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(tier.storyRes),
            fontFamily = LimitraDisplay,
            fontStyle = FontStyle.Italic,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            color = PureBlack.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        if (!unlocked) {
            Spacer(modifier = Modifier.height(10.dp))
            val left = (tier.requiredDays - currentStreak).coerceAtLeast(0)
            Text(
                text = pluralStringResource(R.plurals.achievements_days_left, left, left),
                fontSize = 13.sp,
                color = MutedGray
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        if (unlocked && !isEquipped) {
            LimitraPrimaryButton(
                text = stringResource(R.string.achievements_equip),
                onClick = onEquip,
                icon = Icons.Default.Check,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Yeni çerçeve kazanıldığında tam ekran kutlama: dönen ışık huzmeleri, saçılan
 * kıvılcımlar ve yaylı biçimde büyüyen çerçeve.
 */
@Composable
fun AchievementCelebration(
    tier: FrameTier,
    streak: Int,
    onEquip: () -> Unit,
    onDismiss: () -> Unit
) {
    val look = tier.look()
    val appear = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(tier) {
        appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 140f))
    }
    LaunchedEffect(tier) {
        burst.animateTo(1f, tween(1600))
    }
    val rays = rememberInfiniteTransition(label = "rays")
    val rayAngle by rays.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing)),
        label = "rayAngle"
    )

    // Kutlanan eşik: seri sonradan kırılmış olsa bile kazanılan gün sayısı gösterilir.
    val milestone = if (streak >= tier.requiredDays) streak else tier.requiredDays
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060607))
            .background(Brush.radialGradient(listOf(look.innerTop, Color(0xFF060607))))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * 0.40f)
            val reach = size.maxDimension * 0.7f
            rotate(rayAngle, c) {
                for (i in 0 until 14) {
                    val a = (i * 360f / 14f) * (PI / 180f).toFloat()
                    val a2 = a + 0.09f
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(c.x, c.y)
                        lineTo(c.x + cos(a) * reach, c.y + sin(a) * reach)
                        lineTo(c.x + cos(a2) * reach, c.y + sin(a2) * reach)
                        close()
                    }
                    drawPath(path, brush = Brush.radialGradient(listOf(look.accent.copy(alpha = 0.16f * appear.value), Color.Transparent), center = c, radius = reach))
                }
            }
            // Kıvılcım saçılması
            val b = burst.value
            if (b < 1f) {
                for (i in 0 until 36) {
                    val a = (i * 360f / 36f + (i % 3) * 7f) * (PI / 180f).toFloat()
                    val dist = size.minDimension * (0.18f + 0.38f * b) * (0.7f + (i % 5) * 0.08f)
                    val p = Offset(c.x + cos(a) * dist, c.y + sin(a) * dist + b * b * 120f)
                    drawCircle(look.metal[i % look.metal.size].copy(alpha = (1f - b)), radius = 5f * (1f - b * 0.6f), center = p)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.achievements_celebration_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = look.accent,
                modifier = Modifier.graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }
            )
            Spacer(modifier = Modifier.height(18.dp))
            FrameEmblem(
                tier = tier,
                modifier = Modifier
                    .size(240.dp)
                    .graphicsLayer {
                        val s = 0.4f + 0.6f * appear.value
                        scaleX = s
                        scaleY = s
                        alpha = appear.value.coerceIn(0f, 1f)
                    }
            ) {
                StreakReadout(streak = milestone, color = look.content, big = true)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(tier.nameRes),
                fontFamily = LimitraDisplay,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                color = Color(0xFFF7F2E8),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.achievements_celebration_desc, stringResource(tier.nameRes), milestone),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFFC9C2B6),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(30.dp))
            LimitraPrimaryButton(
                text = stringResource(R.string.achievements_celebration_cta),
                onClick = onEquip,
                containerColor = look.accent,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.achievements_celebration_later),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC9C2B6)
            )
        }
    }
}

/** Profil ve ana ekrandaki küçük madalyon: takılı çerçeve + iç içerik. */
@Composable
fun EquippedEmblem(
    equipped: FrameTier?,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    content: @Composable () -> Unit
) {
    FrameEmblem(tier = equipped, modifier = modifier, animated = animated) {
        content()
    }
}
