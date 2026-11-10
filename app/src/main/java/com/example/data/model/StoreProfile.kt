package com.example.data.model

data class StoreProfile(
    val storeName: String = "Toko Berkah POS",
    val logoUri: String? = null,
    val ownerName: String = "Rafline",
    val address: String = "Jl. Merdeka No. 88, Jakarta Selatan",
    val whatsapp: String = "0812-3456-7890",
    val footerMessage: String = "Terima kasih atas kunjungan Anda!\nBarang yang sudah dibeli tidak dapat ditukar.",
    val paperSize: String = "58mm", // "58mm" or "80mm"
    val bluetoothPrinterName: String? = null,
    val bluetoothPrinterAddress: String? = null,
    val taxEnabled: Boolean = false,
    val defaultTaxPercent: Double = 11.0,
    val cloudSyncCode: String = "KASIR-7890",
    val cloudSyncActive: Boolean = true
)
