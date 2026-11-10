package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionRecord
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

object ReceiptExporter {

    fun generateReceiptText(transaction: TransactionRecord, store: StoreProfile): String {
        val sb = StringBuilder()
        sb.append("===============================\n")
        sb.append("      ${store.storeName.uppercase()}      \n")
        sb.append("${store.address}\n")
        sb.append("WA: ${store.whatsapp}\n")
        sb.append("===============================\n")
        sb.append("No. TRX : ${transaction.id}\n")
        sb.append("Waktu   : ${FormatUtils.formatDateTime(transaction.timestamp)}\n")
        sb.append("Kasir   : ${transaction.cashierName}\n")
        sb.append("Pelanggan: ${transaction.customerName}\n")
        sb.append("-------------------------------\n")

        val items = transaction.itemsSummary.split("; ")
        for (item in items) {
            if (item.isNotBlank()) {
                sb.append(item.trim()).append("\n")
            }
        }

        sb.append("-------------------------------\n")
        sb.append("Subtotal : ${FormatUtils.formatRupiah(transaction.subtotal)}\n")
        if (transaction.discount > 0) {
            sb.append("Diskon   : -${FormatUtils.formatRupiah(transaction.discount)}\n")
        }
        if (transaction.tax > 0) {
            sb.append("Pajak    : ${FormatUtils.formatRupiah(transaction.tax)}\n")
        }
        sb.append("TOTAL    : ${FormatUtils.formatRupiah(transaction.total)}\n")
        sb.append("Bayar (${transaction.paymentMethod}): ${FormatUtils.formatRupiah(transaction.cashPaid)}\n")
        sb.append("Kembali  : ${FormatUtils.formatRupiah(transaction.change)}\n")
        sb.append("===============================\n")
        sb.append(store.footerMessage).append("\n")
        sb.append("KasirKu Pintar v1.0 • Rafline Dev\n")
        sb.append("===============================\n")
        return sb.toString()
    }

    fun generateReceiptBitmap(transaction: TransactionRecord, store: StoreProfile): Bitmap {
        val width = 580
        val items = transaction.itemsSummary.split("; ").filter { it.isNotBlank() }
        val estimatedHeight = 650 + (items.size * 38)
        val bitmap = Bitmap.createBitmap(width, estimatedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
        }
        val paintTitle = Paint().apply {
            color = Color.parseColor("#0D253F")
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val paintCenter = Paint().apply {
            color = Color.DKGRAY
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
        }
        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 2f
        }

        var y = 50f
        canvas.drawText(store.storeName, width / 2f, y, paintTitle)
        y += 35f
        canvas.drawText(store.address, width / 2f, y, paintCenter)
        y += 30f
        canvas.drawText("WhatsApp: ${store.whatsapp}", width / 2f, y, paintCenter)
        y += 25f
        canvas.drawLine(20f, y, width - 20f, y, paintLine)
        y += 30f

        val paintLeft = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
        }
        val paintRight = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("No: ${transaction.id}", 30f, y, paintLeft)
        y += 28f
        canvas.drawText("Tgl: ${FormatUtils.formatDateTime(transaction.timestamp)}", 30f, y, paintLeft)
        y += 28f
        canvas.drawText("Kasir: ${transaction.cashierName} | Pelanggan: ${transaction.customerName}", 30f, y, paintLeft)
        y += 25f
        canvas.drawLine(20f, y, width - 20f, y, paintLine)
        y += 30f

        for (item in items) {
            canvas.drawText(item, 30f, y, paintLeft)
            y += 30f
        }

        canvas.drawLine(20f, y, width - 20f, y, paintLine)
        y += 30f

        canvas.drawText("Subtotal", 30f, y, paintLeft)
        canvas.drawText(FormatUtils.formatRupiah(transaction.subtotal), width - 30f, y, paintRight)
        y += 28f

        if (transaction.discount > 0) {
            canvas.drawText("Diskon", 30f, y, paintLeft)
            canvas.drawText("-${FormatUtils.formatRupiah(transaction.discount)}", width - 30f, y, paintRight)
            y += 28f
        }

        if (transaction.tax > 0) {
            canvas.drawText("PPN (Pajak)", 30f, y, paintLeft)
            canvas.drawText("+${FormatUtils.formatRupiah(transaction.tax)}", width - 30f, y, paintRight)
            y += 28f
        }

        val paintTotal = Paint().apply {
            color = Color.parseColor("#0D253F")
            textSize = 24f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val paintTotalRight = Paint().apply {
            color = Color.parseColor("#0D253F")
            textSize = 24f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("TOTAL", 30f, y, paintTotal)
        canvas.drawText(FormatUtils.formatRupiah(transaction.total), width - 30f, y, paintTotalRight)
        y += 30f

        canvas.drawText("Bayar (${transaction.paymentMethod})", 30f, y, paintLeft)
        canvas.drawText(FormatUtils.formatRupiah(transaction.cashPaid), width - 30f, y, paintRight)
        y += 28f

        canvas.drawText("Kembalian", 30f, y, paintLeft)
        canvas.drawText(FormatUtils.formatRupiah(transaction.change), width - 30f, y, paintRight)
        y += 30f

        canvas.drawLine(20f, y, width - 20f, y, paintLine)
        y += 35f

        val footerLines = store.footerMessage.split("\n")
        for (line in footerLines) {
            canvas.drawText(line, width / 2f, y, paintCenter)
            y += 25f
        }
        canvas.drawText("KasirKu Pintar v1.0 • Rafline Dev", width / 2f, y, paintCenter)

        return bitmap
    }

    fun shareReceiptImage(context: Context, transaction: TransactionRecord, store: StoreProfile) {
        try {
            val bitmap = generateReceiptBitmap(transaction, store)
            val cachePath = File(context.cacheDir, "receipts")
            cachePath.mkdirs()
            val file = File(cachePath, "struk_${transaction.id}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "Struk Transaksi ${transaction.id} - ${store.storeName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Struk Gambar"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan struk: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadReceiptImage(context: Context, transaction: TransactionRecord, store: StoreProfile) {
        try {
            val bitmap = generateReceiptBitmap(transaction, store)
            val dir = File(context.getExternalFilesDir(null), "SavedReceipts")
            dir.mkdirs()
            val file = File(dir, "Struk_${transaction.id}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            Toast.makeText(context, "Struk tersimpan di: ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal menyimpan struk: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareReceiptText(context: Context, transaction: TransactionRecord, store: StoreProfile) {
        val text = generateReceiptText(transaction, store)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Struk ${transaction.id}")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Struk"))
    }

    fun sendToWhatsApp(context: Context, transaction: TransactionRecord, store: StoreProfile, targetPhone: String = "") {
        val text = generateReceiptText(transaction, store)
        val encoded = URLEncoder.encode(text, "UTF-8")
        val cleanPhone = targetPhone.replace("[^0-9]".toRegex(), "")
        val uri = if (cleanPhone.isNotBlank()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encoded")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=$encoded")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // fallback share
            shareReceiptText(context, transaction, store)
        }
    }
}
