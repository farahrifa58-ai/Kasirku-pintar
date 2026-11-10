package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupRestoreResult(
    val isSuccess: Boolean,
    val message: String,
    val products: List<Product> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val storeProfile: StoreProfile? = null
)

object BackupManager {

    fun generateBackupFile(
        context: Context,
        products: List<Product>,
        transactions: List<TransactionRecord>,
        storeProfile: StoreProfile
    ): File {
        val root = JSONObject().apply {
            put("app", "KasirKu Pintar")
            put("version", "1.0")
            put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("timestamp", System.currentTimeMillis())

            // Store Profile
            val storeObj = JSONObject().apply {
                put("storeName", storeProfile.storeName)
                put("ownerName", storeProfile.ownerName)
                put("address", storeProfile.address)
                put("whatsapp", storeProfile.whatsapp)
                put("footerMessage", storeProfile.footerMessage)
                put("logoUri", storeProfile.logoUri ?: "")
                put("taxEnabled", storeProfile.taxEnabled)
                put("defaultTaxPercent", storeProfile.defaultTaxPercent)
                put("cloudSyncCode", storeProfile.cloudSyncCode)
                put("paperSize", storeProfile.paperSize)
            }
            put("storeProfile", storeObj)

            // Products
            val prodArray = JSONArray()
            for (p in products) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("sku", p.sku)
                    put("category", p.category)
                    put("unit", p.unit)
                    put("buyPrice", p.buyPrice)
                    put("sellPrice", p.sellPrice)
                    put("stock", p.stock)
                    put("imageUri", p.imageUri ?: "")
                }
                prodArray.put(pObj)
            }
            put("products", prodArray)

            // Transactions
            val trxArray = JSONArray()
            for (t in transactions) {
                val tObj = JSONObject().apply {
                    put("id", t.id)
                    put("timestamp", t.timestamp)
                    put("itemsSummary", t.itemsSummary)
                    put("totalItems", t.totalItems)
                    put("subtotal", t.subtotal)
                    put("discount", t.discount)
                    put("tax", t.tax)
                    put("total", t.total)
                    put("totalProfit", t.totalProfit)
                    put("paymentMethod", t.paymentMethod)
                    put("cashPaid", t.cashPaid)
                    put("change", t.change)
                    put("customerName", t.customerName)
                    put("cashierName", t.cashierName)
                    put("notes", t.notes)
                }
                trxArray.put(tObj)
            }
            put("transactions", trxArray)
        }

        val backupDir = File(context.cacheDir, "backups")
        backupDir.mkdirs()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "KasirKuPintar_Backup_$timeStamp.json"
        val file = File(backupDir, fileName)

        FileOutputStream(file).use { out ->
            out.write(root.toString(2).toByteArray(Charsets.UTF_8))
        }

        return file
    }

    fun shareToGoogleDrive(context: Context, backupFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Cadangan KasirKu Pintar (${backupFile.name})")
            putExtra(Intent.EXTRA_TEXT, "File cadangan data produk, transaksi, dan profil toko dari KasirKu Pintar. Simpan ke Google Drive untuk keamanan.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Simpan Cadangan ke Google Drive / Bagikan")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun restoreFromBackupUri(context: Context, uri: Uri): BackupRestoreResult {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return BackupRestoreResult(false, "Tidak dapat membaca file cadangan")

            val root = JSONObject(jsonString)

            // Parse Products
            val restoredProducts = mutableListOf<Product>()
            val prodArray = root.optJSONArray("products")
            if (prodArray != null) {
                for (i in 0 until prodArray.length()) {
                    val obj = prodArray.getJSONObject(i)
                    restoredProducts.add(
                        Product(
                            id = obj.optLong("id", 0L),
                            name = obj.optString("name", "Produk"),
                            sku = obj.optString("sku", ""),
                            category = obj.optString("category", "Umum"),
                            unit = obj.optString("unit", "pcs"),
                            buyPrice = obj.optDouble("buyPrice", 0.0),
                            sellPrice = obj.optDouble("sellPrice", 0.0),
                            stock = obj.optDouble("stock", 0.0),
                            imageUri = obj.optString("imageUri").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }

            // Parse Transactions
            val restoredTransactions = mutableListOf<TransactionRecord>()
            val trxArray = root.optJSONArray("transactions")
            if (trxArray != null) {
                for (i in 0 until trxArray.length()) {
                    val obj = trxArray.getJSONObject(i)
                    restoredTransactions.add(
                        TransactionRecord(
                            id = obj.optString("id", "TRX-${System.currentTimeMillis()}-$i"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            itemsSummary = obj.optString("itemsSummary", ""),
                            totalItems = obj.optInt("totalItems", 1),
                            subtotal = obj.optDouble("subtotal", 0.0),
                            discount = obj.optDouble("discount", 0.0),
                            tax = obj.optDouble("tax", 0.0),
                            total = obj.optDouble("total", 0.0),
                            totalProfit = obj.optDouble("totalProfit", 0.0),
                            paymentMethod = obj.optString("paymentMethod", "Tunai"),
                            cashPaid = obj.optDouble("cashPaid", 0.0),
                            change = obj.optDouble("change", 0.0),
                            customerName = obj.optString("customerName", "Pelanggan"),
                            cashierName = obj.optString("cashierName", "Kasir"),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }

            // Parse Store Profile if present
            var restoredProfile: StoreProfile? = null
            val storeObj = root.optJSONObject("storeProfile")
            if (storeObj != null) {
                restoredProfile = StoreProfile(
                    storeName = storeObj.optString("storeName", "Toko KasirKu Pintar"),
                    ownerName = storeObj.optString("ownerName", "Rafline"),
                    address = storeObj.optString("address", ""),
                    whatsapp = storeObj.optString("whatsapp", ""),
                    footerMessage = storeObj.optString("footerMessage", "Terima Kasih Atas Kunjungan Anda!"),
                    logoUri = storeObj.optString("logoUri").takeIf { it.isNotBlank() },
                    taxEnabled = storeObj.optBoolean("taxEnabled", true),
                    defaultTaxPercent = storeObj.optDouble("defaultTaxPercent", 11.0),
                    cloudSyncCode = storeObj.optString("cloudSyncCode", "KASIR-7890"),
                    paperSize = storeObj.optString("paperSize", "58mm")
                )
            }

            BackupRestoreResult(
                isSuccess = true,
                message = "Berhasil memulihkan ${restoredProducts.size} produk dan ${restoredTransactions.size} riwayat transaksi.",
                products = restoredProducts,
                transactions = restoredTransactions,
                storeProfile = restoredProfile
            )
        } catch (e: Exception) {
            BackupRestoreResult(
                isSuccess = false,
                message = "Gagal memulihkan file: format tidak cocok (${e.localizedMessage})"
            )
        }
    }
}
