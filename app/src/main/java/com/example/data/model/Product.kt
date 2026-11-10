package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String = "",
    val category: String,
    val unit: String = "pcs", // "kg", "gram", "liter", "ml", "cup", "pack", "meter", "pcs", dll
    val buyPrice: Double,
    val sellPrice: Double,
    val stock: Double = 100.0,
    val imageUri: String? = null
) {
    val profitPerUnit: Double get() = (sellPrice - buyPrice).coerceAtLeast(0.0)

    val isWeightBased: Boolean
        get() = unit.equals("kg", ignoreCase = true) ||
                unit.equals("gram", ignoreCase = true) ||
                unit.equals("liter", ignoreCase = true) ||
                unit.equals("meter", ignoreCase = true)
}
