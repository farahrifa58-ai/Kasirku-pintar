package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionRecord
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.NavyPrimary
import com.example.util.FormatUtils
import com.example.util.ReceiptExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDialog(
    transaction: TransactionRecord,
    storeProfile: StoreProfile,
    onDismiss: () -> Unit,
    onPrintClick: () -> Unit,
    isPrinting: Boolean = false
) {
    val context = LocalContext.current
    var showShareBottomSheet by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val currentDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = currentDensity.density,
                fontScale = 1.0f
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(20.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Struk Transaksi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Thermal Receipt View
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFAFAFA)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Logo if available
                        if (storeProfile.logoUri != null) {
                            AsyncImage(
                                model = storeProfile.logoUri,
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Store Header Info
                        Text(
                            text = storeProfile.storeName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = storeProfile.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "WhatsApp: ${storeProfile.whatsapp} • Pemilik: ${storeProfile.ownerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        ReceiptDottedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Transaction Metadata
                        ReceiptRow(label = "No. Transaksi", value = transaction.id)
                        ReceiptRow(label = "Waktu", value = FormatUtils.formatDateTime(transaction.timestamp))
                        ReceiptRow(label = "Kasir", value = transaction.cashierName)
                        ReceiptRow(label = "Pelanggan", value = transaction.customerName)

                        Spacer(modifier = Modifier.height(8.dp))
                        ReceiptDottedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Items Breakdown
                        val items = transaction.itemsSummary.split("; ").filter { it.isNotBlank() }
                        for (item in items) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF334155),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        ReceiptDottedDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Financial Totals
                        ReceiptRow(label = "Subtotal", value = FormatUtils.formatRupiah(transaction.subtotal))
                        if (transaction.discount > 0) {
                            ReceiptRow(
                                label = "Diskon",
                                value = "-${FormatUtils.formatRupiah(transaction.discount)}",
                                valueColor = Color(0xFFE63946)
                            )
                        }
                        if (transaction.tax > 0) {
                            ReceiptRow(label = "PPN (Pajak)", value = "+${FormatUtils.formatRupiah(transaction.tax)}")
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text(
                                text = FormatUtils.formatRupiah(transaction.total),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }

                        ReceiptRow(
                            label = "Bayar (${transaction.paymentMethod})",
                            value = FormatUtils.formatRupiah(transaction.cashPaid)
                        )
                        ReceiptRow(
                            label = "Kembalian",
                            value = FormatUtils.formatRupiah(transaction.change),
                            valueColor = AccentEmerald
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        ReceiptDottedDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Footer
                        Text(
                            text = storeProfile.footerMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "KasirKu Pintar v1.0 • Rafline Dev",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons: Print Struk and Share Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Button Print Struk (di samping button share)
                    Button(
                        onClick = onPrintClick,
                        enabled = !isPrinting,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("print_receipt_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mencetak...")
                        } else {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Print Struk", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Button Share (akan memunculkan pilihan share & download saat diklik)
                    OutlinedButton(
                        onClick = { showShareBottomSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("share_receipt_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(NavyPrimary)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = NavyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share", color = NavyPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal BottomSheet for Share & Download options
    if (showShareBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Bagikan & Unduh Struk",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pilih metode pengiriman struk untuk pelanggan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Share Image Struk (PNG)
                ShareOptionItem(
                    icon = Icons.Default.Image,
                    title = "Bagikan Gambar Struk (PNG)",
                    subtitle = "Kirim struk dalam bentuk format gambar berkualitas tinggi",
                    onClick = {
                        showShareBottomSheet = false
                        ReceiptExporter.shareReceiptImage(context, transaction, storeProfile)
                    }
                )

                // 2. Share PDF / Dokumen Teks
                ShareOptionItem(
                    icon = Icons.Default.Description,
                    title = "Bagikan Struk PDF / Teks",
                    subtitle = "Format teks lengkap struk ke aplikasi lain",
                    onClick = {
                        showShareBottomSheet = false
                        ReceiptExporter.shareReceiptText(context, transaction, storeProfile)
                    }
                )

                // 3. Kirim langsung via WhatsApp
                ShareOptionItem(
                    icon = Icons.Default.Send,
                    title = "Kirim via WhatsApp",
                    subtitle = "Kirim struk langsung ke nomor WhatsApp pelanggan",
                    onClick = {
                        showShareBottomSheet = false
                        ReceiptExporter.sendToWhatsApp(context, transaction, storeProfile)
                    }
                )

                // 4. Unduh Gambar Struk ke Penyimpanan
                ShareOptionItem(
                    icon = Icons.Default.Download,
                    title = "Unduh Gambar Struk",
                    subtitle = "Simpan gambar struk ke memori HP",
                    onClick = {
                        showShareBottomSheet = false
                        ReceiptExporter.downloadReceiptImage(context, transaction, storeProfile)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
}

@Composable
private fun ShareOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = NavyPrimary)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF334155)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
private fun ReceiptDottedDivider() {
    Text(
        text = "- - - - - - - - - - - - - - - - - - - - - - - - -",
        color = Color(0xFFCBD5E1),
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
