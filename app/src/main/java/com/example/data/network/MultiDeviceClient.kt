package com.example.data.network

import com.example.data.model.TransactionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class RemoteServerStatus(
    val isOnline: Boolean,
    val storeName: String = "",
    val totalSales: Double = 0.0,
    val totalProfit: Double = 0.0,
    val transactionCount: Int = 0,
    val connectedDevicesCount: Int = 0,
    val lastUpdate: Long = System.currentTimeMillis()
)

class MultiDeviceClient {

    suspend fun checkConnection(hostAddress: String, port: Int = 8080): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanHost = hostAddress.trim().removePrefix("http://").removeSuffix("/")
            val url = URL("http://$cleanHost:$port/api/ping")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
            }
            val responseCode = conn.responseCode
            conn.disconnect()
            responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchStatus(hostAddress: String, port: Int = 8080): RemoteServerStatus? = withContext(Dispatchers.IO) {
        try {
            val cleanHost = hostAddress.trim().removePrefix("http://").removeSuffix("/")
            val url = URL("http://$cleanHost:$port/api/status")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val json = JSONObject(response)
                RemoteServerStatus(
                    isOnline = true,
                    storeName = json.optString("storeName", "Toko KasirPro"),
                    totalSales = json.optDouble("totalSales", 0.0),
                    totalProfit = json.optDouble("totalProfit", 0.0),
                    transactionCount = json.optInt("transactionCount", 0),
                    connectedDevicesCount = json.optInt("connectedDevicesCount", 1),
                    lastUpdate = json.optLong("timestamp", System.currentTimeMillis())
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchTransactions(hostAddress: String, port: Int = 8080): List<TransactionRecord> = withContext(Dispatchers.IO) {
        try {
            val cleanHost = hostAddress.trim().removePrefix("http://").removeSuffix("/")
            val url = URL("http://$cleanHost:$port/api/transactions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                conn.disconnect()

                val array = JSONArray(response)
                val list = mutableListOf<TransactionRecord>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        TransactionRecord(
                            id = obj.getString("id"),
                            timestamp = obj.getLong("timestamp"),
                            itemsSummary = obj.getString("items"),
                            totalItems = 1,
                            subtotal = obj.getDouble("total"),
                            discount = 0.0,
                            tax = 0.0,
                            total = obj.getDouble("total"),
                            totalProfit = obj.optDouble("profit", 0.0),
                            paymentMethod = obj.getString("paymentMethod"),
                            cashPaid = obj.getDouble("total"),
                            change = 0.0,
                            customerName = obj.optString("customerName", "Pelanggan"),
                            cashierName = obj.optString("cashierName", "Kasir Utama")
                        )
                    )
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
