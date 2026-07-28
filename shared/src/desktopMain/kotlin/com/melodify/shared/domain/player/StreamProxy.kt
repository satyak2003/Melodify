package com.melodify.shared.domain.player

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder

object StreamProxy {
    private var serverSocket: ServerSocket? = null
    private var proxyPort = 0
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isStarted = false

    fun start(): Int {
        if (isStarted && proxyPort > 0 && serverSocket?.isClosed == false) return proxyPort
        try {
            val server = ServerSocket(0) // Bind to free local port
            serverSocket = server
            proxyPort = server.localPort
            isStarted = true

            scope.launch {
                while (isActive && !server.isClosed) {
                    try {
                        val client = server.accept()
                        launch { handleClient(client) }
                    } catch (e: Exception) {
                        if (server.isClosed) break
                    }
                }
            }
        } catch (e: Exception) {
            println("StreamProxy start error: ${e.message}")
        }
        return proxyPort
    }

    fun getProxyUrl(targetUrl: String): String {
        val port = start()
        val encoded = URLEncoder.encode(targetUrl, "UTF-8")
        return "http://127.0.0.1:$port/stream.m4a?url=$encoded"
    }

    private suspend fun handleClient(socket: java.net.Socket) = withContext(Dispatchers.IO) {
        try {
            socket.soTimeout = 30000
            val input = socket.getInputStream()
            val reader = BufferedReader(InputStreamReader(input))
            val requestLine = reader.readLine() ?: return@withContext

            val parts = requestLine.split(" ")
            if (parts.size < 2 || (!parts[0].equals("GET", ignoreCase = true) && !parts[0].equals("HEAD", ignoreCase = true))) {
                socket.close()
                return@withContext
            }

            val isHead = parts[0].equals("HEAD", ignoreCase = true)
            val path = parts[1]
            val urlQuery = path.substringAfter("url=", "")
            if (urlQuery.isEmpty()) {
                socket.close()
                return@withContext
            }

            val targetUrl = URLDecoder.decode(urlQuery, "UTF-8")

            var rangeHeader: String? = null
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                if (line!!.startsWith("Range:", ignoreCase = true)) {
                    rangeHeader = line!!.substringAfter("Range:").trim()
                }
            }

            val connection = URL(targetUrl).openConnection() as HttpURLConnection
            connection.requestMethod = if (isHead) "HEAD" else "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

            if (rangeHeader != null) {
                connection.setRequestProperty("Range", rangeHeader)
            }

            val responseCode = connection.responseCode
            val contentLength = connection.contentLengthLong

            val out = socket.getOutputStream()

            val statusLine = when (responseCode) {
                206 -> "HTTP/1.1 206 Partial Content\r\n"
                200 -> "HTTP/1.1 200 OK\r\n"
                else -> "HTTP/1.1 $responseCode OK\r\n"
            }

            var headers = statusLine +
                    "Content-Type: audio/mp4\r\n" +
                    "Accept-Ranges: bytes\r\n"

            if (contentLength > 0) {
                headers += "Content-Length: $contentLength\r\n"
            }
            val contentRange = connection.getHeaderField("Content-Range")
            if (contentRange != null) {
                headers += "Content-Range: $contentRange\r\n"
            }
            headers += "Connection: close\r\n\r\n"

            out.write(headers.toByteArray())
            out.flush()

            if (!isHead && responseCode in 200..299) {
                connection.inputStream.use { inStream ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        try {
                            out.write(buffer, 0, bytesRead)
                            out.flush()
                        } catch (e: Exception) {
                            // Client closed connection (normal when seeking or probing stream header)
                            break
                        }
                    }
                }
            }
            socket.close()
        } catch (e: Exception) {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    fun stop() {
        isStarted = false
        scope.cancel()
        try { serverSocket?.close() } catch (ignored: Exception) {}
    }
}
