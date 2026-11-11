package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DeviceMode
import com.example.ui.FirestoreSyncState
import com.example.ui.KasirViewModel
import com.example.ui.components.KasirTopHeader
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.NavyPrimary
import com.example.util.FormatUtils

@Composable
fun MultiDeviceScreen(
    viewModel: KasirViewModel,
    onOpenSettings: () -> Unit,
    onRequireLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val storeProfile by viewModel.storeProfile.collectAsState()
    val deviceMode by viewModel.deviceMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val syncState by viewModel.firestoreSyncState.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    // Calculate today's live sales
    val now = System.currentTimeMillis()
    val startOfDay = remember(now) { FormatUtils.getStartOfDay(now) }
    val todayTransactions = remember(allTransactions) {
        allTransactions.filter { it.timestamp >= startOfDay }
    }
    val todaySales = remember(todayTransactions) { todayTransactions.sumOf { it.total } }
    val todayTrxCount = remember(todayTransactions) { todayTransactions.size }

    val isConnected = syncState is FirestoreSyncState.Connected

    Scaffold(
        topBar = {
            KasirTopHeader(
                storeProfile = storeProfile,
                title = "Koneksi Multi-Smartphone",
                subtitle = "Sinkronisasi Real-Time Firestore",
                isHostServerRunning = isConnected,
                onSettingsClick = onOpenSettings
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==================== ACCURATE CONNECTION STATUS ====================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (syncState) {
                            is FirestoreSyncState.Connected -> MaterialTheme.colorScheme.surface
                            is FirestoreSyncState.PermissionDenied -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                            is FirestoreSyncState.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            is FirestoreSyncState.Connecting -> MaterialTheme.colorScheme.surface
                            is FirestoreSyncState.Offline -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = when (syncState) {
                            is FirestoreSyncState.Connected -> AccentEmerald.copy(alpha = 0.5f)
                            is FirestoreSyncState.PermissionDenied -> MaterialTheme.colorScheme.error
                            is FirestoreSyncState.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            is FirestoreSyncState.Connecting -> Color(0xFFFFB300)
                            is FirestoreSyncState.Offline -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (syncState) {
                                                is FirestoreSyncState.Connected -> AccentEmerald
                                                is FirestoreSyncState.PermissionDenied -> Color.Red
                                                is FirestoreSyncState.Error -> Color.Red
                                                is FirestoreSyncState.Connecting -> Color(0xFFFFB300)
                                                is FirestoreSyncState.Offline -> Color.Gray
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = when (syncState) {
                                            is FirestoreSyncState.Connected -> "Cloud Firestore: Terhubung"
                                            is FirestoreSyncState.PermissionDenied -> "Akses Ditolak (PERMISSION_DENIED)"
                                            is FirestoreSyncState.Error -> "Koneksi Cloud Terkendala"
                                            is FirestoreSyncState.Connecting -> "Memverifikasi Izin Database..."
                                            is FirestoreSyncState.Offline -> "Mode Offline (Belum Login)"
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (syncState) {
                                            is FirestoreSyncState.Connected -> AccentEmerald
                                            is FirestoreSyncState.PermissionDenied -> MaterialTheme.colorScheme.error
                                            is FirestoreSyncState.Error -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Text(
                                        text = when (syncState) {
                                            is FirestoreSyncState.Connected -> (syncState as FirestoreSyncState.Connected).email
                                            is FirestoreSyncState.PermissionDenied -> "Aturan keamanan Firestore menolak akses untuk akun ini."
                                            is FirestoreSyncState.Error -> (syncState as FirestoreSyncState.Error).message
                                            is FirestoreSyncState.Connecting -> "Memeriksa status otentikasi & izin Firestore..."
                                            is FirestoreSyncState.Offline -> "Masuk dengan Akun Google untuk sinkron multi-perangkat"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (currentUser == null) {
                                Button(
                                    onClick = onRequireLogin,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Masuk", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ==================== PILIHAN PERAN PERANGKAT ====================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Mode Perangkat Ini",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pilih peran HP ini. Semua perangkat yang login dengan akun Google yang sama otomatis tersinkron.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Mode Kasir Tile
                            val isKasirActive = deviceMode == DeviceMode.MODE_KASIR
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setDeviceMode(DeviceMode.MODE_KASIR) }
                                    .testTag("mode_kasir_tile"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isKasirActive) NavyPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isKasirActive) 2.dp else 1.dp,
                                    color = if (isKasirActive) NavyPrimary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isKasirActive) NavyPrimary else Color.Gray.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PointOfSale,
                                                contentDescription = null,
                                                tint = if (isKasirActive) Color.White else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        if (isKasirActive) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = NavyPrimary
                                            ) {
                                                Text(
                                                    text = "✓ Aktif",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Mode Kasir",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isKasirActive) NavyPrimary else MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "Input transaksi penjualan & cetak struk",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 13.sp
                                    )
                                }
                            }

                            // Mode Pemantau Tile
                            val isPemantauActive = deviceMode == DeviceMode.MODE_PEMANTAU
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setDeviceMode(DeviceMode.MODE_PEMANTAU) }
                                    .testTag("mode_pemantau_tile"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPemantauActive) AccentEmerald.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    width = if (isPemantauActive) 2.dp else 1.dp,
                                    color = if (isPemantauActive) AccentEmerald else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isPemantauActive) AccentEmerald else Color.Gray.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = if (isPemantauActive) Color.White else Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        if (isPemantauActive) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = AccentEmerald
                                            ) {
                                                Text(
                                                    text = "✓ Aktif",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Mode Pemantau",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPemantauActive) AccentEmerald else MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "Pantau omzet & transaksi dari jauh",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================== MODE SPECIFIC CONTENT ====================
            if (deviceMode == DeviceMode.MODE_PEMANTAU) {
                // ==================== MONITORING VIEW ====================
                item {
                    Text(
                        text = "Ringkasan Penjualan Hari Ini",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Omzet Kasir Hari Ini", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = FormatUtils.formatRupiah(todaySales),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentEmerald
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Total Transaksi", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = "$todayTrxCount Transaksi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Aktivitas Penjualan Masuk (Real-Time)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (todayTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Belum ada transaksi kasir hari ini.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Setiap penjualan di HP kasir akan langsung muncul di sini.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(todayTransactions) { trx ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openReceipt(trx) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = trx.itemsSummary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${FormatUtils.formatTimeOnly(trx.timestamp)} • ${trx.paymentMethod} • ${trx.cashierName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = FormatUtils.formatRupiah(trx.total),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }
                        }
                    }
                }
            } else {
                // ==================== CASHIER VIEW ====================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.PointOfSale, contentDescription = null, tint = NavyPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Terminal Kasir Aktif",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "HP ini digunakan untuk memproses transaksi. Setiap penjualan otomatis tersimpan ke Cloud Firestore dan memotong stok produk secara atomik.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = NavyPrimary.copy(alpha = 0.08f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Katalog Tersedia", style = MaterialTheme.typography.labelSmall, color = NavyPrimary)
                                        Text("${allProducts.size} Produk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentEmerald.copy(alpha = 0.08f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Penjualan Hari Ini", style = MaterialTheme.typography.labelSmall, color = AccentEmerald)
                                        Text("$todayTrxCount Transaksi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
