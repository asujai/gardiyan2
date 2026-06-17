package com.gardiyan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
    isBatteryExempted: Boolean,
    isNotificationsEnabled: Boolean,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (hasAllPermissions) SuccessGreen.copy(alpha = 0.05f) else DangerRed.copy(alpha = 0.03f))
                    .border(1.5.dp, if (hasAllPermissions) SuccessGreen.copy(alpha = 0.3f) else DangerRed.copy(alpha = 0.15f), CircleShape)
            )
            Icon(
                imageVector = if (hasAllPermissions) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (hasAllPermissions) SuccessGreen else DangerRed,
                modifier = Modifier.size(40.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.perm_title_header),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = PureBlack
            )
            Text(
                text = stringResource(R.string.perm_desc_header),
                fontSize = 12.sp,
                color = MutedGray,
                lineHeight = 18.sp
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PureBlack.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = PureBlack.copy(alpha = 0.02f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.perm_guidance_text),
                fontSize = 12.sp,
                color = MutedGray,
                lineHeight = 18.sp,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (accessibilityNeedsReenable) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DangerRed.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
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
                        text = stringResource(R.string.accessibility_reenable_warning),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = DangerRed,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SuccessGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.1f))
                ) {
                    Text(text = "🛡️", fontSize = 16.sp)
                }

                Text(
                    text = stringResource(R.string.perm_privacy_msg),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedGray,
                    lineHeight = 16.sp
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModernPermissionCard(
                title = stringResource(R.string.perm_usage_access_title),
                description = stringResource(R.string.perm_usage_access_desc),
                isGranted = isUsageEnabled,
                onClick = { showUsageDialog = true }
            )

            ModernPermissionCard(
                title = stringResource(R.string.perm_accessibility_title),
                description = if (accessibilityNeedsReenable) {
                    stringResource(R.string.perm_accessibility_reenable_desc)
                } else {
                    stringResource(R.string.perm_accessibility_desc)
                },
                isGranted = isAccessibilityEnabled,
                stateText = if (accessibilityNeedsReenable) {
                    stringResource(R.string.perm_state_reenable)
                } else {
                    null
                },
                onClick = { showAccessibilityDialog = true }
            )

            ModernPermissionCard(
                title = stringResource(R.string.perm_overlay_title),
                description = stringResource(R.string.perm_overlay_desc),
                isGranted = isOverlayEnabled,
                onClick = { showOverlayDialog = true }
            )

            ModernPermissionCard(
                title = stringResource(R.string.perm_battery_title),
                description = stringResource(R.string.perm_battery_desc),
                isGranted = isBatteryExempted,
                onClick = { showBatteryDialog = true }
            )

            ModernPermissionCard(
                title = stringResource(R.string.perm_notification_title),
                description = stringResource(R.string.perm_notification_desc),
                isGranted = isNotificationsEnabled,
                onClick = { viewModel.openNotificationSettings(context) }
            )
        }

        Button(
            onClick = { onNavigateToDashboard() },
            enabled = hasAllPermissions,
            colors = ButtonDefaults.buttonColors(
                containerColor = PureBlack,
                contentColor = OnPureBlack,
                disabledContainerColor = BorderGray
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text(stringResource(R.string.btn_start_protection), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModernPermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    stateText: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isGranted) SuccessGreen.copy(alpha = 0.2f) else BorderGray, RoundedCornerShape(20.dp))
            .clickable(enabled = !isGranted) { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isGranted) SuccessGreen.copy(alpha = 0.1f) else DangerRed.copy(alpha = 0.1f))
            ) {
                Text(text = if (isGranted) "🛡️" else "🔑", fontSize = 18.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureBlack)
                Text(text = description, fontSize = 10.sp, color = MutedGray, lineHeight = 14.sp)
            }

            Text(
                text = stateText ?: if (isGranted) stringResource(R.string.perm_state_active) else stringResource(R.string.perm_state_grant),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isGranted) SuccessGreen else DangerRed
            )
        }
    }
}
