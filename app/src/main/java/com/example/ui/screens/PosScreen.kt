package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.ui.KasirViewModel
import com.example.ui.components.KasirTopHeader
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavyVariant
import com.example.util.FormatUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: KasirViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storeProfile by viewModel.storeProfile.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val isHostRunning by viewModel.isHostRunning.collectAsState()
    val connectedClientsCount by viewModel.connectedClientsCount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var showCheckoutModal by remember { mutableStateOf(false) }

    // Dialog timbangan otomatis untuk produk kg/meter/liter
    var weightProductToSelect by remember { mutableStateOf<Product?>(null) }

    val categories = remember(allProducts) {
        listOf("Semua") + allProducts.map { it.category }.distinct()
    }

    val filteredProducts = remember(allProducts, searchQuery, selectedCategory) {
        allProducts.filter { product ->
            val matchCategory = selectedCategory == "Semua" || product.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = searchQuery.isBlank() || product.name.contains(searchQuery, ignoreCase = true) || product.sku.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    val cartTotal = remember(cart) { cart.sumOf { it.subtotal } }
    val cartItemCount = remember(cart) { cart.size }

    Scaffold(
        topBar = {
            KasirTopHeader(
                storeProfile = storeProfile,
                title = "Kasir Penjualan",
                subtitle = "Pilih item katalog untuk transaksi",
                connectedDevicesCount = connectedClientsCount + 1,
                isHostServerRunning = isHostRunning,
                onSettingsClick = onOpenSettings
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(visible = cart.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(
                                    containerColor = NavyPrimary,
                                    contentColor = Color.White
                                ) {
                                    Text("$cartItemCount Item", modifier = Modifier.padding(2.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Keranjang",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = FormatUtils.formatRupiah(cartTotal),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }

                        Button(
                            onClick = { showCheckoutModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("checkout_button")
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingCartCheckout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bayar Sekarang", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar & Filter Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari produk / barcode...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_product_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Products Grid
            if (allProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(NavyPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBusiness,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = NavyPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Katalog Kasir Masih Kosong",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Buka menu Produk untuk menambahkan barang dagangan Anda sendiri.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak ada produk yang cocok dengan '$searchQuery'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val cartItem = cart.find { it.product.id == product.id }
                        ProductGridCard(
                            product = product,
                            cartItem = cartItem,
                            onProductClick = {
                                if (product.stock <= 0) {
                                    Toast.makeText(context, "Stok ${product.name} habis!", Toast.LENGTH_SHORT).show()
                                    return@ProductGridCard
                                }

                                if (product.isWeightBased || product.unit.equals("kg", ignoreCase = true)) {
                                    // Buka dialog otomatis pilihan timbangan kg
                                    weightProductToSelect = product
                                } else {
                                    // Langsung tambah 1 satuan
                                    viewModel.addToCart(product, 1.0)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Pemilihan Bobot Otomatis (0.25, 0.50, 1.0, 2.0, dll & input manual)
    if (weightProductToSelect != null) {
        WeightSelectionDialog(
            product = weightProductToSelect!!,
            onDismiss = { weightProductToSelect = null },
            onConfirm = { qty ->
                viewModel.addToCart(weightProductToSelect!!, qty)
                weightProductToSelect = null
            }
        )
    }

    // Modal Checkout & Pembayaran (Rapi dengan PPN Manual & Otomatis)
    if (showCheckoutModal) {
        CheckoutBottomSheet(
            cart = cart,
            total = cartTotal,
            defaultTaxEnabled = storeProfile.taxEnabled,
            defaultTaxPercent = storeProfile.defaultTaxPercent,
            onDismiss = { showCheckoutModal = false },
            onQuantityChange = { id, delta -> viewModel.updateCartQuantity(id, delta) },
            onConfirmCheckout = { method, paid, discount, tax, customer ->
                viewModel.checkout(
                    paymentMethod = method,
                    cashPaid = paid,
                    discount = discount,
                    tax = tax,
                    customerName = customer
                )
                showCheckoutModal = false
            }
        )
    }
}

@Composable
fun ProductGridCard(
    product: Product,
    cartItem: CartItem?,
    onProductClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductClick() }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(NavyPrimary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUri != null) {
                    AsyncImage(
                        model = product.imageUri,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = when (product.category.lowercase()) {
                            "sayur & buah" -> Icons.Default.Eco
                            "sembako & pokok" -> Icons.Default.ShoppingBasket
                            "fashion & pakaian" -> Icons.Default.Checkroom
                            "elektronik & gadget" -> Icons.Default.Devices
                            "bahan bangunan" -> Icons.Default.Construction
                            "makanan & minuman" -> Icons.Default.Restaurant
                            else -> Icons.Default.Inventory2
                        },
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // In-Cart Badge with formatted quantity
                if (cartItem != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentEmerald,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = cartItem.formattedQuantity(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Unit badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "/${product.unit}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = FormatUtils.formatRupiah(product.sellPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "Stok: ${if (product.stock % 1.0 == 0.0) product.stock.toInt() else product.stock}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.stock <= 5) Color(0xFFE63946) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Dialog Timbangan Bobot Otomatis untuk Produk Satuan KG
@Composable
fun WeightSelectionDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var selectedQty by remember { mutableStateOf(1.0) }
    var manualQtyInput by remember { mutableStateOf("1.0") }

    val presetWeights = listOf(0.25, 0.50, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0)
    val calculatedSubtotal = product.sellPrice * selectedQty

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.96f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Timbang / Pilih Bobot",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                        Text(
                            text = "${product.name} • ${FormatUtils.formatRupiah(product.sellPrice)}/${product.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Pilihan Cepat Otomatis (${product.unit}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Presets 0.25, 0.50, 1.0, 2.0 dan seterusnya
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetWeights.take(4).forEach { weight ->
                        FilterChip(
                            selected = selectedQty == weight,
                            onClick = {
                                selectedQty = weight
                                manualQtyInput = weight.toString()
                            },
                            label = {
                                Text(
                                    text = if (weight == 0.25) "0,25 (1/4)" else if (weight == 0.5) "0,50 (1/2)" else "$weight ${product.unit}",
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetWeights.drop(4).forEach { weight ->
                        FilterChip(
                            selected = selectedQty == weight,
                            onClick = {
                                selectedQty = weight
                                manualQtyInput = weight.toString()
                            },
                            label = { Text(text = "$weight ${product.unit}", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Manual
                OutlinedTextField(
                    value = manualQtyInput,
                    onValueChange = { input ->
                        val clean = input.replace(',', '.')
                        manualQtyInput = clean
                        val parsed = clean.toDoubleOrNull()
                        if (parsed != null && parsed > 0.0) {
                            selectedQty = parsed
                        }
                    },
                    label = { Text("Atau Isi Bobot Manual (${product.unit})") },
                    placeholder = { Text("Contoh: 0.35 atau 1.75") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = {
                        Text(
                            text = product.unit,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Kalkulasi Subtotal Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentEmerald.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Total Harga Bobot:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "$selectedQty ${product.unit} × ${FormatUtils.formatRupiah(product.sellPrice)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = FormatUtils.formatRupiah(calculatedSubtotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AccentEmerald
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (selectedQty > 0.0) {
                            onConfirm(selectedQty)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_weight_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tambahkan ke Keranjang", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Modal Pembayaran Rapi dengan PPN Manual & Otomatis
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutBottomSheet(
    cart: List<CartItem>,
    total: Double,
    defaultTaxEnabled: Boolean,
    defaultTaxPercent: Double,
    onDismiss: () -> Unit,
    onQuantityChange: (Long, Double) -> Unit,
    onConfirmCheckout: (String, Double, Double, Double, String) -> Unit
) {
    var paymentMethod by remember { mutableStateOf("Tunai") }
    var customerName by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }
    var cashPaidInput by remember { mutableStateOf("") }

    // PPN State
    var taxMode by remember { mutableStateOf(if (defaultTaxEnabled) "Otomatis" else "Nonaktif") } // "Otomatis", "Manual", "Nonaktif"
    var taxPercent by remember { mutableStateOf(defaultTaxPercent) }
    var manualTaxInput by remember { mutableStateOf("") }

    val discount = discountInput.toDoubleOrNull() ?: 0.0
    val afterDiscount = (total - discount).coerceAtLeast(0.0)

    val calculatedTax = when (taxMode) {
        "Otomatis" -> (afterDiscount * (taxPercent / 100.0))
        "Manual" -> manualTaxInput.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    val finalTotal = (afterDiscount + calculatedTax).coerceAtLeast(0.0)

    val cashPaid = when (paymentMethod) {
        "Tunai" -> cashPaidInput.toDoubleOrNull() ?: finalTotal
        else -> finalTotal
    }
    val change = (cashPaid - finalTotal).coerceAtLeast(0.0)

    val paymentOptions = listOf("Tunai", "QRIS", "Transfer Bank", "Debit")
    val quickCashOptions = listOf(finalTotal, 20000.0, 50000.0, 100000.0, 200000.0, 500000.0).distinct().filter { it >= finalTotal }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = NavyPrimary)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Pembayaran Kasir",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==================== RINGKASAN ITEM PESANAN ====================
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Rincian Item (${cart.size} Produk)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    cart.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${item.formattedQuantity()} × ${FormatUtils.formatRupiah(item.product.sellPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onQuantityChange(item.product.id, -1.0) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "Kurang")
                                }
                                Text(
                                    text = item.formattedQuantity(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                IconButton(
                                    onClick = { onQuantityChange(item.product.id, 1.0) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Tambah")
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = FormatUtils.formatRupiah(item.subtotal),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== PENGATURAN PPN (OTOMATIS & MANUAL) ====================
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Percent, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pengaturan PPN (Pajak)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode PPN Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = taxMode == "Otomatis",
                            onClick = { taxMode = "Otomatis" },
                            label = { Text("PPN Otomatis") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NavyPrimary, selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = taxMode == "Manual",
                            onClick = { taxMode = "Manual" },
                            label = { Text("PPN Manual") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NavyPrimary, selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = taxMode == "Nonaktif",
                            onClick = { taxMode = "Nonaktif" },
                            label = { Text("Tanpa PPN") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NavyPrimary, selectedLabelColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (taxMode == "Otomatis") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Tarif PPN:", style = MaterialTheme.typography.bodySmall)
                            listOf(11.0, 10.0, 12.0).forEach { p ->
                                SuggestionChip(
                                    onClick = { taxPercent = p },
                                    label = { Text("${p.toInt()}%", fontWeight = if (taxPercent == p) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "+${FormatUtils.formatRupiah(calculatedTax)}",
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                        }
                    } else if (taxMode == "Manual") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualTaxInput,
                            onValueChange = { manualTaxInput = it },
                            label = { Text("Nominal PPN (Rp)") },
                            placeholder = { Text("Contoh: 5000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== DISKON & NAMA PELANGGAN ====================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it },
                    label = { Text("Potongan Diskon (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nama Pelanggan") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== METODE PEMBAYARAN ====================
            Text(text = "Metode Pembayaran", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentOptions.forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Uang Tunai & Pecahan
            if (paymentMethod == "Tunai") {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = cashPaidInput,
                    onValueChange = { cashPaidInput = it },
                    label = { Text("Nominal Uang Diterima (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickCashOptions) { amount ->
                        SuggestionChip(
                            onClick = { cashPaidInput = amount.toInt().toString() },
                            label = { Text(if (amount == finalTotal) "Uang Pas" else FormatUtils.formatRupiah(amount)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==================== RINGKASAN TOTAL PEMBAYARAN ====================
            Card(
                colors = CardDefaults.cardColors(containerColor = NavyVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Subtotal", color = Color.White.copy(alpha = 0.8f))
                        Text(text = FormatUtils.formatRupiah(total), color = Color.White)
                    }
                    if (discount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Diskon", color = Color.White.copy(alpha = 0.8f))
                            Text(text = "-${FormatUtils.formatRupiah(discount)}", color = Color(0xFFFF8A80))
                        }
                    }
                    if (calculatedTax > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "PPN (Pajak)", color = Color.White.copy(alpha = 0.8f))
                            Text(text = "+${FormatUtils.formatRupiah(calculatedTax)}", color = Color(0xFFFFD54F))
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL BAYAR",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = FormatUtils.formatRupiah(finalTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (paymentMethod == "Tunai" && cashPaid >= finalTotal) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Kembalian", color = Color.White.copy(alpha = 0.85f))
                            Text(
                                text = FormatUtils.formatRupiah(change),
                                fontWeight = FontWeight.Bold,
                                color = AccentEmerald
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Selesaikan Transaksi
            Button(
                onClick = {
                    onConfirmCheckout(paymentMethod, cashPaid, discount, calculatedTax, customerName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_checkout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Selesaikan Transaksi & Cetak", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
