package com.gardiyan.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.R
import com.gardiyan.app.ui.components.LimitraCard
import com.gardiyan.app.ui.components.LimitraPrimaryButton
import com.gardiyan.app.ui.components.LimitraSecondaryButton
import com.gardiyan.app.ui.components.ScreenHeader
import com.gardiyan.app.ui.theme.*

@Composable
fun SavedQuotesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("gardiyan_settings", android.content.Context.MODE_PRIVATE) }
    var customQuotesList by remember { mutableStateOf(loadCustomQuotes(prefs)) }

    var tempQuoteText by remember { mutableStateOf("") }
    var tempQuoteAuthor by remember { mutableStateOf("") }
    var editingQuoteId by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CopperAccent,
        unfocusedBorderColor = BorderGray,
        focusedLabelColor = CopperAccent,
        unfocusedLabelColor = MutedGray,
        focusedContainerColor = MatteSurface,
        unfocusedContainerColor = MatteSurface,
        focusedTextColor = PureBlack,
        unfocusedTextColor = PureBlack,
        cursorColor = CopperAccent
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MatteSurface)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            ScreenHeader(
                title = stringResource(R.string.saved_quotes_title),
                onBack = onBack
            )
        }

        // Ekleme / Düzenleme Formu
        item {
            LimitraCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (editingQuoteId != null) stringResource(R.string.saved_quotes_edit_title) else stringResource(R.string.saved_quotes_add_title),
                        fontFamily = LimitraDisplay,
                        fontSize = 22.sp,
                        color = PureBlack
                    )

                    OutlinedTextField(
                        value = tempQuoteText,
                        onValueChange = { tempQuoteText = it },
                        label = { Text(stringResource(R.string.settings_quote_text_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        shape = RoundedCornerShape(16.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = tempQuoteAuthor,
                        onValueChange = { tempQuoteAuthor = it },
                        label = { Text(stringResource(R.string.settings_quote_author_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (editingQuoteId != null) {
                            LimitraSecondaryButton(
                                text = stringResource(R.string.btn_cancel),
                                onClick = {
                                    editingQuoteId = null
                                    tempQuoteText = ""
                                    tempQuoteAuthor = ""
                                },
                                contentColor = MutedGray,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        LimitraPrimaryButton(
                            text = if (editingQuoteId != null) stringResource(R.string.saved_quotes_btn_update) else stringResource(R.string.saved_quotes_btn_add),
                            onClick = {
                                if (tempQuoteText.trim().isNotEmpty()) {
                                    val authorText = tempQuoteAuthor.trim().ifEmpty { context.getString(R.string.quote_author_anonymous) }
                                    if (editingQuoteId != null) {
                                        customQuotesList = customQuotesList.map {
                                            if (it.id == editingQuoteId) {
                                                it.copy(text = tempQuoteText.trim(), author = authorText)
                                            } else it
                                        }
                                        editingQuoteId = null
                                    } else {
                                        val newItem = CustomQuoteItem(
                                            id = System.currentTimeMillis().toString(),
                                            text = tempQuoteText.trim(),
                                            author = authorText,
                                            isSelected = true
                                        )
                                        customQuotesList = customQuotesList + newItem
                                    }
                                    saveCustomQuotes(prefs, customQuotesList)
                                    tempQuoteText = ""
                                    tempQuoteAuthor = ""
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_quote_empty_error),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            height = 52.dp,
                            modifier = Modifier.weight(if (editingQuoteId != null) 1f else 2f)
                        )
                    }
                }
            }
        }

        // Liste Bölümü
        item {
            Text(
                text = stringResource(R.string.saved_quotes_list_title_format, customQuotesList.size),
                modifier = Modifier.padding(top = 6.dp, start = 2.dp),
                fontFamily = LimitraDisplay,
                fontSize = 22.sp,
                color = PureBlack
            )
        }

        if (customQuotesList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.saved_quotes_empty),
                        fontFamily = LimitraDisplay,
                        fontStyle = FontStyle.Italic,
                        fontSize = 18.sp,
                        color = MutedGray
                    )
                }
            }
        } else {
            items(customQuotesList, key = { it.id }) { item ->
                val isEditing = editingQuoteId == item.id
                LimitraCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    borderColor = if (isEditing) CopperAccent else BorderGray,
                    elevated = false
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        QuoteCheck(
                            checked = item.isSelected,
                            onToggle = {
                                val isChecked = !item.isSelected
                                customQuotesList = customQuotesList.map {
                                    if (it.id == item.id) it.copy(isSelected = isChecked) else it
                                }
                                saveCustomQuotes(prefs, customQuotesList)
                            }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    tempQuoteText = item.text
                                    tempQuoteAuthor = item.author
                                    editingQuoteId = item.id
                                }
                        ) {
                            Text(
                                text = "“${item.text}”",
                                fontFamily = LimitraDisplay,
                                fontStyle = FontStyle.Italic,
                                fontSize = 18.sp,
                                lineHeight = 25.sp,
                                color = if (item.isSelected) PureBlack else MutedGray
                            )
                            Text(
                                text = item.author,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = MutedGray,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                customQuotesList = customQuotesList.filter { it.id != item.id }
                                saveCustomQuotes(prefs, customQuotesList)
                                if (editingQuoteId == item.id) {
                                    editingQuoteId = null
                                    tempQuoteText = ""
                                    tempQuoteAuthor = ""
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.btn_delete_desc),
                                tint = MutedGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

/** Söz seçimi: yuvarlak, seçilince accent dolgulu. */
@Composable
private fun QuoteCheck(checked: Boolean, onToggle: () -> Unit) {
    val bg by animateColorAsState(if (checked) CopperAccent else androidx.compose.ui.graphics.Color.Transparent, label = "quoteCheck")
    Box(
        modifier = Modifier
            .padding(top = 3.dp)
            .size(24.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.5.dp, if (checked) CopperAccent else BorderGray, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(Icons.Default.Check, contentDescription = null, tint = OnAccent, modifier = Modifier.size(15.dp))
        }
    }
}
