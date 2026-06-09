package com.gardiyan.app.ui.components

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val dailyProgress = if (app.dailyLimitMinutes > 0) {
        (app.remainingSecondsToday.toFloat() / (app.dailyLimitMinutes * 60).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val mm = totalSecs / 60
    val ss = totalSecs % 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                BorderGray,
                RoundedCornerShape(24.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Top section: Icon + Title + Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Circular app icon container with black border
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(2.dp, PureBlack, CircleShape)
                            .background(PureWhite)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIconView(
                            packageName = app.packageName,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureBlack
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.protected_apps_daily_limit) + ": " + stringResource(R.string.protected_apps_minutes, app.dailyLimitMinutes),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MutedGray
                        )
                    }

                    // Green/Red badge on the right
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isLocked || app.isFailed) SoftDangerRed else Color(0xFFF0FAF5)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isLocked || app.isFailed) Icons.Default.Warning else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isLocked || app.isFailed) DangerRed else Color(0xFF34A853),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isLocked) "Süre Doldu" else if (app.isFailed) "Kilitlendi" else "Korunuyor",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLocked || app.isFailed) DangerRed else Color(0xFF34A853)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom section: linear progress bar and Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { dailyProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(CircleShape),
                        color = if (isLocked) DangerRed else PureBlack,
                        trackColor = Color(0xFFF0F4FF)
                    )
                    Text(
                        text = String.format(Locale.ROOT, "%02d:%02d", mm, ss),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isLocked) DangerRed else PureBlack
                    )
                }
            }
        }
    }
}
