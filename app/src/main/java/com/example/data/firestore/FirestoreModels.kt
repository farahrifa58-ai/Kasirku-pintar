package com.example.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreStore(
    @DocumentId val storeId: String = "",
    val ownerUid: String = "",
    val ownerEmail: String = "",
    val cashierUids: List<String> = emptyList(),
    val storeName: String = "",
    val ownerName: String = "",
    val address: String = "",
    val whatsapp: String = "",
    val footerMessage: String = "",
    val taxEnabled: Boolean = true,
    val defaultTaxPercent: Double = 11.0,
    val cloudSyncCode: String = "",
    val paperSize: String = "58mm",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreProduct(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val name: String = "",
    val sku: String = "",
    val category: String = "Umum",
    val unit: String = "pcs",
    val buyPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stock: Double = 0.0,
    val imageUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreTransaction(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val timestamp: Long = 0L,
    val itemsSummary: String = "",
    val totalItems: Int = 0,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double = 0.0,
    val totalProfit: Double = 0.0,
    val paymentMethod: String = "Tunai",
    val cashPaid: Double = 0.0,
    val change: Double = 0.0,
    val customerName: String = "Pelanggan",
    val cashierName: String = "Kasir",
    val cashierUid: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
