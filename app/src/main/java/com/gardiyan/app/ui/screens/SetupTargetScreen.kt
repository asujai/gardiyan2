package com.gardiyan.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import com.gardiyan.app.ui.components.AnimatedSheet
import com.gardiyan.app.ui.components.Hairline
import com.gardiyan.app.ui.components.IconBadge
import com.gardiyan.app.ui.components.LimitraCard
import com.gardiyan.app.ui.components.LimitraIcons
import com.gardiyan.app.ui.components.LimitraPrimaryButton
import com.gardiyan.app.ui.components.ScreenHeader
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
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
    var restrictionName by remember { mutableStateOf("") }
    var activeWindowEnabled by remember { mutableStateOf(false) }
    var activeStartMinutes by remember { mutableStateOf(0) }
    var activeEndMinutes by remember { mutableStateOf(8 * 60) }
    
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

    val presetChoices = remember(context) {
        emptyList<Pair<String, Int>>()
    }

    val currentTotalMinutes = selectedHours * 60 + selectedMinutes

    Box(modifier = Modifier.fillMaxSize().background(MatteSurface)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ScreenHeader(
                    title = stringResource(R.string.dashboard_add_restriction),
                    onBack = onBack
                )
            }

            // Form paneli tek kart içinde
            item {
                LimitraCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        // SECTION 1: Uygulama seçimi
                        Column {
                            FormLabel(stringResource(R.string.setup_target_select_app_title))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MatteSurface)
                                    .border(
                                        1.dp,
                                        if (selectedApps.isNotEmpty()) CopperAccent.copy(alpha = 0.5f) else BorderGray,
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        searchQuery = ""
                                        isAppSheetVisible = true
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
                                                },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.setup_target_select_app_placeholder),
                                        fontSize = 14.sp,
                                        color = MutedGray,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(CopperAccent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = LimitraIcons.Plus,
                                        contentDescription = null,
                                        tint = CopperAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // SECTION 2: Günlük limit
                        Column {
                            FormLabel(stringResource(R.string.setup_target_daily_limit))
                            Spacer(modifier = Modifier.height(12.dp))

                            if (presetChoices.isNotEmpty()) {
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
                            }

                            // Saat / dakika seçici
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(MatteSurface)
                                    .border(1.dp, BorderGray, RoundedCornerShape(22.dp))
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TimeUnitStepper(
                                    label = stringResource(R.string.setup_target_hour_label),
                                    value = selectedHours,
                                    incDescription = stringResource(R.string.setup_target_hour_inc_desc),
                                    decDescription = stringResource(R.string.setup_target_hour_dec_desc),
                                    onInc = { if (selectedHours < 23) selectedHours++ },
                                    onDec = { if (selectedHours > 0) selectedHours-- }
                                )
                                Text(
                                    text = ":",
                                    fontFamily = LimitraDisplay,
                                    fontSize = 40.sp,
                                    color = MutedGray,
                                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 22.dp)
                                )
                                TimeUnitStepper(
                                    label = stringResource(R.string.setup_target_minute_label),
                                    value = selectedMinutes,
                                    incDescription = stringResource(R.string.setup_target_minute_inc_desc),
                                    decDescription = stringResource(R.string.setup_target_minute_dec_desc),
                                    onInc = { if (selectedMinutes < 59) selectedMinutes++ },
                                    onDec = { if (selectedMinutes > 0) selectedMinutes-- }
                                )
                            }
                        }

                        Hairline()

                        // SECTION 3: Aktif saat aralığı
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    FormLabel(stringResource(R.string.setup_target_active_window))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.setup_target_active_window_desc),
                                        fontSize = 12.sp,
                                        color = MutedGray,
                                        lineHeight = 17.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = activeWindowEnabled,
                                    onCheckedChange = { activeWindowEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = OnAccent,
                                        checkedTrackColor = CopperAccent,
                                        checkedBorderColor = CopperAccent,
                                        uncheckedThumbColor = MutedGray,
                                        uncheckedTrackColor = WarmGray,
                                        uncheckedBorderColor = BorderGray
                                    )
                                )
                            }

                            AnimatedVisibility(
                                visible = activeWindowEnabled,
                                enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 400f)) + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    ScheduleTimeField(
                                        label = stringResource(R.string.setup_target_start_time),
                                        minutes = activeStartMinutes,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, hour, minute -> activeStartMinutes = hour * 60 + minute },
                                                activeStartMinutes / 60,
                                                activeStartMinutes % 60,
                                                true
                                            ).show()
                                        }
                                    )
                                    Text(
                                        "–",
                                        color = MutedGray,
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(bottom = 14.dp)
                                    )
                                    ScheduleTimeField(
                                        label = stringResource(R.string.setup_target_end_time),
                                        minutes = activeEndMinutes,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, hour, minute -> activeEndMinutes = hour * 60 + minute },
                                                activeEndMinutes / 60,
                                                activeEndMinutes % 60,
                                                true
                                            ).show()
                                        }
                                    )
                                }
                            }
                        }

                        Hairline()

                        // SECTION 4: Günler
                        Column {
                            FormLabel(stringResource(R.string.setup_target_days))
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                daysOfWeek.forEach { day ->
                                    val isSelected = selectedDays.contains(day)
                                    val dayBg by animateColorAsState(
                                        if (isSelected) CopperAccent.copy(alpha = 0.14f) else MatteSurface,
                                        label = "dayBg"
                                    )
                                    val dayFg by animateColorAsState(
                                        if (isSelected) CopperAccent else MutedGray,
                                        label = "dayFg"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(horizontal = 2.5.dp)
                                            .clip(CircleShape)
                                            .background(dayBg)
                                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) CopperAccent else BorderGray, CircleShape)
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
                                            color = dayFg,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Hairline()

                        // SECTION 5: İsim (isteğe bağlı)
                        Column {
                            FormLabel(stringResource(R.string.setup_target_restriction_name_optional))
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = restrictionName,
                                onValueChange = { if (it.length <= 50) restrictionName = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.setup_target_restriction_name_placeholder),
                                        color = MutedGray,
                                        fontSize = 14.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = MutedGray, modifier = Modifier.size(20.dp))
                                },
                                trailingIcon = {
                                    if (restrictionName.isNotEmpty()) {
                                        IconButton(onClick = { restrictionName = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = MutedGray)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CopperAccent,
                                    unfocusedBorderColor = BorderGray,
                                    focusedContainerColor = MatteSurface,
                                    unfocusedContainerColor = MatteSurface,
                                    focusedTextColor = PureBlack,
                                    unfocusedTextColor = PureBlack,
                                    cursorColor = CopperAccent
                                )
                            )
                        }
                    }
                }
            }

            // Bottom actions
            item {
                LimitraPrimaryButton(
                    text = stringResource(R.string.setup_target_btn_activate),
                    icon = LimitraIcons.Shield,
                    onClick = {
                        if (selectedApps.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.setup_target_error_no_app), Toast.LENGTH_SHORT).show()
                            return@LimitraPrimaryButton
                        }
                        if (selectedDays.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.setup_target_error_no_day), Toast.LENGTH_SHORT).show()
                            return@LimitraPrimaryButton
                        }
                        if (currentTotalMinutes <= 0) {
                            Toast.makeText(context, context.getString(R.string.setup_target_error_zero_duration), Toast.LENGTH_SHORT).show()
                            return@LimitraPrimaryButton
                        }
                        val daysStr = daysOfWeek.filter { it in selectedDays }.joinToString(",")
                        viewModel.addRestrictionGroup(
                            restrictionName = restrictionName,
                            apps = selectedApps.toList(),
                            dailyLimitMinutes = currentTotalMinutes,
                            activeDays = daysStr,
                            activeWindowEnabled = activeWindowEnabled,
                            activeStartMinutes = activeStartMinutes,
                            activeEndMinutes = activeEndMinutes
                        )
                        Toast.makeText(
                            context,
                            context.getString(R.string.setup_target_toast_added, selectedApps.size),
                            Toast.LENGTH_SHORT
                        ).show()
                        selectedApps = emptySet()
                        onCompleted()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Uygulama seçme sayfası
        AnimatedSheet(
            visible = isAppSheetVisible,
            onDismiss = { isAppSheetVisible = false },
            heightFraction = 0.9f
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.setup_target_select_app_title),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MutedGray
                )
                IconButton(
                    onClick = { isAppSheetVisible = false },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MatteSurface)
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_close), tint = PureBlack, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arama kutusu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.setup_target_search_placeholder), color = MutedGray, fontSize = 14.sp) },
                leadingIcon = { Icon(LimitraIcons.Search, contentDescription = null, tint = MutedGray, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.btn_clean), tint = MutedGray)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CopperAccent,
                    unfocusedBorderColor = BorderGray,
                    focusedContainerColor = MatteSurface,
                    unfocusedContainerColor = MatteSurface,
                    focusedTextColor = PureBlack,
                    unfocusedTextColor = PureBlack,
                    cursorColor = CopperAccent
                ),
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        IconBadge(icon = LimitraIcons.Search, tint = MutedGray, size = 60.dp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stringResource(R.string.setup_target_no_match),
                            fontFamily = LimitraDisplay,
                            fontSize = 22.sp,
                            color = PureBlack
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.setup_target_no_match_desc, searchQuery),
                            fontSize = 13.sp,
                            color = MutedGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredApps, key = { it.second }) { app ->
                        val isSelected = selectedApps.any { it.second == app.second }
                        val rowBg by animateColorAsState(
                            if (isSelected) CopperAccent.copy(alpha = 0.10f) else Color.Transparent,
                            label = "appRowBg"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(rowBg)
                                .clickable {
                                    selectedApps = if (isSelected) {
                                        selectedApps.filterNot { it.second == app.second }.toSet()
                                    } else {
                                        selectedApps + app
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AppIconView(packageName = app.second, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.first,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = PureBlack
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = app.second,
                                    fontSize = 11.sp,
                                    color = MutedGray,
                                    maxLines = 1
                                )
                            }
                            SelectionCheck(selected = isSelected)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedApps.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                LimitraPrimaryButton(
                    text = stringResource(R.string.setup_target_confirm_selection, selectedApps.size),
                    onClick = { isAppSheetVisible = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 12.dp)
                )
            }
        }
    }
}

