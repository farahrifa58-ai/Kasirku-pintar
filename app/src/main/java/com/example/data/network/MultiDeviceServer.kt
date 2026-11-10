package com.example.data.network

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import com.example.data.model.StoreProfile
import com.example.data.model.TransactionRecord
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

data class ConnectedClient(
    val ip: String,
    val deviceName: String,
    val connectedAt: Long = System.currentTimeMillis()
)

class MultiDeviceServer(private val context: Context) {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val connectedClients = CopyOnWriteArrayList<ConnectedClient>()

    var onStatusRequested: (() -> Pair<StoreProfile, List<TransactionRecord>>)? = null

    fun getLocalIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiIp = wifiManager?.connectionInfo?.ipAddress
            if (wifiIp != null && wifiIp != 0) {
                @Suppress("DEPRECATION")
                return Formatter.formatIpAddress(wifiIp)
            }

            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "192.168.1.10"
    }

    fun startServer(port: Int = 8080): Boolean {
        if (isRunning) return true
        return try {
            serverSocket = ServerSocket(port)
            isRunning = true
            scope.launch {
                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch { handleClient(clientSocket) }
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        connectedClients.clear()
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val path = parts[1]

            val clientIp = socket.inetAddress.hostAddress ?: "Unknown"

            // Keep track of connected clients
            if (connectedClients.none { it.ip == clientIp }) {
                connectedClients.add(
                    ConnectedClient(
                        ip = clientIp,
                        deviceName = "Smartphone #${connectedClients.size + 1}"
                    )
                )
            }

            when {
                path.startsWith("/api/status") -> {
                    val data = onStatusRequested?.invoke()
                    val store = data?.first ?: StoreProfile()
                    val transactions = data?.second ?: emptyList()

                    val totalSales = transactions.sumOf { it.total }
                    val totalProfit = transactions.sumOf { it.totalProfit }

                    val json = JSONObject().apply {
                        put("status", "online")
                        put("storeName", store.storeName)
                        put("totalSales", totalSales)
                        put("totalProfit", totalProfit)
                        put("transactionCount", transactions.size)
                        put("connectedDevicesCount", connectedClients.size)
                        put("timestamp", System.currentTimeMillis())
                    }
                    sendJsonResponse(out, 200, json.toString())
                }

                path.startsWith("/api/transactions") -> {
                    val data = onStatusRequested?.invoke()
                    val transactions = data?.second ?: emptyList()

                    val array = JSONArray()
                    for (trx in transactions.take(30)) {
                        val obj = JSONObject().apply {
                            put("id", trx.id)
                            put("timestamp", trx.timestamp)
                            put("items", trx.itemsSummary)
                            put("total", trx.total)
                            put("profit", trx.totalProfit)
                            put("paymentMethod", trx.paymentMethod)
                            put("customerName", trx.customerName)
                            put("cashierName", trx.cashierName)
                        }
                        array.put(obj)
                    }
                    sendJsonResponse(out, 200, array.toString())
                }

                path.startsWith("/api/ping") -> {
                    val json = JSONObject().apply {
                        put("status", "pong")
                        put("server", "KasirPro Host")
                    }
                    sendJsonResponse(out, 200, json.toString())
                }

                else -> {
                    val json = JSONObject().apply {
                        put("message", "KasirPro Multi-Device Endpoint Ready")
                    }
                    sendJsonResponse(out, 200, json.toString())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun sendJsonResponse(out: OutputStream, code: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $code OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }
}
