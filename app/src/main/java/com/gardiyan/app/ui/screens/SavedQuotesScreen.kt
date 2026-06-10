package com.gardiyan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gardiyan.app.R
import com.gardiyan.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.saved_quotes_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PureBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back_desc),
                            tint = PureBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MatteSurface
                )
            )
        },
        containerColor = MatteSurface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Ekleme / Düzenleme Formu
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (editingQuoteId != null) stringResource(R.string.saved_quotes_edit_title) else stringResource(R.string.saved_quotes_add_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureBlack
                    )

                    OutlinedTextField(
                        value = tempQuoteText,
                        onValueChange = { tempQuoteText = it },
                        label = { Text(stringResource(R.string.settings_quote_text_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PureBlack,
                            unfocusedBorderColor = BorderGray,
                            focusedLabelColor = PureBlack
                        ),
                        singleLine = false,
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = tempQuoteAuthor,
                        onValueChange = { tempQuoteAuthor = it },
                        label = { Text(stringResource(R.string.settings_quote_author_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PureBlack,
                            unfocusedBorderColor = BorderGray,
                            focusedLabelColor = PureBlack
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (editingQuoteId != null) {
                            TextButton(
                                onClick = {
                                    editingQuoteId = null
                                    tempQuoteText = ""
                                    tempQuoteAuthor = ""
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.btn_cancel), color = MutedGray, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureBlack,
                                contentColor = OnPureBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(if (editingQuoteId != null) 1f else 2f)
                        ) {
                            Text(
                                text = if (editingQuoteId != null) stringResource(R.string.saved_quotes_btn_update) else stringResource(R.string.saved_quotes_btn_add),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Liste Bölümü
            Text(
                text = stringResource(R.string.saved_quotes_list_title_format, customQuotesList.size),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PureBlack
            )

            if (customQuotesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.saved_quotes_empty),
                        fontSize = 13.sp,
                        color = MutedGray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customQuotesList) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderGray.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkCharcoal),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isSelected,
                                    onCheckedChange = { isChecked ->
                                        customQuotesList = customQuotesList.map {
                                            if (it.id == item.id) it.copy(isSelected = isChecked) else it
                                        }
                                        saveCustomQuotes(prefs, customQuotesList)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = PureBlack)
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            tempQuoteText = item.text
                                            tempQuoteAuthor = item.author
                                            editingQuoteId = item.id
                                        }
                                ) {
                                    Text(
                                        text = item.text,
                                        fontSize = 13.sp,
                                        color = PureBlack,
                                        lineHeight = 18.sp
                                    )
                                    Text(
                                        text = "- ${item.author}",
                                        fontSize = 11.sp,
                                        color = MutedGray,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

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
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.btn_delete_desc),
                                        tint = DangerRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
