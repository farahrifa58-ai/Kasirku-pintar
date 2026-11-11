package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionRecord
import com.example.util.FormatUtils
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestorePosRepository(
    val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {

    // Convenience constructor resolving named database ID from string resources
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    fun getStoreIdForCurrentUser(): String {
        val uid = requireUserId()
        return "store_$uid"
    }

    fun getCurrentStoreId(): String? {
        return auth.currentUser?.uid?.let { "store_$it" }
    }

    suspend fun verifyDatabaseAccess(): Result<String> {
        return try {
            val uid = requireUserId()
            val storeId = getStoreIdForCurrentUser()
            val docRef = db.collection("stores").document(storeId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val newStore = hashMapOf(
                    "storeId" to storeId,
                    "ownerUid" to uid,
                    "ownerEmail" to (auth.currentUser?.email ?: ""),
                    "cashierUids" to listOf<String>(),
                    "storeName" to "Toko Kasir",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(newStore).await()
            }
            Result.success(storeId)
        } catch (e: Exception) {
            Log.e("FirestorePOS", "Database access verification failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== STORE PROFILE ====================

    suspend fun getOrCreateStore(storeProfile: StoreProfile): String {
        val uid = requireUserId()
        val storeId = getStoreIdForCurrentUser()
        val storeDocRef = db.collection("stores").document(storeId)

        try {
            val snapshot = storeDocRef.get().await()
            if (!snapshot.exists()) {
                val newStore = hashMapOf(
                    "storeId" to storeId,
                    "ownerUid" to uid,
                    "ownerEmail" to (auth.currentUser?.email ?: ""),
                    "cashierUids" to listOf<String>(),
                    "storeName" to storeProfile.storeName,
                    "ownerName" to storeProfile.ownerName,
                    "address" to storeProfile.address,
                    "whatsapp" to storeProfile.whatsapp,
                    "footerMessage" to storeProfile.footerMessage,
                    "taxEnabled" to storeProfile.taxEnabled,
                    "defaultTaxPercent" to storeProfile.defaultTaxPercent,
                    "paperSize" to storeProfile.paperSize,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                storeDocRef.set(newStore).await()
            } else {
                // If the user is the owner, keep store details updated
                val ownerUid = snapshot.getString("ownerUid")
                if (ownerUid == uid) {
                    val updates = hashMapOf<String, Any>(
                        "storeName" to storeProfile.storeName,
                        "ownerName" to storeProfile.ownerName,
                        "address" to storeProfile.address,
                        "whatsapp" to storeProfile.whatsapp,
                        "footerMessage" to storeProfile.footerMessage,
                        "taxEnabled" to storeProfile.taxEnabled,
                        "defaultTaxPercent" to storeProfile.defaultTaxPercent,
                        "paperSize" to storeProfile.paperSize,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    storeDocRef.update(updates).await()
                }
            }
            return storeId
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, storeDocRef.path)
            throw e
        }
    }

    suspend fun joinStoreAsCashier(storeCode: String): Boolean {
        return true
    }

    fun observeStore(storeId: String): Flow<StoreProfile?> = callbackFlow {
        if (!isUserSignedIn()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val cleanId = storeId.trim()
        val docRef = db.collection("stores").document(cleanId)

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = StoreProfile(
                    storeName = snapshot.getString("storeName") ?: "Toko KasirKu",
                    ownerName = snapshot.getString("ownerName") ?: "Pemilik",
                    address = snapshot.getString("address") ?: "",
                    whatsapp = snapshot.getString("whatsapp") ?: "",
                    footerMessage = snapshot.getString("footerMessage") ?: "Terima Kasih",
                    logoUri = null,
                    taxEnabled = snapshot.getBoolean("taxEnabled") ?: true,
                    defaultTaxPercent = snapshot.getDouble("defaultTaxPercent") ?: 11.0,
                    cloudSyncCode = cleanId,
                    paperSize = snapshot.getString("paperSize") ?: "58mm"
                )
                trySend(profile)
            } else {
                trySend(null)
            }
        }

        awaitClose { registration.remove() }
    }

    // ==================== PRODUCTS ====================

    fun observeProducts(storeId: String): Flow<List<Product>> = callbackFlow {
        if (!isUserSignedIn()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val cleanId = storeId.trim()
        val query = db.collection("stores").document(cleanId).collection("products")
            .orderBy("name", Query.Direction.ASCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "stores/$cleanId/products")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val products = snapshot.documents.mapNotNull { doc ->
                    val idLong = doc.id.toLongOrNull() ?: doc.getLong("id") ?: System.currentTimeMillis()
                    Product(
                        id = idLong,
                        name = doc.getString("name") ?: "",
                        sku = doc.getString("sku") ?: "",
                        category = doc.getString("category") ?: "Umum",
                        unit = doc.getString("unit") ?: "pcs",
                        buyPrice = doc.getDouble("buyPrice") ?: 0.0,
                        sellPrice = doc.getDouble("sellPrice") ?: 0.0,
                        stock = doc.getDouble("stock") ?: 0.0,
                        imageUri = doc.getString("imageUri")
                    )
                }
                trySend(products)
            }
        }

        awaitClose { registration.remove() }
    }

    suspend fun saveProduct(storeId: String, product: Product) {
        val cleanId = storeId.trim()
        val prodIdStr = if (product.id > 0) product.id.toString() else System.currentTimeMillis().toString()
        val docRef = db.collection("stores").document(cleanId).collection("products").document(prodIdStr)

        val payload = hashMapOf<String, Any>(
            "id" to prodIdStr,
            "storeId" to cleanId,
            "name" to product.name,
            "sku" to product.sku,
            "category" to product.category,
            "unit" to product.unit,
            "buyPrice" to product.buyPrice,
            "sellPrice" to product.sellPrice,
            "stock" to product.stock,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (product.imageUri != null) {
            payload["imageUri"] = product.imageUri
        }

        try {
            val exists = docRef.get().await().exists()
            if (!exists) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteProduct(storeId: String, productId: Long) {
        val cleanId = storeId.trim()
        val docRef = db.collection("stores").document(cleanId).collection("products").document(productId.toString())
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // ==================== TRANSACTIONS ====================

    fun observeTransactions(storeId: String): Flow<List<TransactionRecord>> = callbackFlow {
        if (!isUserSignedIn()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val cleanId = storeId.trim()
        val query = db.collection("stores").document(cleanId).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "stores/$cleanId/transactions")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val transactions = snapshot.documents.mapNotNull { doc ->
                    TransactionRecord(
                        id = doc.getString("id") ?: doc.id,
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        itemsSummary = doc.getString("itemsSummary") ?: "",
                        totalItems = doc.getLong("totalItems")?.toInt() ?: 1,
                        subtotal = doc.getDouble("subtotal") ?: 0.0,
                        discount = doc.getDouble("discount") ?: 0.0,
                        tax = doc.getDouble("tax") ?: 0.0,
                        total = doc.getDouble("total") ?: 0.0,
                        totalProfit = doc.getDouble("totalProfit") ?: 0.0,
                        paymentMethod = doc.getString("paymentMethod") ?: "Tunai",
                        cashPaid = doc.getDouble("cashPaid") ?: 0.0,
                        change = doc.getDouble("change") ?: 0.0,
                        customerName = doc.getString("customerName") ?: "Pelanggan",
                        cashierName = doc.getString("cashierName") ?: "Kasir",
                        notes = doc.getString("notes") ?: ""
                    )
                }
                trySend(transactions)
            }
        }

        awaitClose { registration.remove() }
    }

    suspend fun recordTransactionAndDeductStock(
        storeId: String,
        transaction: TransactionRecord,
        cartItems: List<CartItem>
    ) {
        val uid = requireUserId()
        val cleanId = storeId.trim()
        val trxDocRef = db.collection("stores").document(cleanId).collection("transactions").document(transaction.id)

        val trxPayload = hashMapOf<String, Any>(
            "id" to transaction.id,
            "storeId" to cleanId,
            "timestamp" to transaction.timestamp,
            "itemsSummary" to transaction.itemsSummary,
            "totalItems" to transaction.totalItems,
            "subtotal" to transaction.subtotal,
            "discount" to transaction.discount,
            "tax" to transaction.tax,
            "total" to transaction.total,
            "totalProfit" to transaction.totalProfit,
            "paymentMethod" to transaction.paymentMethod,
            "cashPaid" to transaction.cashPaid,
            "change" to transaction.change,
            "customerName" to transaction.customerName,
            "cashierName" to transaction.cashierName,
            "cashierUid" to uid,
            "notes" to transaction.notes,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            // Write transaction atomically using batch to avoid duplicates
            val batch = db.batch()
            batch.set(trxDocRef, trxPayload, SetOptions.merge())

            // Decrement stock in Firestore for each sold item
            for (item in cartItems) {
                if (item.product.id > 0) {
                    val prodDocRef = db.collection("stores").document(cleanId)
                        .collection("products").document(item.product.id.toString())
                    batch.set(
                        prodDocRef,
                        hashMapOf(
                            "id" to item.product.id.toString(),
                            "storeId" to cleanId,
                            "name" to item.product.name,
                            "sku" to item.product.sku,
                            "category" to item.product.category,
                            "unit" to item.product.unit,
                            "buyPrice" to item.product.buyPrice,
                            "sellPrice" to item.product.sellPrice,
                            "stock" to FieldValue.increment(-item.quantity),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ),
                        SetOptions.merge()
                    )
                }
            }

            batch.commit().await()
            Log.d("FirestorePOS", "Transaction ${transaction.id} committed and stock decremented.")
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, trxDocRef.path)
            throw e
        }
    }

    // ==================== MIGRATION ====================

    suspend fun migrateDataFromLocalRoom(
        storeId: String,
        localProducts: List<Product>,
        localTransactions: List<TransactionRecord>
    ): Pair<Int, Int> {
        val cleanId = storeId.trim()
        var migratedProducts = 0
        var migratedTrx = 0

        try {
            // Check existing products in cloud
            val cloudProdDocs = db.collection("stores").document(cleanId).collection("products").get().await()
            val cloudProdIds = cloudProdDocs.documents.map { it.id }.toSet()

            for (p in localProducts) {
                val pIdStr = p.id.toString()
                if (!cloudProdIds.contains(pIdStr)) {
                    saveProduct(cleanId, p)
                    migratedProducts++
                }
            }

            // Check existing transactions in cloud
            val cloudTrxDocs = db.collection("stores").document(cleanId).collection("transactions").get().await()
            val cloudTrxIds = cloudTrxDocs.documents.map { it.id }.toSet()

            for (t in localTransactions) {
                if (!cloudTrxIds.contains(t.id)) {
                    val docRef = db.collection("stores").document(cleanId).collection("transactions").document(t.id)
                    val payload = hashMapOf<String, Any>(
                        "id" to t.id,
                        "storeId" to cleanId,
                        "timestamp" to t.timestamp,
                        "itemsSummary" to t.itemsSummary,
                        "totalItems" to t.totalItems,
                        "subtotal" to t.subtotal,
                        "discount" to t.discount,
                        "tax" to t.tax,
                        "total" to t.total,
                        "totalProfit" to t.totalProfit,
                        "paymentMethod" to t.paymentMethod,
                        "cashPaid" to t.cashPaid,
                        "change" to t.change,
                        "customerName" to t.customerName,
                        "cashierName" to t.cashierName,
                        "cashierUid" to requireUserId(),
                        "notes" to t.notes,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(payload, SetOptions.merge()).await()
                    migratedTrx++
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "stores/$cleanId/migration")
            Log.e("FirestorePOS", "Migration partial error: ${e.message}")
        }

        return Pair(migratedProducts, migratedTrx)
    }
}
