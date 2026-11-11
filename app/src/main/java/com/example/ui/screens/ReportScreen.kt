package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppNotification
import com.example.data.model.TransactionRecord
import com.example.ui.FirestoreSyncState
import com.example.ui.KasirViewModel
import com.example.ui.TimePeriodFilter
import com.example.ui.components.KasirTopHeader
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.NavyPrimary
import com.example.util.FormatUtils

@Composable
fun ReportScreen(
    viewModel: KasirViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val storeProfile by viewModel.storeProfile.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val syncState by viewModel.firestoreSyncState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedPaymentFilter by remember { mutableStateOf("Semua") }

    val filteredByPeriod = remember(allTransactions, selectedPeriod) {
        viewModel.getFilteredTransactions(allTransactions, selectedPeriod)
    }

    val finalTransactions = remember(filteredByPeriod, searchQuery, selectedPaymentFilter) {
        filteredByPeriod.filter { trx ->
            val matchPayment = selectedPaymentFilter == "Semua" || trx.paymentMethod.equals(selectedPaymentFilter, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() ||
                    trx.id.contains(searchQuery, ignoreCase = true) ||
                    trx.customerName.contains(searchQuery, ignoreCase = true) ||
                    trx.itemsSummary.contains(searchQuery, ignoreCase = true)
            matchPayment && matchSearch
        }
    }

    val totalRevenue = remember(finalTransactions) { finalTransactions.sumOf { it.total } }
    val totalProfit = remember(finalTransactions) { finalTransactions.sumOf { it.totalProfit } }
    val totalCount = remember(finalTransactions) { finalTransactions.size }
    val avgTransaction = remember(totalRevenue, totalCount) {
        if (totalCount > 0) totalRevenue / totalCount else 0.0
    }

    val periodTabs = listOf(
        Pair(TimePeriodFilter.TODAY, "Hari Ini"),
        Pair(TimePeriodFilter.THIS_WEEK, "Minggu Ini"),
        Pair(TimePeriodFilter.THIS_MONTH, "Bulan Ini"),
        Pair(TimePeriodFilter.THIS_YEAR, "Tahun Ini"),
        Pair(TimePeriodFilter.ALL, "Semua")
    )

    Scaffold(
        topBar = {
            KasirTopHeader(
                storeProfile = storeProfile,
                title = "Laporan Penjualan",
                subtitle = "Pantau omset & riwayat transaksi real-time",
                connectedDevicesCount = 1,
                isHostServerRunning = syncState is FirestoreSyncState.Connected,
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
            // Period Filter Tabs
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(periodTabs) { (filter, label) ->
                        FilterChip(
                            selected = selectedPeriod == filter,
                            onClick = { viewModel.setPeriodFilter(filter) },
                            label = { Text(label, fontWeight = if (selectedPeriod == filter) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Summary Stats Cards (2x2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportStatCard(
                            title = "Total Omset",
                            value = FormatUtils.formatRupiah(totalRevenue),
                            icon = Icons.Default.MonetizationOn,
                            color = AccentEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        ReportStatCard(
                            title = "Total Transaksi",
                            value = "$totalCount Trx",
                            icon = Icons.Default.Receipt,
                            color = NavyPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportStatCard(
                            title = "Laba Bersih",
                            value = FormatUtils.formatRupiah(totalProfit),
                            icon = Icons.Default.TrendingUp,
                            color = AccentAmber,
                            modifier = Modifier.weight(1f)
                        )
                        ReportStatCard(
                            title = "Rata-rata Transaksi",
                            value = FormatUtils.formatRupiah(avgTransaction),
                            icon = Icons.Default.ShowChart,
                            color = Color(0xFF00B4D8),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Notifications & Milestone Alerts Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notifikasi Riwayat Transaksi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (notifications.isEmpty()) {
                            Text(
                                text = "Semua transaksi harian, mingguan, bulanan & tahunan tersinkronisasi otomatis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            notifications.take(3).forEach { notif ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(AccentEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = notif.message,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = FormatUtils.formatTimeOnly(notif.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search & Payment Filter Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari ID / Pelanggan...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_report_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // Transaction History List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Riwayat Transaksi (${finalTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (finalTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tidak ada transaksi pada periode ini",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(finalTransactions, key = { it.id }) { trx ->
                    ReportTransactionItemCard(
                        transaction = trx,
                        onClick = { viewModel.openReceipt(trx) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReportStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ReportTransactionItemCard(
    transaction: TransactionRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("report_item_${transaction.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.id,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = FormatUtils.formatRupiah(transaction.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = transaction.itemsSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NavyPrimary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = transaction.paymentMethod,
                            style = MaterialTheme.typography.labelSmall,
                            color = NavyPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = FormatUtils.formatDateTime(transaction.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Struk",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
