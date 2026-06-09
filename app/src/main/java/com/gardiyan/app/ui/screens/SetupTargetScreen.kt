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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.gardiyan.app.R
import com.gardiyan.app.data.repository.GuardianRepository
import com.gardiyan.app.ui.components.AppIconView
import com.gardiyan.app.ui.theme.*
import com.gardiyan.app.viewmodel.GuardianViewModel
import java.util.Locale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetupTargetScreen(
    viewModel: GuardianViewModel,
    onBack: () -> Unit,
    onCompleted: () -> Unit
) {
    val context = LocalContext.current
    val installedApps = remember { viewModel.getInstalledApps(context) }
    var selectedApps by remember { mutableStateOf<Set<Pair<String, String>>>(emptySet()) }
    
    // Time picker states
    var selectedHours by remember { mutableStateOf(1) }
    var selectedMinutes by remember { mutableStateOf(0) }
    
    val daysOfWeek = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    val daysMap = mapOf(
        "Pzt" to R.string.day_mon,
        "Sal" to R.string.day_tue,
        "Çar" to R.string.day_wed,
        "Per" to R.string.day_thu,
        "Cum" to R.string.day_fri,
        "Cmt" to R.string.day_sat,
        "Paz" to R.string.day_sun
    )
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
        Pair(stringResource(R.string.setup_target_test_10s), 0)
    )

    val currentTotalMinutes = selectedHours * 60 + selectedMinutes

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
                            contentDescription = stringResource(R.string.btn_close),
                            tint = PureBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.setup_target_add),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Form panel enclosed in a single card
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
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // SECTION 1: App Selection
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.setup_target_select_app_title),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MutedGray,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MatteSurface)
                                    .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                                    .clickable { 
                                        searchQuery = ""
                                        isAppSheetVisible = true 
                                    }
                                    .padding(14.dp)
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
                                            text = stringResource(R.string.setup_target_select_app_placeholder),
                                            fontSize = 13.sp,
                                            color = MutedGray,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        tint = MutedGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BorderGray, thickness = 1.dp)

                        // SECTION 2: Daily Limit
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.setup_target_daily_limit),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MutedGray,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(presetChoices) { choice ->
                                    val isSelected = currentTotalMinutes == choice.second
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(if (isSelected) PureBlack else MatteSurface)
                                            .border(1.dp, if (isSelected) PureBlack else BorderGray, RoundedCornerShape(99.dp))
                                            .clickable {
                                                selectedHours = choice.second / 60
                                                selectedMinutes = choice.second % 60
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = choice.first,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) OnPureBlack else PureBlack
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom Hour/Minute Up-Down Picker
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MatteSurface, RoundedCornerShape(16.dp))
                                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Hours Column
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(80.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.setup_target_hour_label),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedGray,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .background(DarkCharcoal, RoundedCornerShape(12.dp))
                                            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                                    ) {
                                        RepeatingIconButton(
                                            onClick = {
                                                if (selectedHours < 23) selectedHours++
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = stringResource(R.string.setup_target_hour_inc_desc),
                                                tint = MutedGray,
                                                modifier = Modifier.rotate(180f)
                                            )
                                        }
                                        Text(
                                            text = String.format(Locale.ROOT, "%02d", selectedHours),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                        )
                                        RepeatingIconButton(
                                            onClick = {
                                                if (selectedHours > 0) selectedHours--
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = stringResource(R.string.setup_target_hour_dec_desc),
                                                tint = MutedGray
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = ":",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedGray,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                                )

                                // Minutes Column
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(80.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.setup_target_minute_label),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedGray,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .background(DarkCharcoal, RoundedCornerShape(12.dp))
                                            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                                    ) {
                                        RepeatingIconButton(
                                            onClick = {
                                                if (selectedMinutes < 59) selectedMinutes++
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = stringResource(R.string.setup_target_minute_inc_desc),
                                                tint = MutedGray,
                                                modifier = Modifier.rotate(180f)
                                            )
                                        }
                                        Text(
                                            text = String.format(Locale.ROOT, "%02d", selectedMinutes),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PureBlack,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                        )
                                        RepeatingIconButton(
                                            onClick = {
                                                if (selectedMinutes > 0) selectedMinutes--
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = stringResource(R.string.setup_target_minute_dec_desc),
                                                tint = MutedGray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BorderGray, thickness = 1.dp)

                        // SECTION 3: Days Selection
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MatteSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MutedGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.setup_target_days),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MutedGray,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                daysOfWeek.forEach { day ->
                                    val isSelected = selectedDays.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(horizontal = 2.dp)
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
                                            text = stringResource(daysMap[day]!!),
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

            // Bottom actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (selectedApps.isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.setup_target_error_no_app), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (selectedDays.isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.setup_target_error_no_day), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val daysStr = selectedDays.joinToString(",")
                            if (currentTotalMinutes == 0) {
                                selectedApps.forEach { app ->
                                    viewModel.startQuickTest(context, app.second, app.first, testSeconds = 10, activeDays = daysStr)
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.setup_target_toast_test_start, selectedApps.size),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onCompleted()
                            } else {
                                selectedApps.forEach { app ->
                                    viewModel.addRestrictedApp(app.second, app.first, currentTotalMinutes, activeDays = daysStr)
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.setup_target_toast_added, selectedApps.size),
                                    Toast.LENGTH_SHORT
                                ).show()
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
                            text = if (currentTotalMinutes == 0) stringResource(R.string.setup_target_btn_test) else stringResource(R.string.setup_target_btn_activate),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Search Bottom Sheet Overlay
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
                            text = stringResource(R.string.setup_target_select_app_title),
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
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_close), tint = PureBlack, modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Search box
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.setup_target_search_placeholder), color = MutedGray, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MutedGray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.btn_clean), tint = MutedGray)
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
                                    text = stringResource(R.string.setup_target_no_match),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureBlack
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.setup_target_no_match_desc, searchQuery),
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
                                text = stringResource(R.string.setup_target_confirm_selection, selectedApps.size),
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
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp)),
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
                    contentDescription = stringResource(R.string.btn_close),
                    tint = MutedGray,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}


@Composable
fun RepeatingIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val currentClickListener by rememberUpdatedState(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                coroutineScope {
                    detectTapGestures(
                        onTap = { currentClickListener() },
                        onPress = { offset ->
                            val press = PressInteraction.Press(offset)
                            interactionSource.emit(press)
                            val job = launch {
                                delay(500)
                                while (true) {
                                    currentClickListener()
                                    delay(100)
                                }
                            }
                            try {
                                awaitRelease()
                                interactionSource.emit(PressInteraction.Release(press))
                            } catch (c: Exception) {
                                interactionSource.emit(PressInteraction.Cancel(press))
                            } finally {
                                job.cancel()
                            }
                        }
                    )
                }
            }
            .indication(
                interactionSource = interactionSource,
                indication = ripple(bounded = false)
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
