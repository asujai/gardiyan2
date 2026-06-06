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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    isBatteryExempted: Boolean,
    isNotificationsEnabled: Boolean,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val hasAllPermissions = isOverlayEnabled && isUsageEnabled && isAccessibilityEnabled && isBatteryExempted && isNotificationsEnabled

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

        // Üst Durum Kalkanı (Asimetrik ve Minimalist Koruma Halkası)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp)
        ) {
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "SİSTEM İZİNLERİ",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = PureBlack,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Gardiyan'ın kısıtlamaları dürüstçe uygulayabilmesi için aşağıdaki izinleri aktif edin. Tüm verileriniz cihazınızda yerel olarak işlenir.",
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = MutedGray,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Dikey Liste Şeklinde İzin Kartları
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModernPermissionCard(
                title = stringResource(R.string.perm_usage_access_title),
                description = stringResource(R.string.perm_usage_access_desc),
                isGranted = isUsageEnabled,
                onClick = { viewModel.openUsageStatsSettings(context) }
            )

            ModernPermissionCard(
                title = stringResource(R.string.perm_accessibility_title),
                description = stringResource(R.string.perm_accessibility_desc),
                isGranted = isAccessibilityEnabled,
                onClick = { viewModel.openAccessibilitySettings(context) }
            )

            ModernPermissionCard(
                title = stringResource(R.string.perm_overlay_title),
                description = stringResource(R.string.perm_overlay_desc),
                isGranted = isOverlayEnabled,
                onClick = { viewModel.openOverlaySettings(context) }
            )

            ModernPermissionCard(
                title = stringResource(R.string.perm_battery_title),
                description = stringResource(R.string.perm_battery_desc),
                isGranted = isBatteryExempted,
                onClick = { viewModel.requestBatteryOptimizationIgnore(context) }
            )

            ModernPermissionCard(
                title = stringResource(R.string.perm_notification_title),
                description = stringResource(R.string.perm_notification_desc),
                isGranted = isNotificationsEnabled,
                onClick = { viewModel.openNotificationSettings(context) }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Başlat Butonu
        Button(
            onClick = { onNavigateToDashboard() },
            enabled = hasAllPermissions,
            colors = ButtonDefaults.buttonColors(
                containerColor = PureBlack,
                contentColor = OnPureBlack,
                disabledContainerColor = BorderGray,
                disabledContentColor = MutedGray.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = "KORUMA PANELİNİ BAŞLAT",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun ModernPermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (isGranted) SuccessGreen.copy(alpha = 0.2f) else BorderGray, RoundedCornerShape(20.dp))
            .clickable(enabled = !isGranted) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) SuccessGreen.copy(alpha = 0.03f) else DarkCharcoal
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Durum Emojisi
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) SuccessGreen.copy(alpha = 0.08f) else DangerRed.copy(alpha = 0.08f))
            ) {
                Text(text = if (isGranted) "🛡️" else "🔑", fontSize = 18.sp)
            }

            // Metinler
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = MutedGray,
                    lineHeight = 14.sp
                )
            }

            // Buton Durumu
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isGranted) SuccessGreen.copy(alpha = 0.1f) else DangerRed.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isGranted) "AKTİF" else "YETKİ VER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isGranted) SuccessGreen else DangerRed,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
