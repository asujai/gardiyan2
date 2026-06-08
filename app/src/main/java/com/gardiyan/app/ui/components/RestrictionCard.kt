package com.gardiyan.app.ui.components

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.gardiyan.app.data.local.entity.RestrictedAppEntity
import com.gardiyan.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.ui.res.stringResource
import com.gardiyan.app.R
import java.util.Locale

@Composable
fun AppIconView(packageName: String, modifier: Modifier = Modifier) {
    if (packageName.isEmpty()) {
        Box(modifier = modifier)
        return
    }
    val context = LocalContext.current
    var iconDrawable by remember(packageName) { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val icon = pm.getApplicationIcon(packageName)
                iconDrawable = icon
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    if (iconDrawable != null) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            },
            modifier = modifier,
            update = { imageView ->
                imageView.setImageDrawable(iconDrawable)
            }
        )
    } else {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageResource(android.R.drawable.sym_def_app_icon)
                }
            },
            modifier = modifier
        )
    }
}

@Composable
fun ModernRestrictionCard(
    app: RestrictedAppEntity,
    onClick: () -> Unit
) {
    val totalSecs = app.remainingSecondsToday.coerceAtLeast(0)
    val isLocked = totalSecs <= 0

    // Süre kalan ilerleme (toplam günlük)
    val dailyProgress = if (app.dailyLimitMinutes > 0) {
        (app.remainingSecondsToday.toFloat() / (app.dailyLimitMinutes * 60).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val mm = totalSecs / 60
    val ss = totalSecs % 60

    val daysMap = mapOf(
        "Pzt" to R.string.day_mon,
        "Sal" to R.string.day_tue,
        "Çar" to R.string.day_wed,
        "Per" to R.string.day_thu,
        "Cum" to R.string.day_fri,
        "Cmt" to R.string.day_sat,
        "Paz" to R.string.day_sun
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.2.dp,
                if (isLocked) DangerRed.copy(alpha = 0.3f)
                else if (app.isFailed) DangerRed.copy(alpha = 0.3f)
                else SuccessGreen.copy(alpha = 0.2f),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Üst kısım: İkon + Ad + Durum
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // App icon with status ring
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { dailyProgress },
                        modifier = Modifier.size(56.dp),
                        color = if (isLocked) DangerRed else PureBlack,
                        trackColor = if (isLocked) DangerRed.copy(alpha = 0.1f) else BorderGray,
                        strokeWidth = 3.5.dp
                    )
                    AppIconView(
                        packageName = app.packageName,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.protected_apps_daily_limit) + ": " + stringResource(R.string.protected_apps_minutes, app.dailyLimitMinutes),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MutedGray
                    )
                    val isAllDays = app.activeDays.split(",").map { it.trim() }.filter { it.isNotEmpty() }.size >= 7
                    if (app.activeDays.isNotEmpty() && !isAllDays) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val localizedDays = app.activeDays.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .mapNotNull { daysMap[it] }
                            .map { stringResource(it) }
                            .joinToString(",")
                        Text(
                            text = stringResource(R.string.protected_apps_active_days) + ": $localizedDays",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MutedGray.copy(alpha = 0.8f)
                        )
                    }
                }

                // Durum badge'i (İkon ağırlıklı ve nötr)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isLocked -> SoftDangerRed
                                app.isFailed -> SoftDangerRed
                                else -> SuccessGreen.copy(alpha = 0.1f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val statusIcon = when {
                        isLocked -> "🔒"
                        app.isFailed -> "⚠️"
                        else -> "🛡️"
                    }
                    val statusTextRes = when {
                        isLocked -> R.string.protected_apps_limit_reached
                        app.isFailed -> R.string.protected_apps_discipline_process
                        else -> R.string.status_protected
                    }
                    val statusColor = when {
                        isLocked || app.isFailed -> DangerRed
                        else -> SuccessGreen
                    }
                    Text(text = statusIcon, fontSize = 10.sp)
                    Text(
                        text = stringResource(statusTextRes),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alt kısım: günlük süre ilerleme çubuğu ve Sayaç
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { dailyProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isLocked) DangerRed else PureBlack,
                    trackColor = BorderGray
                )
                Text(
                    text = String.format(Locale.ROOT, "%02d:%02d", mm, ss),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isLocked) DangerRed else PureBlack
                )
            }
        }
    }
}