/** Form bölüm etiketi. */
@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MutedGray,
        letterSpacing = 1.sp
    )
}

/** Yuvarlak seçim işareti: seçilince dolar ve tik yaylı biçimde belirir. */
@Composable
private fun SelectionCheck(selected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "checkScale"
    )
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .border(1.5.dp, if (selected) CopperAccent else BorderGray, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(CopperAccent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(16.dp))
        }
    }
}

/** Saat veya dakika için büyük serif rakam ve yukarı/aşağı düğmeleri. */
@Composable
private fun TimeUnitStepper(
    label: String,
    value: Int,
    incDescription: String,
    decDescription: String,
    onInc: () -> Unit,
    onDec: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(92.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MutedGray
        )
        RepeatingIconButton(onClick = onInc, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = incDescription,
                tint = PureBlack
            )
        }
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut())
                } else {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                }
            },
            label = "stepper"
        ) { v ->
            Text(
                text = String.format(Locale.ROOT, "%02d", v),
                fontFamily = LimitraDisplay,
                fontSize = 44.sp,
                lineHeight = 48.sp,
                color = PureBlack
            )
        }
        RepeatingIconButton(onClick = onDec, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = decDescription,
                tint = PureBlack
            )
        }
    }
}

@Composable
fun SelectedAppChip(
    appName: String,
    packageName: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(DarkCharcoal)
            .border(1.dp, BorderGray, RoundedCornerShape(99.dp))
            .padding(start = 5.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppIconView(packageName = packageName, modifier = Modifier.size(26.dp).clip(CircleShape))
        Text(text = appName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PureBlack, maxLines = 1)
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.btn_close),
                tint = MutedGray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ScheduleTimeField(
    label: String,
    minutes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MutedGray
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MatteSurface)
                .border(1.dp, BorderGray, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60),
                fontFamily = LimitraDisplay,
                fontSize = 28.sp,
                color = PureBlack
            )
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
