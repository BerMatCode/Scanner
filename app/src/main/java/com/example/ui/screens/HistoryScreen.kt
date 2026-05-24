package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScanRecord
import com.example.ui.ScanViewModel
import com.example.ui.components.GlassCard
import com.example.ui.components.TypeBadge
import com.example.ui.theme.DarkPurple
import com.example.ui.theme.ElectricBlue
import com.example.utils.QRParser
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val records by viewModel.historyRecords.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Filter list based on search term
    val filteredRecords = remember(records, searchQuery) {
        if (searchQuery.isBlank()) {
            records
        } else {
            records.filter {
                it.rawContent.contains(searchQuery, ignoreCase = true) ||
                it.qrType.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Historial",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (records.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                .testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Wipe history",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar escaneos...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar busqueda")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("search_bar_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPurple,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredRecords.isEmpty()) {
                // Render beautiful Empty State view
                EmptyStateLayout(hasSearchActive = searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = filteredRecords,
                        key = { it.id }
                    ) { record ->
                        HistoryRecordItem(
                            record = record,
                            onDelete = { viewModel.deleteRecord(record) },
                            onItemClick = {
                                // Double check parsing and execute principal action directly
                                val parsed = QRParser.parse(record.rawContent)
                                performPrimaryAction(context, parsed, record.rawContent, clipboardManager)
                            },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }

            // Wipe confirmation popup
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("¿Limpiar historial?") },
                    text = { Text("Esta acción eliminará de forma permanente todos los registros de tu historial de escaneos. No se puede deshacer.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearAllHistory()
                                showDeleteConfirmDialog = false
                                Toast.makeText(context, "Historial vaciado", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("confirm_clear_button")
                        ) {
                            Text("Eliminar todo", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Cancelar")
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

@Composable
fun EmptyStateLayout(hasSearchActive: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(DarkPurple.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (hasSearchActive) Icons.Default.SearchOff else Icons.Default.QrCodeScanner,
                contentDescription = "No items",
                tint = DarkPurple,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val title = if (hasSearchActive) "Sin resultados" else "Historial Vacío"
        val subtitle = if (hasSearchActive) {
            "No pudimos encontrar ningún escaneo que coincida con tu búsqueda. Intenta con otros términos."
        } else {
            "Tu historial de códigos QR escaneados aparecerá aquí. Comienza a escanear desde la pestaña principal."
        }

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun HistoryRecordItem(
    record: ScanRecord,
    onDelete: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val formattedDate = remember(record.timestamp) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(record.timestamp))
    }

    val icon = when (record.qrType.uppercase()) {
        "YAPE" -> Icons.Default.AccountBalanceWallet
        "LINK" -> Icons.Default.Link
        "WIFI" -> Icons.Default.Wifi
        "WHATSAPP" -> Icons.Default.Message
        "PHONE" -> Icons.Default.Phone
        "EMAIL" -> Icons.Default.Email
        else -> Icons.Default.TextFormat
    }

    val iconColor = when (record.qrType.uppercase()) {
        "YAPE" -> Color(0xFF00FFCC)
        "LINK" -> ElectricBlue
        "WIFI" -> Color(0xFFFFCC00)
        "WHATSAPP" -> Color(0xFF25D366)
        "PHONE" -> Color(0xFFFF5E5B)
        "EMAIL" -> DarkPurple
        else -> Color.Gray
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .testTag("history_item_${record.id}"),
        cornerRadius = 16.dp,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded Icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeBadge(type = record.qrType)
                    
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Light
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = record.rawContent.trim(),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Direct Context Actions
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy button
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(record.rawContent))
                        Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_item_button_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Borrar",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
