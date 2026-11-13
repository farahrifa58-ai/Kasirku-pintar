package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.ui.FirestoreSyncState
import com.example.ui.KasirViewModel
import com.example.ui.components.KasirTopHeader
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.BiruMudaHeaderEnd
import com.example.ui.theme.BiruMudaHeaderStart
import com.example.ui.theme.BiruMudaPrimary
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
            val syncState by viewModel.firestoreSyncState.collectAsState()
            KasirTopHeader(
                storeProfile = storeProfile,
                title = "Kasir Penjualan",
                subtitle = "Pilih item katalog untuk transaksi",
                isHostServerRunning = syncState is FirestoreSyncState.Connected,
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
                        Column(modifier = Modifier.weight(1f, fill = false)) {
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

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = { showCheckoutModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("checkout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCartCheckout,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bayar Sekarang",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                softWrap = false
                            )
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
        val currentDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = currentDensity.density,
                fontScale = 1.0f
            )
        ) {
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
}

// Modal Pembayaran Rapi, Ringkas, dan Modern dengan PPN Manual & Otomatis
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

    val quickCashOptions = listOf(finalTotal, 20000.0, 50000.0, 100000.0, 200000.0, 500000.0).distinct().filter { it >= finalTotal }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        val currentDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = currentDensity.density,
                fontScale = 1.0f
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Drag Handle Minimalis
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                }

                // Header Ringkas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(BiruMudaPrimary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = BiruMudaPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pembayaran Kasir",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ==================== RINGKASAN ITEM PESANAN ====================
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rincian Item (${cart.size} Produk)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Subtotal: ${FormatUtils.formatRupiah(total)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BiruMudaPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        cart.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.formattedQuantity()} × ${FormatUtils.formatRupiah(item.product.sellPrice)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onQuantityChange(item.product.id, -1.0) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RemoveCircleOutline,
                                            contentDescription = "Kurang",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = item.formattedQuantity(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 3.dp)
                                    )
                                    IconButton(
                                        onClick = { onQuantityChange(item.product.id, 1.0) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircleOutline,
                                            contentDescription = "Tambah",
                                            modifier = Modifier.size(18.dp),
                                            tint = BiruMudaPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = FormatUtils.formatRupiah(item.subtotal),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ==================== PENGATURAN PPN (OTOMATIS & MANUAL) ====================
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, BiruMudaPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Percent,
                                    contentDescription = null,
                                    tint = BiruMudaPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Pengaturan PPN (Pajak)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (calculatedTax > 0) BiruMudaPrimary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = if (calculatedTax > 0) "+${FormatUtils.formatRupiah(calculatedTax)}" else "Rp 0",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (calculatedTax > 0) BiruMudaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(7.dp))

                        // Segmented Pill Toggle PPN (Anti-wrap & Ringkas)
                        val taxModes = listOf(
                            "Otomatis" to "Otomatis",
                            "Manual" to "Manual",
                            "Nonaktif" to "Tanpa PPN"
                        )
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                taxModes.forEach { (modeKey, modeLabel) ->
                                    val isSelected = taxMode == modeKey
                                    Surface(
                                        onClick = { taxMode = modeKey },
                                        shape = RoundedCornerShape(7.dp),
                                        color = if (isSelected) BiruMudaPrimary else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = modeLabel,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (taxMode == "Otomatis") {
                            Spacer(modifier = Modifier.height(7.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tarif:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                listOf(10.0, 11.0, 12.0).forEach { p ->
                                    val isRateSelected = taxPercent == p
                                    Surface(
                                        onClick = { taxPercent = p },
                                        shape = RoundedCornerShape(7.dp),
                                        color = if (isRateSelected) BiruMudaPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = if (isRateSelected) BorderStroke(1.dp, BiruMudaPrimary) else null,
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 10.dp)
                                        ) {
                                            Text(
                                                text = "${p.toInt()}%",
                                                fontSize = 11.sp,
                                                fontWeight = if (isRateSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isRateSelected) BiruMudaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (taxMode == "Manual") {
                            Spacer(modifier = Modifier.height(7.dp))
                            CompactCheckoutInput(
                                value = manualTaxInput,
                                onValueChange = { manualTaxInput = it.filter { ch -> ch.isDigit() } },
                                label = "Nominal PPN (Rp)",
                                placeholder = "Contoh: 5000",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ==================== DISKON & NAMA PELANGGAN ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactCheckoutInput(
                        value = discountInput,
                        onValueChange = { discountInput = it.filter { ch -> ch.isDigit() } },
                        label = "Diskon (Rp)",
                        placeholder = "0",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    CompactCheckoutInput(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = "Nama Pelanggan",
                        placeholder = "Umum",
                        keyboardType = KeyboardType.Text,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ==================== METODE PEMBAYARAN ====================
                Text(
                    text = "Metode Pembayaran",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(5.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val paymentRows = listOf(
                        listOf(
                            Triple("Tunai", Icons.Default.Payments, "Tunai"),
                            Triple("QRIS", Icons.Default.QrCode2, "QRIS")
                        ),
                        listOf(
                            Triple("Transfer Bank", Icons.Default.AccountBalance, "Transfer Bank"),
                            Triple("Debit", Icons.Default.CreditCard, "Debit / EDC")
                        )
                    )
                    paymentRows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { (method, icon, label) ->
                                val isSelected = paymentMethod == method
                                Surface(
                                    onClick = { paymentMethod = method },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) BiruMudaPrimary else MaterialTheme.colorScheme.surface,
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else BiruMudaPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Uang Tunai & Pecahan Cepat
                if (paymentMethod == "Tunai") {
                    Spacer(modifier = Modifier.height(8.dp))
                    CompactCheckoutInput(
                        value = cashPaidInput,
                        onValueChange = { cashPaidInput = it.filter { ch -> ch.isDigit() } },
                        label = "Uang Diterima (Rp)",
                        placeholder = "Kosongkan jika uang pas (${FormatUtils.formatRupiah(finalTotal)})",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickCashOptions) { amount ->
                            val isExact = amount == finalTotal
                            val chipSelected = cashPaidInput == amount.toInt().toString()
                            Surface(
                                onClick = { cashPaidInput = amount.toInt().toString() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (chipSelected) BiruMudaPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (chipSelected) BiruMudaPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Text(
                                        text = if (isExact) "Uang Pas" else FormatUtils.formatRupiah(amount),
                                        fontSize = 11.sp,
                                        fontWeight = if (isExact || chipSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isExact || chipSelected) BiruMudaPrimary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(9.dp))

                // ==================== RINGKASAN TOTAL PEMBAYARAN ====================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(BiruMudaHeaderStart, BiruMudaHeaderEnd)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Baris rincian mini (Subtotal • Diskon • PPN)
                        val summaryParts = buildList {
                            add("Subtotal ${FormatUtils.formatRupiah(total)}")
                            if (discount > 0) add("Diskon -${FormatUtils.formatRupiah(discount)}")
                            if (calculatedTax > 0) add("PPN +${FormatUtils.formatRupiah(calculatedTax)}")
                        }.joinToString("  •  ")

                        Text(
                            text = summaryParts,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL BAYAR",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = FormatUtils.formatRupiah(finalTotal),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        if (paymentMethod == "Tunai" && cashPaid >= finalTotal) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kembalian",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                                Text(
                                    text = FormatUtils.formatRupiah(change),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tombol Selesaikan Transaksi
                Button(
                    onClick = {
                        onConfirmCheckout(paymentMethod, cashPaid, discount, calculatedTax, customerName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("confirm_checkout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BiruMudaPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Selesaikan Transaksi & Cetak",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun CompactCheckoutInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        modifier = modifier.height(46.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = BiruMudaPrimary,
                maxLines = 1,
                softWrap = false
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(BiruMudaPrimary),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
