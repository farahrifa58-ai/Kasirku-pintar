package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.network.CloudSyncManager
import com.example.data.network.CloudSyncPacket
import com.example.data.network.MultiDeviceClient
import com.example.data.network.MultiDeviceServer
import com.example.data.network.RemoteServerStatus
import com.example.printer.BluetoothReceiptPrinter
import com.example.printer.PrintResult
import com.example.ui.components.ChartPoint
import com.example.util.BackupManager
import com.example.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class TimePeriodFilter {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    ALL
}

enum class DeviceMode {
    KASIR_HOST,
    MONITOR_CLIENT
}

enum class SyncConnectionMethod {
    CLOUD_INTERNET,
    LOCAL_WIFI
}

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "TRANSACTION"
)

class KasirViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val productDao = db.productDao()
    private val transactionDao = db.transactionDao()
    private val prefs: SharedPreferences = application.getSharedPreferences("kasir_pro_prefs", Context.MODE_PRIVATE)

    val bluetoothPrinter = BluetoothReceiptPrinter(application)
    val multiDeviceServer = MultiDeviceServer(application)
    val multiDeviceClient = MultiDeviceClient()

    // Preferences & Settings State
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        if (prefs.getString("app_language", "ID") == "EN") AppLanguage.ENGLISH else AppLanguage.INDONESIA
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _storeProfile = MutableStateFlow(loadStoreProfile())
    val storeProfile: StateFlow<StoreProfile> = _storeProfile.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getString("last_backup_time", "Belum pernah") ?: "Belum pernah")
    val lastBackupTime: StateFlow<String> = _lastBackupTime.asStateFlow()

    // Products & POS Cart State
    val allProducts: StateFlow<List<Product>> = productDao.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Transactions & Reports
    val allTransactions: StateFlow<List<TransactionRecord>> = transactionDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPeriod = MutableStateFlow(TimePeriodFilter.TODAY)
    val selectedPeriod: StateFlow<TimePeriodFilter> = _selectedPeriod.asStateFlow()

    // Notifications list
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    // Active Receipt Modal
    private val _activeReceipt = MutableStateFlow<TransactionRecord?>(null)
    val activeReceipt: StateFlow<TransactionRecord?> = _activeReceipt.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    // Multi-Device Mode & Live Sync
    private val _deviceMode = MutableStateFlow(DeviceMode.KASIR_HOST)
    val deviceMode: StateFlow<DeviceMode> = _deviceMode.asStateFlow()

    private val _syncMethod = MutableStateFlow(SyncConnectionMethod.CLOUD_INTERNET)
    val syncMethod: StateFlow<SyncConnectionMethod> = _syncMethod.asStateFlow()

    private val _isHostRunning = MutableStateFlow(false)
    val isHostRunning: StateFlow<Boolean> = _isHostRunning.asStateFlow()

    private val _hostIpAddress = MutableStateFlow("127.0.0.1")
    val hostIpAddress: StateFlow<String> = _hostIpAddress.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    // Remote Monitor State
    private val _remoteTargetIp = MutableStateFlow(prefs.getString("remote_target_ip", "192.168.1.10") ?: "192.168.1.10")
    val remoteTargetIp: StateFlow<String> = _remoteTargetIp.asStateFlow()

    private val _remoteTargetCloudCode = MutableStateFlow(prefs.getString("remote_target_cloud_code", "KASIR-7890") ?: "KASIR-7890")
    val remoteTargetCloudCode: StateFlow<String> = _remoteTargetCloudCode.asStateFlow()

    private val _remoteStatus = MutableStateFlow<RemoteServerStatus?>(null)
    val remoteStatus: StateFlow<RemoteServerStatus?> = _remoteStatus.asStateFlow()

    private val _cloudSyncPacket = MutableStateFlow<CloudSyncPacket?>(null)
    val cloudSyncPacket: StateFlow<CloudSyncPacket?> = _cloudSyncPacket.asStateFlow()

    private val _remoteTransactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val remoteTransactions: StateFlow<List<TransactionRecord>> = _remoteTransactions.asStateFlow()

    private val _isMonitorPolling = MutableStateFlow(false)
    val isMonitorPolling: StateFlow<Boolean> = _isMonitorPolling.asStateFlow()

    private var monitorJob: Job? = null
    private var cloudHostJob: Job? = null

    init {
        clearSampleProductsIfAny()
        setupMultiDeviceHost()
        startCloudHostSync()
    }

    private fun loadStoreProfile(): StoreProfile {
        return StoreProfile(
            storeName = prefs.getString("store_name", "Toko Berkah POS") ?: "Toko Berkah POS",
            logoUri = prefs.getString("store_logo_uri", null),
            ownerName = prefs.getString("store_owner", "Rafline") ?: "Rafline",
            address = prefs.getString("store_address", "Jl. Merdeka No. 88, Jakarta Selatan") ?: "Jl. Merdeka No. 88, Jakarta Selatan",
            whatsapp = prefs.getString("store_wa", "0812-3456-7890") ?: "0812-3456-7890",
            footerMessage = prefs.getString("store_footer", "Terima kasih atas kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar.") ?: "",
            paperSize = prefs.getString("store_paper_size", "58mm") ?: "58mm",
            bluetoothPrinterName = prefs.getString("store_bt_name", null),
            bluetoothPrinterAddress = prefs.getString("store_bt_addr", null),
            taxEnabled = prefs.getBoolean("store_tax_enabled", false),
            defaultTaxPercent = prefs.getFloat("store_tax_percent", 11.0f).toDouble(),
            cloudSyncCode = prefs.getString("store_cloud_code", "KASIR-7890") ?: "KASIR-7890",
            cloudSyncActive = prefs.getBoolean("store_cloud_active", true)
        )
    }

    fun updateStoreProfile(
        storeName: String,
        ownerName: String,
        address: String,
        whatsapp: String,
        footerMessage: String,
        logoUri: String? = _storeProfile.value.logoUri,
        taxEnabled: Boolean = _storeProfile.value.taxEnabled,
        defaultTaxPercent: Double = _storeProfile.value.defaultTaxPercent,
        cloudSyncCode: String = _storeProfile.value.cloudSyncCode
    ) {
        val updated = _storeProfile.value.copy(
            storeName = storeName,
            ownerName = ownerName,
            address = address,
            whatsapp = whatsapp,
            footerMessage = footerMessage,
            logoUri = logoUri,
            taxEnabled = taxEnabled,
            defaultTaxPercent = defaultTaxPercent,
            cloudSyncCode = cloudSyncCode.uppercase().trim()
        )
        _storeProfile.value = updated
        prefs.edit()
            .putString("store_name", storeName)
            .putString("store_owner", ownerName)
            .putString("store_address", address)
            .putString("store_wa", whatsapp)
            .putString("store_footer", footerMessage)
            .putString("store_logo_uri", logoUri)
            .putBoolean("store_tax_enabled", taxEnabled)
            .putFloat("store_tax_percent", defaultTaxPercent.toFloat())
            .putString("store_cloud_code", cloudSyncCode)
            .apply()

        // Push new profile to cloud
        pushCurrentDataToCloud()
    }

    fun updateStoreLogo(uri: Uri?) {
        val uriStr = uri?.toString()
        _storeProfile.value = _storeProfile.value.copy(logoUri = uriStr)
        prefs.edit().putString("store_logo_uri", uriStr).apply()
    }

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        prefs.edit().putBoolean("is_dark_theme", enabled).apply()
    }

    fun setAppLanguage(language: AppLanguage) {
        _appLanguage.value = language
        prefs.edit().putString("app_language", if (language == AppLanguage.ENGLISH) "EN" else "ID").apply()
    }

    fun setPaperSize(size: String) {
        _storeProfile.value = _storeProfile.value.copy(paperSize = size)
        prefs.edit().putString("store_paper_size", size).apply()
    }

    fun setBluetoothPrinter(name: String, address: String) {
        _storeProfile.value = _storeProfile.value.copy(
            bluetoothPrinterName = name,
            bluetoothPrinterAddress = address
        )
        prefs.edit()
            .putString("store_bt_name", name)
            .putString("store_bt_addr", address)
            .apply()
    }

    fun disconnectBluetoothPrinter() {
        _storeProfile.value = _storeProfile.value.copy(
            bluetoothPrinterName = null,
            bluetoothPrinterAddress = null
        )
        prefs.edit()
            .remove("store_bt_name")
            .remove("store_bt_addr")
            .apply()
    }

    // Product & POS Cart Methods
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun addToCart(product: Product, quantityToAdd: Double = 1.0) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + quantityToAdd)
        } else {
            current.add(CartItem(product = product, quantity = quantityToAdd))
        }
        _cart.value = current
    }

    fun updateCartQuantity(productId: Long, delta: Double) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = current[index]
            val newQty = item.quantity + delta
            if (newQty <= 0.05) {
                current.removeAt(index)
            } else {
                current[index] = item.copy(quantity = newQty)
            }
            _cart.value = current
        }
    }

    fun setCartExactQuantity(productId: Long, exactQty: Double) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (exactQty <= 0.0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = exactQty)
            }
            _cart.value = current
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun addOrUpdateProduct(
        id: Long = 0,
        name: String,
        sku: String,
        category: String,
        unit: String,
        buyPrice: Double,
        sellPrice: Double,
        stock: Double,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val product = Product(
                id = id,
                name = name,
                sku = sku,
                category = category,
                unit = unit,
                buyPrice = buyPrice,
                sellPrice = sellPrice,
                stock = stock,
                imageUri = imageUri
            )
            if (id == 0L) {
                productDao.insertProduct(product)
            } else {
                productDao.updateProduct(product)
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            productDao.deleteProduct(product)
        }
    }

    // Checkout & Print Struk with PPN Tax
    fun checkout(
        paymentMethod: String,
        cashPaid: Double,
        discount: Double = 0.0,
        tax: Double = 0.0,
        customerName: String = "Pelanggan",
        notes: String = ""
    ) {
        val items = _cart.value
        if (items.isEmpty()) return

        val subtotal = items.sumOf { it.subtotal }
        val finalTotal = (subtotal - discount + tax).coerceAtLeast(0.0)
        val change = (cashPaid - finalTotal).coerceAtLeast(0.0)
        val profit = items.sumOf { it.totalProfit } - discount

        val summary = items.joinToString("; ") {
            "${it.product.name} ${it.formattedQuantity()} (${FormatUtils.formatRupiah(it.subtotal)})"
        }
        val dateCode = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val trxId = "TRX-$dateCode"

        val transaction = TransactionRecord(
            id = trxId,
            timestamp = System.currentTimeMillis(),
            itemsSummary = summary,
            totalItems = items.size,
            subtotal = subtotal,
            discount = discount,
            tax = tax,
            total = finalTotal,
            totalProfit = profit,
            paymentMethod = paymentMethod,
            cashPaid = cashPaid,
            change = change,
            customerName = customerName.ifBlank { "Pelanggan" },
            cashierName = _storeProfile.value.ownerName.ifBlank { "Kasir 1" },
            notes = notes
        )

        viewModelScope.launch {
            transactionDao.insertTransaction(transaction)

            for (item in items) {
                productDao.reduceStock(item.product.id, item.quantity)
            }

            _cart.value = emptyList()
            _activeReceipt.value = transaction

            addNotification(
                title = "Transaksi Baru Berhasil!",
                message = "Pembayaran ${FormatUtils.formatRupiah(finalTotal)} via $paymentMethod ($summary)"
            )

            // Auto-trigger print if printer is paired
            if (!_storeProfile.value.bluetoothPrinterAddress.isNullOrBlank()) {
                printActiveReceipt()
            }

            // Sync to cloud relay immediately
            pushCurrentDataToCloud()
        }
    }

    fun openReceipt(transaction: TransactionRecord) {
        _activeReceipt.value = transaction
    }

    fun closeReceipt() {
        _activeReceipt.value = null
    }

    fun printActiveReceipt() {
        val trx = _activeReceipt.value ?: return
        viewModelScope.launch {
            _isPrinting.value = true
            val result = bluetoothPrinter.printReceipt(
                transaction = trx,
                store = _storeProfile.value
            )
            _isPrinting.value = false
            when (result) {
                is PrintResult.Success -> {
                    Toast.makeText(getApplication(), result.message, Toast.LENGTH_SHORT).show()
                }
                is PrintResult.Error -> {
                    Toast.makeText(getApplication(), result.error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Time-based filtering & Analytics
    fun setPeriodFilter(filter: TimePeriodFilter) {
        _selectedPeriod.value = filter
    }

    fun getFilteredTransactions(all: List<TransactionRecord>, filter: TimePeriodFilter): List<TransactionRecord> {
        val now = System.currentTimeMillis()
        val startTime = when (filter) {
            TimePeriodFilter.TODAY -> FormatUtils.getStartOfDay(now)
            TimePeriodFilter.THIS_WEEK -> FormatUtils.getStartOfWeek(now)
            TimePeriodFilter.THIS_MONTH -> FormatUtils.getStartOfMonth(now)
            TimePeriodFilter.THIS_YEAR -> FormatUtils.getStartOfYear(now)
            TimePeriodFilter.ALL -> 0L
        }
        return all.filter { it.timestamp >= startTime }
    }

    fun getHourlyTrendingPoints(todayTransactions: List<TransactionRecord>): List<ChartPoint> {
        val hourSlots = listOf("08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00", "22:00")
        val hourlyMap = mutableMapOf<String, Double>()
        for (slot in hourSlots) hourlyMap[slot] = 0.0

        val cal = Calendar.getInstance()
        for (trx in todayTransactions) {
            cal.timeInMillis = trx.timestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val slot = when {
                hour < 9 -> "08:00"
                hour < 11 -> "10:00"
                hour < 13 -> "12:00"
                hour < 15 -> "14:00"
                hour < 17 -> "16:00"
                hour < 19 -> "18:00"
                hour < 21 -> "20:00"
                else -> "22:00"
            }
            hourlyMap[slot] = (hourlyMap[slot] ?: 0.0) + trx.total
        }

        val hasData = hourlyMap.values.any { it > 0.0 }
        return if (!hasData) {
            hourSlots.map { ChartPoint(it, 0.0) }
        } else {
            hourSlots.map { ChartPoint(it, hourlyMap[it] ?: 0.0) }
        }
    }

    // Google Drive Backup and Data Recovery
    fun backupToGoogleDrive(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val products = productDao.getAllProductsSync()
                val transactions = transactionDao.getAllTransactionsSync()
                val profile = _storeProfile.value

                val file = BackupManager.generateBackupFile(context, products, transactions, profile)
                val nowStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
                _lastBackupTime.value = nowStr
                prefs.edit().putString("last_backup_time", nowStr).apply()

                withContext(Dispatchers.Main) {
                    BackupManager.shareToGoogleDrive(context, file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal membuat cadangan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun restoreFromBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = BackupManager.restoreFromBackupUri(context, uri)
                if (result.isSuccess) {
                    if (result.products.isNotEmpty()) {
                        productDao.insertProducts(result.products)
                    }
                    if (result.transactions.isNotEmpty()) {
                        transactionDao.insertTransactions(result.transactions)
                    }
                    val prof = result.storeProfile
                    if (prof != null) {
                        withContext(Dispatchers.Main) {
                            updateStoreProfile(
                                storeName = prof.storeName,
                                ownerName = prof.ownerName,
                                address = prof.address,
                                whatsapp = prof.whatsapp,
                                footerMessage = prof.footerMessage,
                                logoUri = prof.logoUri,
                                taxEnabled = prof.taxEnabled,
                                defaultTaxPercent = prof.defaultTaxPercent,
                                cloudSyncCode = prof.cloudSyncCode
                            )
                        }
                    }
                    val nowStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
                    _lastBackupTime.value = nowStr
                    prefs.edit().putString("last_backup_time", nowStr).apply()

                    withContext(Dispatchers.Main) {
                        onResult(true, result.message)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, result.message)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Terjadi kesalahan saat memulihkan: ${e.message}")
                }
            }
        }
    }

    // Multi-Device Modes: Local Wi-Fi and Cloud Internet Sync
    fun setDeviceMode(mode: DeviceMode) {
        _deviceMode.value = mode
        if (mode == DeviceMode.KASIR_HOST) {
            stopMonitorClient()
            setupMultiDeviceHost()
            startCloudHostSync()
        } else {
            multiDeviceServer.stopServer()
            _isHostRunning.value = false
            cloudHostJob?.cancel()
            startMonitorClient()
        }
    }

    fun setSyncMethod(method: SyncConnectionMethod) {
        _syncMethod.value = method
        if (_deviceMode.value == DeviceMode.MONITOR_CLIENT) {
            startMonitorClient()
        }
    }

    private fun setupMultiDeviceHost() {
        val ip = multiDeviceServer.getLocalIpAddress()
        _hostIpAddress.value = ip

        multiDeviceServer.onStatusRequested = {
            val now = System.currentTimeMillis()
            val startOfDay = FormatUtils.getStartOfDay(now)
            val todayTrx = allTransactions.value.filter { it.timestamp >= startOfDay }
            Pair(_storeProfile.value, todayTrx)
        }

        val success = multiDeviceServer.startServer(8080)
        _isHostRunning.value = success

        viewModelScope.launch {
            while (_isHostRunning.value) {
                _connectedClientsCount.value = multiDeviceServer.connectedClients.size
                delay(3000)
            }
        }
    }

    private fun startCloudHostSync() {
        cloudHostJob?.cancel()
        cloudHostJob = viewModelScope.launch {
            while (true) {
                pushCurrentDataToCloud()
                delay(4000) // Periodic cloud heartbeat every 4 seconds
            }
        }
    }

    private fun pushCurrentDataToCloud() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val startOfDay = FormatUtils.getStartOfDay(now)
            val todayTrx = allTransactions.value.filter { it.timestamp >= startOfDay }
            CloudSyncManager.pushToCloud(_storeProfile.value, todayTrx)
        }
    }

    fun restartHostServer() {
        multiDeviceServer.stopServer()
        setupMultiDeviceHost()
        pushCurrentDataToCloud()
    }

    fun setRemoteTargetIp(ip: String) {
        _remoteTargetIp.value = ip
        prefs.edit().putString("remote_target_ip", ip).apply()
        if (_deviceMode.value == DeviceMode.MONITOR_CLIENT && _syncMethod.value == SyncConnectionMethod.LOCAL_WIFI) {
            startMonitorClient()
        }
    }

    fun setRemoteTargetCloudCode(code: String) {
        val clean = code.uppercase().trim()
        _remoteTargetCloudCode.value = clean
        prefs.edit().putString("remote_target_cloud_code", clean).apply()
        if (_deviceMode.value == DeviceMode.MONITOR_CLIENT && _syncMethod.value == SyncConnectionMethod.CLOUD_INTERNET) {
            startMonitorClient()
        }
    }

    fun startMonitorClient() {
        monitorJob?.cancel()
        _isMonitorPolling.value = true
        monitorJob = viewModelScope.launch {
            var previousTrxCount = 0
            while (_isMonitorPolling.value) {
                if (_syncMethod.value == SyncConnectionMethod.CLOUD_INTERNET) {
                    // Pull from Remote Cloud Relay via Mobile Data / Sinyal Internet
                    val packet = CloudSyncManager.pullFromCloud(_remoteTargetCloudCode.value)
                    _cloudSyncPacket.value = packet
                    if (packet != null) {
                        _remoteStatus.value = RemoteServerStatus(
                            isOnline = true,
                            storeName = packet.storeName,
                            totalSales = packet.totalSales,
                            totalProfit = packet.totalProfit,
                            transactionCount = packet.transactionCount,
                            connectedDevicesCount = 2,
                            lastUpdate = packet.timestamp
                        )
                        _remoteTransactions.value = packet.latestTransactions

                        if (previousTrxCount > 0 && packet.latestTransactions.size > previousTrxCount) {
                            val latest = packet.latestTransactions.firstOrNull()
                            if (latest != null) {
                                addNotification(
                                    title = "🔔 Penjualan Baru di Kasir (Cloud Sync)!",
                                    message = "${FormatUtils.formatRupiah(latest.total)} - ${latest.itemsSummary}"
                                )
                            }
                        }
                        previousTrxCount = packet.latestTransactions.size
                    }
                } else {
                    // Pull from Local Wi-Fi
                    val status = multiDeviceClient.fetchStatus(_remoteTargetIp.value)
                    _remoteStatus.value = status
                    if (status != null && status.isOnline) {
                        val trxs = multiDeviceClient.fetchTransactions(_remoteTargetIp.value)
                        _remoteTransactions.value = trxs
                        if (previousTrxCount > 0 && trxs.size > previousTrxCount) {
                            val latest = trxs.firstOrNull()
                            if (latest != null) {
                                addNotification(
                                    title = "🔔 Penjualan Baru di Kasir (Wi-Fi)!",
                                    message = "${FormatUtils.formatRupiah(latest.total)} - ${latest.itemsSummary}"
                                )
                            }
                        }
                        previousTrxCount = trxs.size
                    }
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    fun stopMonitorClient() {
        _isMonitorPolling.value = false
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun addNotification(title: String, message: String) {
        val list = _notifications.value.toMutableList()
        list.add(0, AppNotification(title = title, message = message))
        _notifications.value = list.take(25)
    }

    private fun clearSampleProductsIfAny() {
        viewModelScope.launch {
            val hasCleaned = prefs.getBoolean("has_cleaned_initial_samples_v1", false)
            if (!hasCleaned) {
                productDao.clearAllProducts()
                transactionDao.clearSampleTransactions()
                prefs.edit().putBoolean("has_cleaned_initial_samples_v1", true).apply()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        multiDeviceServer.stopServer()
        stopMonitorClient()
        cloudHostJob?.cancel()
    }
}
