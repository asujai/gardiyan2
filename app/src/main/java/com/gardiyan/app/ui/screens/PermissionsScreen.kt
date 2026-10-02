package com.gardiyan.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.gardiyan.app.ui.components.Hairline
import com.gardiyan.app.ui.components.IconBadge
import com.gardiyan.app.ui.components.LimitraCard
import com.gardiyan.app.ui.components.LimitraPrimaryButton
import com.gardiyan.app.ui.components.StatusPill
import com.gardiyan.app.ui.components.entrance
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.R
import com.gardiyan.app.ui.theme.*
import com.gardiyan.app.viewmodel.GuardianViewModel

@Composable
fun PermissionsScreen(
    viewModel: GuardianViewModel,
    isOverlayEnabled: Boolean,
    isUsageEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    accessibilityNeedsReenable: Boolean,
    accessibilityFailSafeActive: Boolean,
    isBatteryExempted: Boolean,
    isNotificationsEnabled: Boolean,
    canContinueToApp: Boolean,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )
    val hasAllPermissions = isOverlayEnabled && isUsageEnabled && isAccessibilityEnabled && isBatteryExempted

    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var showUsageDialog by remember { mutableStateOf(false) }
    var showOverlayDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.disclosure_accessibility_title),
                    fontWeight = FontWeight.Bold,
                    color = PureBlack,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = if (accessibilityNeedsReenable) {
                        stringResource(R.string.accessibility_reenable_dialog_desc)
                    } else {
                        stringResource(R.string.disclosure_accessibility_desc)
                    },
                    color = MutedGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityDialog = false
                        context.getSharedPreferences("gardiyan_settings", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("accessibility_approved", true)
                            .apply()
                        viewModel.openAccessibilitySettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureBlack,
                        contentColor = OnPureBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_accept_disclosure), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAccessibilityDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MutedGray)
                ) {
                    Text(stringResource(R.string.btn_deny_disclosure))
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showUsageDialog) {
        AlertDialog(
            onDismissRequest = { showUsageDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.disclosure_usage_title),
                    fontWeight = FontWeight.Bold,
                    color = PureBlack,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.disclosure_usage_desc),
                    color = MutedGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUsageDialog = false
                        viewModel.openUsageStatsSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.perm_btn_grant), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsageDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = MutedGray)
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showOverlayDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.disclosure_overlay_title),
                    fontWeight = FontWeight.Bold,
                    color = PureBlack,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.disclosure_overlay_desc),
                    color = MutedGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayDialog = false
                        viewModel.openOverlaySettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureBlack,
                        contentColor = OnPureBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_accept_disclosure), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showOverlayDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MutedGray)
                ) {
                    Text(stringResource(R.string.btn_deny_disclosure))
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.disclosure_battery_title),
                    fontWeight = FontWeight.Bold,
                    color = PureBlack,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.disclosure_battery_desc),
                    color = MutedGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryDialog = false
                        viewModel.requestBatteryOptimizationIgnore(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureBlack,
                        contentColor = OnPureBlack
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_accept_disclosure), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatteryDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MutedGray)
                ) {
                    Text(stringResource(R.string.btn_deny_disclosure))
                }
            },
            containerColor = DarkCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    val grantedCount = listOf(isUsageEnabled, isAccessibilityEnabled, isOverlayEnabled, isBatteryExempted, isNotificationsEnabled)
        .count { it }
    val ringProgress by animateFloatAsState(
        targetValue = grantedCount / 5f,
        animationSpec = tween(900),
        label = "permRing"
    )
    val locale = LocalConfiguration.current.locales[0]
    val played = remember { mutableSetOf<Int>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Marka simgesi ve verilen izinlerin halkası
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(132.dp)
                .entrance(0, played)
        ) {
            val ringColor = if (hasAllPermissions) SuccessGreen else CopperAccent
            val track = BorderGray
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 5.dp.toPx()
                drawArc(
                    color = track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke)
                )
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * ringProgress,
                    useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Image(
                painter = painterResource(R.drawable.limitra_brand_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.entrance(1, played)
        ) {
            Text(
                text = stringResource(R.string.perm_title_header).lowercase(locale)
                    .replaceFirstChar { it.titlecase(locale) },
                fontFamily = LimitraDisplay,
                fontSize = 34.sp,
                lineHeight = 38.sp,
                color = PureBlack,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.perm_desc_header),
                fontSize = 14.sp,
                color = MutedGray,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            StatusPill(
                text = "$grantedCount / 5",
                color = if (hasAllPermissions) SuccessGreen else CopperAccent
            )
        }

        if (accessibilityNeedsReenable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DangerRed.copy(alpha = 0.07f))
                    .border(1.dp, DangerRed.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (accessibilityFailSafeActive) {
                        stringResource(R.string.accessibility_failsafe_warning)
                    } else {
                        stringResource(R.string.accessibility_reenable_warning)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DangerRed,
                    lineHeight = 17.sp
                )
            }
        }

        // İzin adımları
        LimitraCard(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(2, played),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                ModernPermissionCard(
                    step = 1,
                    title = stringResource(R.string.perm_usage_access_title),
                    description = stringResource(R.string.perm_usage_access_desc),
                    isGranted = isUsageEnabled,
                    onClick = { showUsageDialog = true }
                )
                Hairline(modifier = Modifier.padding(start = 66.dp, end = 16.dp))
                ModernPermissionCard(
                    step = 2,
                    title = stringResource(R.string.perm_accessibility_title),
                    description = if (accessibilityNeedsReenable) {
                        if (accessibilityFailSafeActive) {
                            stringResource(R.string.perm_accessibility_failsafe_desc)
                        } else {
                            stringResource(R.string.perm_accessibility_reenable_desc)
                        }
                    } else {
                        stringResource(R.string.perm_accessibility_desc)
                    },
                    isGranted = isAccessibilityEnabled,
                    stateText = if (accessibilityNeedsReenable) {
                        if (accessibilityFailSafeActive) {
                            stringResource(R.string.perm_state_failsafe)
                        } else {
                            stringResource(R.string.perm_state_reenable)
                        }
                    } else {
                        null
                    },
                    onClick = { showAccessibilityDialog = true }
                )
                Hairline(modifier = Modifier.padding(start = 66.dp, end = 16.dp))
                ModernPermissionCard(
                    step = 3,
                    title = stringResource(R.string.perm_overlay_title),
                    description = stringResource(R.string.perm_overlay_desc),
                    isGranted = isOverlayEnabled,
                    onClick = { showOverlayDialog = true }
                )
                Hairline(modifier = Modifier.padding(start = 66.dp, end = 16.dp))
                ModernPermissionCard(
                    step = 4,
                    title = stringResource(R.string.perm_battery_title),
                    description = stringResource(R.string.perm_battery_desc),
                    isGranted = isBatteryExempted,
                    onClick = { showBatteryDialog = true }
                )
                Hairline(modifier = Modifier.padding(start = 66.dp, end = 16.dp))
                ModernPermissionCard(
                    step = 5,
                    title = stringResource(R.string.perm_notification_title),
                    description = stringResource(R.string.perm_notification_desc),
                    isGranted = isNotificationsEnabled,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.openNotificationSettings(context)
                        }
                    }
                )
            }
        }

        Text(
            text = stringResource(R.string.perm_guidance_text),
            fontSize = 12.sp,
            color = MutedGray,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SuccessGreen.copy(alpha = 0.07f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(icon = Icons.Default.Lock, tint = SuccessGreen, size = 36.dp)
            Text(
                text = stringResource(R.string.perm_privacy_msg),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PureBlack.copy(alpha = 0.8f),
                lineHeight = 17.sp
            )
        }

        LimitraPrimaryButton(
            text = stringResource(R.string.btn_start_protection),
            onClick = { onNavigateToDashboard() },
            enabled = canContinueToApp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModernPermissionCard(
    step: Int,
    title: String,
    description: String,
    isGranted: Boolean,
    stateText: String? = null,
    onClick: () -> Unit
) {
    val needsAttention = stateText != null
    val badgeColor by animateColorAsState(
        targetValue = when {
            needsAttention -> DangerRed
            isGranted -> SuccessGreen
            else -> CopperAccent
        },
        label = "permBadge"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted || needsAttention) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = if (isGranted && !needsAttention) 1f else 0.12f))
        ) {
            AnimatedContent(
                targetState = isGranted && !needsAttention,
                transitionSpec = { (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) togetherWith fadeOut() },
                label = "permState"
            ) { granted ->
                if (granted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = onColorFor(SuccessGreen), modifier = Modifier.size(18.dp))
                } else {
                    Text(
                        text = step.toString(),
                        fontFamily = LimitraDisplay,
                        fontSize = 17.sp,
                        color = badgeColor
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureBlack)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, fontSize = 12.sp, color = MutedGray, lineHeight = 16.sp)
        }

        if (!isGranted || needsAttention) {
            Text(
                text = stateText ?: stringResource(R.string.perm_state_grant),
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(badgeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}
