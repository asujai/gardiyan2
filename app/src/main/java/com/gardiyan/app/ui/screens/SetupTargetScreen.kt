package com.gardiyan.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.ui.components.AppIconView
import com.gardiyan.app.ui.theme.*
import com.gardiyan.app.viewmodel.GuardianViewModel

@Composable
fun SetupTargetScreen(
    viewModel: GuardianViewModel,
    onBack: () -> Unit,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val installedApps = remember { viewModel.getInstalledApps(context) }
    var selectedApps by remember { mutableStateOf<Set<Pair<String, String>>>(emptySet()) }
    
    var selectedDurationPreset by remember { mutableStateOf(60) }
    var customDurationText by remember { mutableStateOf("") }
    
    val daysOfWeek = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    var selectedDays by remember { mutableStateOf(daysOfWeek.toSet()) }

    var isAppSheetVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val restrictedApps by viewModel.restrictedApps.collectAsState()
    val activeRestrictedPackages = remember(restrictedApps) {
        restrictedApps.filter { it.isActive }.mapTo(mutableSetOf()) { it.packageName }
    }
    val availableApps = remember(installedApps, activeRestrictedPackages) {
        installedApps.filterNot { it.second in activeRestrictedPackages }
    }

    val presetChoices = listOf(
        Pair("Test (10sn)", 0),
        Pair("15 Dk", 15),
        Pair("30 Dk", 30),
        Pair("1 Saat", 60),
        Pair("2 Saat", 120)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MatteSurface)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkCharcoal)
                            .border(1.dp, BorderGray, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = PureBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "KISITLAMA EKLE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = PureBlack,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Bütünleşik Form Paneli (Tek bir Bento / Minimal Kart yapısı)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // BÖLÜM 1: Uygulama Seçimi
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PureBlack.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = PureBlack, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "UYGULAMA SEÇİN",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MatteSurface)
                                    .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
                                    .clickable { 
                                        searchQuery = ""
                                        isAppSheetVisible = true 
                                    }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (selectedApps.isNotEmpty()) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            items(selectedApps.toList(), key = { it.second }) { app ->
                                                SelectedAppChip(
                                                    appName = app.first,
                                                    packageName = app.second,
                                                    onRemove = {
                                                        selectedApps = selectedApps - app
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Korunacak uygulamaları seçin...",
                                            fontSize = 13.sp,
                                            color = MutedGray,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MutedGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BorderGray, thickness = 1.dp)

                        // BÖLÜM 2: Süre Sınırı
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PureBlack.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, tint = PureBlack, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "GÜNLÜK KULLANIM LİMİTİ",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(presetChoices) { choice ->
                                    val isSelected = selectedDurationPreset == choice.second && customDurationText.isEmpty()
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (isSelected) PureBlack else MatteSurface)
                                            .border(1.dp, if (isSelected) PureBlack else BorderGray, RoundedCornerShape(999.dp))
                                            .clickable {
                                                selectedDurationPreset = choice.second
                                                customDurationText = ""
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = choice.first,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) OnPureBlack else PureBlack
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = customDurationText,
                                onValueChange = { newValue ->
                                    if (newValue.all { it.isDigit() }) {
                                        customDurationText = newValue
                                    }
                                },
                                label = { Text("Özel dakika sınırı girin", color = MutedGray, fontSize = 13.sp) },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PureWhite,
                                    unfocusedBorderColor = BorderGray,
                                    focusedContainerColor = MatteSurface,
                                    unfocusedContainerColor = MatteSurface,
                                    focusedTextColor = PureBlack,
                                    unfocusedTextColor = PureBlack
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        HorizontalDivider(color = BorderGray, thickness = 1.dp)

                        // BÖLÜM 3: Gün Seçimi
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PureBlack.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = PureBlack, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "KORUMA GÜNLERİ",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                daysOfWeek.forEach { day ->
                                    val isSelected = selectedDays.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) PureBlack else MatteSurface)
                                            .border(1.dp, if (isSelected) PureBlack else BorderGray, CircleShape)
                                            .clickable {
                                                selectedDays = if (isSelected) {
                                                    selectedDays - day
                                                } else {
                                                    selectedDays + day
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) OnPureBlack else PureBlack
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ekleme ve bitirme butonları
            item {
                val finalDuration = if (customDurationText.isNotEmpty()) {
                    customDurationText.toIntOrNull() ?: selectedDurationPreset
                } else {
                    selectedDurationPreset
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (selectedApps.isEmpty()) {
                                Toast.makeText(context, "Önce en az bir uygulama seçin", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedDays.isEmpty()) {
                                Toast.makeText(context, "Lütfen en az bir gün seçin", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val daysStr = selectedDays.joinToString(",")
                            if (finalDuration == 0) {
                                selectedApps.forEach { app ->
                                    viewModel.startQuickTest(context, app.second, app.first, testSeconds = 10, activeDays = daysStr)
                                }
                                Toast.makeText(context, "${selectedApps.size} uygulama için hızlı test başlatıldı!", Toast.LENGTH_SHORT).show()
                                onCompleted()
                            } else {
                                selectedApps.forEach { app ->
                                    viewModel.addRestrictedApp(app.second, app.first, finalDuration, activeDays = daysStr)
                                }
                                Toast.makeText(context, "${selectedApps.size} uygulama kısıtlamalara eklendi", Toast.LENGTH_SHORT).show()
                                selectedApps = emptySet()
                                onCompleted()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OnPureBlack),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (finalDuration == 0) "HIZLI TESTİ BAŞLAT (10sn)" else "KISITLAMALARI AKTİFLEŞTİR",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (restrictedApps.any { it.isActive }) {
                        OutlinedButton(
                            onClick = onCompleted,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureBlack),
                            border = BorderStroke(1.5.dp, BorderGray),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Text(
                                text = "İŞLEMLERİ TAMAMLA & KORUMAYI AÇ",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Custom Uygulama Arama/Seçim Bottom Sheet Overlay
        if (isAppSheetVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { isAppSheetVisible = false }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(DarkCharcoal)
                    .border(1.dp, BorderGray, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Drag Indicator
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(BorderGray)
                            .align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UYGULAMALARI SEÇİN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif,
                            color = PureBlack
                        )
                        IconButton(
                            onClick = { isAppSheetVisible = false },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MatteSurface)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = PureBlack, modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Arama Kutusu (Temizleme İkonu ile Birlikte)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Uygulama adı veya paket adı ara...", color = MutedGray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MutedGray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = MutedGray)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = BorderGray,
                            focusedContainerColor = MatteSurface,
                            unfocusedContainerColor = MatteSurface,
                            focusedTextColor = PureBlack,
                            unfocusedTextColor = PureBlack
                        ),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Filtrelenmiş Uygulamalar Listesi
                    val filteredApps = remember(searchQuery, availableApps) {
                        if (searchQuery.isBlank()) {
                            availableApps
                        } else {
                            availableApps.filter {
                                it.first.contains(searchQuery, ignoreCase = true) ||
                                it.second.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }
                    
                    // Liste ve Boş Durum Yönetimi
                    if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text("🔍", fontSize = 44.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Eşleşen Uygulama Yok",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureBlack
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"$searchQuery\" aramasına uygun yüklü uygulama bulunamadı.",
                                    fontSize = 12.sp,
                                    color = MutedGray,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredApps, key = { it.second }) { app ->
                                val isSelected = selectedApps.any { it.second == app.second }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) SuccessGreen.copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable {
                                            selectedApps = if (isSelected) {
                                                selectedApps.filterNot { it.second == app.second }.toSet()
                                            } else {
                                                selectedApps + app
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    AppIconView(packageName = app.second, modifier = Modifier.size(36.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.first,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = PureBlack
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = app.second,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MutedGray
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                    
                    // Alt Kısım: Seçimi Onayla Butonu
                    if (selectedApps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { isAppSheetVisible = false },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "SEÇİMİ ONAYLA (${selectedApps.size} UYGULAMA)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedAppChip(
    appName: String,
    packageName: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .wrapContentWidth()
            .height(38.dp)
            .border(1.dp, PureWhite.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppIconView(packageName = packageName, modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)))
            Text(text = appName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PureBlack)
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kaldır",
                    tint = MutedGray,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
