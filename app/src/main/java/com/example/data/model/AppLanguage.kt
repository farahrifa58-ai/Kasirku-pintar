package com.example.data.model

enum class AppLanguage {
    INDONESIA,
    ENGLISH;

    val displayName: String
        get() = when (this) {
            INDONESIA -> "Bahasa Indonesia"
            ENGLISH -> "English"
        }
}

object StringsHelper {
    fun get(key: String, language: AppLanguage): String {
        val isId = language == AppLanguage.INDONESIA
        return when (key) {
            "app_title" -> if (isId) "KasirKu Pintar" else "KasirKu Pintar"
            "welcome" -> if (isId) "Selamat Datang" else "Welcome"
            "today_summary" -> if (isId) "Ringkasan Aktivitas Hari Ini" else "Today's Activity Summary"
            "total_sales" -> if (isId) "Total Penjualan" else "Total Sales"
            "total_transactions" -> if (isId) "Total Transaksi" else "Total Transactions"
            "net_profit" -> if (isId) "Laba Bersih" else "Net Profit"
            "trending_sales" -> if (isId) "Tren Penjualan Hari Ini" else "Sales Trending Today"
            "nav_dashboard" -> if (isId) "Beranda" else "Dashboard"
            "nav_pos" -> if (isId) "Kasir" else "POS"
            "nav_products" -> if (isId) "Produk" else "Products"
            "nav_reports" -> if (isId) "Laporan" else "Reports"
            "nav_multidevice" -> if (isId) "Multi-HP" else "Multi-Device"
            "settings" -> if (isId) "Pengaturan" else "Settings"
            "theme" -> if (isId) "Tema Tampilan" else "Display Theme"
            "theme_light" -> if (isId) "Terang (Light)" else "Light"
            "theme_dark" -> if (isId) "Gelap (Dark)" else "Dark"
            "language" -> if (isId) "Bahasa" else "Language"
            "store_profile" -> if (isId) "Profil Toko" else "Store Profile"
            "store_name" -> if (isId) "Nama Toko" else "Store Name"
            "store_owner" -> if (isId) "Pemilik Toko" else "Store Owner"
            "store_address" -> if (isId) "Alamat Toko" else "Store Address"
            "store_whatsapp" -> if (isId) "No. WhatsApp" else "WhatsApp Number"
            "upload_logo" -> if (isId) "Ganti Logo Toko" else "Change Store Logo"
            "printer_settings" -> if (isId) "Printer Bluetooth" else "Bluetooth Printer"
            "print_receipt" -> if (isId) "Print Struk" else "Print Receipt"
            "share" -> if (isId) "Share" else "Share"
            "share_receipt" -> if (isId) "Bagikan Struk" else "Share Receipt"
            "share_image" -> if (isId) "Bagikan Gambar Struk" else "Share Receipt Image"
            "share_pdf" -> if (isId) "Bagikan Struk PDF / Dokumen" else "Share Receipt PDF / Document"
            "share_whatsapp" -> if (isId) "Kirim via WhatsApp" else "Send via WhatsApp"
            "download_image" -> if (isId) "Unduh Gambar Struk" else "Download Receipt Image"
            "about_app" -> if (isId) "Tentang Aplikasi" else "About App"
            "about_developer" -> if (isId) "Developer: Rafline Dev" else "Developer: Rafline Dev"
            "app_version" -> if (isId) "Versi Aplikasi 1.0 Full Version" else "App Version 1.0 Full Version"
            "checkout" -> if (isId) "Bayar / Checkout" else "Checkout"
            "cart" -> if (isId) "Keranjang" else "Cart"
            "filter_today" -> if (isId) "Hari Ini" else "Today"
            "filter_week" -> if (isId) "Minggu Ini" else "This Week"
            "filter_month" -> if (isId) "Bulan Ini" else "This Month"
            "filter_year" -> if (isId) "Tahun Ini" else "This Year"
            "filter_all" -> if (isId) "Semua" else "All"
            else -> key
        }
    }
}
