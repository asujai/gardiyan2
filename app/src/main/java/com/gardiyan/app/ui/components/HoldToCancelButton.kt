package com.gardiyan.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CANCEL_HOLD_DURATION_MS = 5000L

@Composable
fun FiveSecondHoldCancelButton(
    onCancelConfirmed: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(completed) {
        if (completed) {
            Toast.makeText(context, "Korumalar devre dışı bırakıldı.", Toast.LENGTH_LONG).show()
            onCancelConfirmed()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DangerRed.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "GÜVENLİK İPTALİ",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = DangerRed,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tüm kısıtlamaları kaldırmak için butona 5 saniye basılı tutun.",
                fontSize = 12.sp,
                color = MutedGray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ceza olarak level düşüşü ve kırmızı rozet uygulanacaktır.",
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                color = MutedGray.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 5 saniye basılı tutma butonu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isHolding) DangerRed.copy(alpha = 0.1f) else SoftDangerRed)
                    .border(
                        width = if (isHolding) 1.5.dp else 1.dp,
                        color = DangerRed.copy(alpha = if (isHolding) 0.8f else 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isHolding = true
                            progress = 0f
                            val steps = 50
                            val stepDelay = CANCEL_HOLD_DURATION_MS / steps
                            val timerJob = coroutineScope.launch {
                                for (i in 1..steps) {
                                    delay(stepDelay)
                                    if (!isHolding) return@launch
                                    progress = i / steps.toFloat()
                                }
                                completed = true
                                isHolding = false
                            }
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    if (event.changes.all { !it.pressed }) {
                                        isHolding = false
                                        progress = 0f
                                        timerJob.cancel()
                                        return@awaitEachGesture
                                    }
                                }
                            } finally {
                                timerJob.cancel()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // İlerleme çubuğu (soldan dolma)
                if (isHolding) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(DangerRed.copy(alpha = 0.2f))
                            .align(Alignment.CenterStart)
                    )
                }

                Text(
                    text = when {
                        completed -> "✓ KİLİTLER AÇILDI"
                        isHolding -> "BIRAKMAYIN · ${(progress * 5).toInt() + 1}s"
                        else -> "KORUMAYI KALDIR (5sn BASILI TUT)"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = if (isHolding) DangerRed else PureWhite
                )
            }
        }
    }
}
