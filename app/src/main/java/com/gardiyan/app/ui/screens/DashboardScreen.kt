package com.gardiyan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.ui.theme.*
import com.gardiyan.app.viewmodel.GuardianViewModel

@Composable
fun DashboardScreen(
    viewModel: GuardianViewModel,
    onNavigateToSetup: () -> Unit,
    onNavigateToProtected: () -> Unit
) {
    val session by viewModel.userSession.collectAsState()
    val restrictedApps by viewModel.restrictedApps.collectAsState()
    val activeApps = remember(restrictedApps) { restrictedApps.filter { it.isActive } }
    val isMonitoring by viewModel.isMonitoringActive.collectAsState()

    val levelName = when (session?.level ?: 1) {
        1 -> "Çaylak"
        2 -> "Disiplinli"
        3 -> "Usta"
        else -> "Çaylak"
    }

    val streak = session?.consecutiveSuccessDays ?: 0
    val level = session?.level ?: 1
    val protectedCount = activeApps.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // — HEADER AREA —
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "GARDİYAN",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = PureBlack,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dijital Güvenlik ve Sınır Koruması",
                    fontSize = 13.sp,
                    color = MutedGray
                )
            }

            // Small "Active" status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isMonitoring) SuccessGreen.copy(alpha = 0.1f) else BorderGray)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isMonitoring) SuccessGreen else MutedGray)
                    )
                    Text(
                        text = if (isMonitoring) "AKTİF" else "PASİF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = if (isMonitoring) SuccessGreen else MutedGray
                    )
                }
            }
        }

        // — MAIN ACTION CARD —
        Card(
            onClick = onNavigateToSetup,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PureWhite.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PureWhite.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Yeni Kısıtlama Başlat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Uygulamaları seç, bir süre sınırı ayarla ve koru",
                        fontSize = 12.sp,
                        color = MutedGray,
                        lineHeight = 16.sp
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MutedGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // — COMPACT SUMMARY CARDS —
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryCard(
                title = "RÜTBE",
                value = levelName,
                subValue = "Level $level",
                icon = "🛡️",
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "SERİ",
                value = "$streak Gün",
                subValue = "Ardışık Başarı",
                icon = "🔥",
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "KORUNAN",
                value = "$protectedCount Hedef",
                subValue = "Uygulama",
                icon = "🔒",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToProtected
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    subValue: String,
    icon: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier
            .height(110.dp)
            .border(1.dp, BorderGray, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = MutedGray,
                    letterSpacing = 0.5.sp
                )
                Text(text = icon, fontSize = 14.sp)
            }
            Column {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = PureBlack
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subValue,
                    fontSize = 9.sp,
                    color = MutedGray
                )
            }
        }
    }
}
