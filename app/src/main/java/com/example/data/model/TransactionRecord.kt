package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

data class CartItem(
    val product: Product,
    val quantity: Double = 1.0,
    val note: String = ""
) {
    val subtotal: Double get() = product.sellPrice * quantity
    val totalProfit: Double get() = product.profitPerUnit * quantity

    fun formattedQuantity(): String {
        return if (quantity % 1.0 == 0.0) {
            "${quantity.toInt()} ${product.unit}"
        } else {
            String.format(Locale.US, "%.2f %s", quantity, product.unit)
        }
    }
}

@Entity(tableName = "transactions")
data class TransactionRecord(
    @PrimaryKey
    val id: String, // e.g. "TRX-20261110-001"
    val timestamp: Long = System.currentTimeMillis(),
    val itemsSummary: String, // Pipe/delimited list of items
    val totalItems: Int,
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0, // PPN
    val total: Double,
    val totalProfit: Double,
    val paymentMethod: String, // "Tunai", "QRIS", "Transfer Bank", "Debit"
    val cashPaid: Double,
    val change: Double,
    val customerName: String = "Pelanggan",
    val cashierName: String = "Kasir 1",
    val notes: String = ""
)
