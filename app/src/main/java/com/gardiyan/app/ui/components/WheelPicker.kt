package com.gardiyan.app.ui.components

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.ui.theme.BorderGray
import com.gardiyan.app.ui.theme.PureBlack
import com.gardiyan.app.R
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 3,
    itemHeight: Dp = 48.dp,
    selectedTextColor: Color = PureBlack,
    unselectedTextColor: Color = Color.Gray
) {
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)
    val midOffset = visibleItemsCount / 2

    // Emit selected item when scrolling stops or active item changes
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index in items.indices) {
                    onItemSelected(index)
                }
            }
    }

    LaunchedEffect(initialIndex) {
        if (lazyListState.firstVisibleItemIndex != initialIndex) {
            lazyListState.scrollToItem(initialIndex)
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Highlight middle element
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
        )

        LazyColumn(
            state = lazyListState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = itemHeight * midOffset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val isSelected = remember { derivedStateOf { lazyListState.firstVisibleItemIndex == index } }.value
                val scale = if (isSelected) 1.15f else 0.9f
                val alpha = if (isSelected) 1f else 0.4f

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        fontSize = (15 * scale).sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = if (isSelected) selectedTextColor else unselectedTextColor,
                        modifier = Modifier.alpha(alpha),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun DurationWheelPicker(
    initialHours: Int,
    initialMinutes: Int,
    onDurationChanged: (hours: Int, minutes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val language = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language
    val hoursList = remember(language) { (0..23).map(context::localizedHours) }
    val minutesList = remember(language) { (0..59).map(context::localizedMinutes) }

    var selectedHours by remember { mutableStateOf(initialHours.coerceIn(0, 23)) }
    var selectedMinutes by remember { mutableStateOf(initialMinutes.coerceIn(0, 59)) }

    // Sync state when initial parameters change
    LaunchedEffect(initialHours, initialMinutes) {
        selectedHours = initialHours.coerceIn(0, 23)
        selectedMinutes = initialMinutes.coerceIn(0, 59)
    }

    val initialHoursIndex = selectedHours
    val initialMinutesIndex = selectedMinutes

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPicker(
            items = hoursList,
            initialIndex = initialHoursIndex,
            onItemSelected = { index ->
                selectedHours = index
                onDurationChanged(index, selectedMinutes)
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        WheelPicker(
            items = minutesList,
            initialIndex = initialMinutesIndex,
            onItemSelected = { index ->
                selectedMinutes = index
                onDurationChanged(selectedHours, index)
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )
    }
}

fun Context.localizedHours(value: Int): String {
    return getString(
        if (value == 1) R.string.protected_apps_hour else R.string.protected_apps_hours,
        value
    )
}

fun Context.localizedMinutes(value: Int): String {
    return getString(
        if (value == 1) R.string.protected_apps_minute else R.string.protected_apps_minutes,
        value
    )
}
