package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DeviceMode
import com.example.ui.KasirViewModel
import com.example.ui.SyncConnectionMethod
import com.example.ui.components.KasirTopHeader
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavyVariant
import com.example.util.FormatUtils

@Composable
fun MultiDeviceScreen(
    viewModel: KasirViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val storeProfile by viewModel.storeProfile.collectAsState()
    val deviceMode by viewModel.deviceMode.collectAsState()
    val syncMethod by viewModel.syncMethod.collectAsState()

    val isHostRunning by viewModel.isHostRunning.collectAsState()
    val hostIpAddress by viewModel.hostIpAddress.collectAsState()
    val connectedClientsCount by viewModel.connectedClientsCount.collectAsState()

    val remoteTargetIp by viewModel.remoteTargetIp.collectAsState()
    val remoteTargetCloudCode by viewModel.remoteTargetCloudCode.collectAsState()
    val remoteStatus by viewModel.remoteStatus.collectAsState()
    val remoteTransactions by viewModel.remoteTransactions.collectAsState()

    var inputCloudCode by remember(remoteTargetCloudCode) { mutableStateOf(remoteTargetCloudCode) }
    var inputTargetIp by remember(remoteTargetIp) { mutableStateOf(remoteTargetIp) }

    Scaffold(
        topBar = {
            KasirTopHeader(
                storeProfile = storeProfile,
                title = "Koneksi Multi-Smartphone",
                subtitle = "Pantau omset kasir jarak jauh via kuota data",
                connectedDevicesCount = connectedClientsCount + 1,
                isHostServerRunning = isHostRunning,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Selector: HP Kasir vs HP Pemilik
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    TabRow(
                        selectedTabIndex = if (deviceMode == DeviceMode.KASIR_HOST) 0 else 1,
                        containerColor = Color.Transparent,
                        contentColor = NavyPrimary
                    ) {
                        Tab(
                            selected = deviceMode == DeviceMode.KASIR_HOST,
                            onClick = { viewModel.setDeviceMode(DeviceMode.KASIR_HOST) },
                            text = { Text("HP Kasir (Host)", fontWeight = FontWeight.Bold) },
                            icon = { Icon(imageVector = Icons.Default.PointOfSale, contentDescription = null) }
                        )
                        Tab(
                            selected = deviceMode == DeviceMode.MONITOR_CLIENT,
                            onClick = { viewModel.setDeviceMode(DeviceMode.MONITOR_CLIENT) },
                            text = { Text("HP Pemilik (Pantau)", fontWeight = FontWeight.Bold) },
                            icon = { Icon(imageVector = Icons.Default.Visibility, contentDescription = null) }
                        )
                    }
                }
            }

            // Connection Method Selector: Jarak Jauh (Cloud) vs Lokal (Wi-Fi)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = syncMethod == SyncConnectionMethod.CLOUD_INTERNET,
                            onClick = { viewModel.setSyncMethod(SyncConnectionMethod.CLOUD_INTERNET) },
                            label = {
                                Text("🌐 Jarak Jauh (Kuota / Sinyal)", fontWeight = FontWeight.Bold)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = syncMethod == SyncConnectionMethod.LOCAL_WIFI,
                            onClick = { viewModel.setSyncMethod(SyncConnectionMethod.LOCAL_WIFI) },
                            label = {
                                Text("📶 Wi-Fi / Hotspot", fontWeight = FontWeight.Bold)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (deviceMode == DeviceMode.KASIR_HOST) {
                // ==================== MODE HP KASIR (HOST) ====================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = NavyVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Status Indicator & Compact Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(AccentEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (syncMethod == SyncConnectionMethod.CLOUD_INTERNET)
                                            "Cloud Sync Jarak Jauh"
                                        else "Server Wi-Fi Lokal",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "2-3 HP Siap",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (syncMethod == SyncConnectionMethod.CLOUD_INTERNET) {
                                Text(
                                    text = "Kode Toko Cloud (Untuk HP Pemilik):",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = storeProfile.cloudSyncCode,
                                            color = Color(0xFFFFD54F),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )

                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(storeProfile.cloudSyncCode))
                                                Toast.makeText(context, "Kode disalin!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Salin",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Bisa dipantau via internet/kuota data tanpa batas jarak.",
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                Text(
                                    text = "Alamat IP Wi-Fi Kasir:",
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.3f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = hostIpAddress,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Port: 8080",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    viewModel.restartHostServer()
                                    Toast.makeText(context, "Koneksi sinkronisasi diperbarui!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.6f))
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Segarkan Sinkronisasi", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Panduan Pemilik
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = NavyPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pantau dari Jarak Jauh (Kuota Data)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            InstructionStep(number = "1", text = "Pasang aplikasi KasirKu Pintar di smartphone pemilik / pengawas (bisa hingga 2-3 HP).")
                            InstructionStep(number = "2", text = "Pilih tab 'HP Pemilik (Pantau)' lalu pilih mode 'Jarak Jauh (Kuota Data)'.")
                            InstructionStep(number = "3", text = "Masukkan Kode Toko: ${storeProfile.cloudSyncCode} lalu klik 'Mulai Pantau Jarak Jauh'.")
                            InstructionStep(number = "4", text = "Selesai! Omset, grafik, dan riwayat penjualan masuk langsung secara real-time dari mana saja.")
                        }
                    }
                }
            } else {
                // ==================== MODE HP PEMILIK (PANTAU) ====================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (syncMethod == SyncConnectionMethod.CLOUD_INTERNET)
                                    "Pantau Jarak Jauh (Mengandalkan Kuota Data / Sinyal)"
                                else "Pantau Jaringan Lokal (Wi-Fi)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (syncMethod == SyncConnectionMethod.CLOUD_INTERNET)
                                    "Masukkan Kode Toko Cloud yang tertera di HP Kasir"
                                else "Masukkan Alamat IP Kasir yang tertera di layar HP Kasir",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (syncMethod == SyncConnectionMethod.CLOUD_INTERNET) {
                                OutlinedTextField(
                                    value = inputCloudCode,
                                    onValueChange = { inputCloudCode = it.uppercase() },
                                    label = { Text("Kode Toko Cloud") },
                                    placeholder = { Text("Contoh: KASIR-7890") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.VpnKey, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("target_cloud_code_input")
                                )
                            } else {
                                OutlinedTextField(
                                    value = inputTargetIp,
                                    onValueChange = { inputTargetIp = it },
                                    label = { Text("Alamat IP Kasir") },
                                    placeholder = { Text("Contoh: 192.168.1.10") },
                                    leadingIcon = { Icon(imageVector = Icons.Default.Lan, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("target_ip_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (syncMethod == SyncConnectionMethod.CLOUD_INTERNET) {
                                        viewModel.setRemoteTargetCloudCode(inputCloudCode)
                                        viewModel.startMonitorClient()
                                        Toast.makeText(context, "Menghubungkan ke toko $inputCloudCode via Cloud...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.setRemoteTargetIp(inputTargetIp)
                                        viewModel.startMonitorClient()
                                        Toast.makeText(context, "Menghubungkan ke $inputTargetIp via Wi-Fi...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("connect_monitor_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mulai Pantau Jarak Jauh", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Status & Data Real-Time
                val status = remoteStatus
                if (status != null && status.isOnline) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = AccentEmerald.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(AccentEmerald)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "🟢 Terhubung Live Jarak Jauh: ${status.storeName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentEmerald
                                    )
                                    Text(
                                        text = "Sinkronisasi via kuota data internet otomatis aktif",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "Omset Kasir (Live)", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        text = FormatUtils.formatRupiah(status.totalSales),
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
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "Transaksi Kasir", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        text = "${status.transactionCount} Trx",
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
                            text = "Aktivitas Penjualan Kasir Masuk (Live)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(remoteTransactions) { trx ->
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
                                    Text(text = trx.itemsSummary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(text = FormatUtils.formatTimeOnly(trx.timestamp) + " • " + trx.paymentMethod, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            }
        }
    }
}

@Composable
fun InstructionStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(NavyPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
