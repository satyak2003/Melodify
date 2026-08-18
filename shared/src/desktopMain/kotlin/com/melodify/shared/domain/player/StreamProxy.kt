package com.melodify.shared.domain.player

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object StreamProxy {
    private const val PROXY_HOST = "127.0.0.1"
    private const val BUFFER_SIZE = 32768
    private const val CONNECT_TIMEOUT_MS = 15_000L
    private const val REQUEST_TIMEOUT_MS = 30_000L
    private const val MAX_CONCURRENT_STREAMS = 10

    private data class ProxyRequest(
        val method: String,
        val targetUrl: String,
        val rangeHeader: String?,
        val isHead: Boolean
    )

    private var serverSocket: ServerSocket? = null
    private var proxyPort = 0
    private var isStarted = false
    private var acceptThread: Thread? = null
    private val activeConnections = AtomicInteger(0)
    private val shutdown = AtomicBoolean(false)

    // HTTP Client for upstream connections with connection pooling
    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
        engine {
            maxConnectionsCount = MAX_CONCURRENT_STREAMS
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _stats = MutableStateFlow(StreamProxyStats(0, 0, 0))
    val stats: StateFlow<StreamProxyStats> = _stats.asStateFlow()

    data class StreamProxyStats(
        val activeStreams: Int,
        val totalBytesProxied: Long,
        val totalRequests: Int
    )

    fun start(): Int {
        if (isRunning()) return proxyPort

        shutdown.set(false)
        activeConnections.set(0)

        try {
            val server = ServerSocket()
            server.reuseAddress = true
            server.bind(InetSocketAddress(PROXY_HOST, 0))
            serverSocket = server
            proxyPort = server.localPort
            isStarted = true

            acceptThread = Thread { acceptLoop(server) }.apply {
                isDaemon = true
                name = "StreamProxy-Accept"
                start()
            }

            println("StreamProxy started on port $proxyPort")
        } catch (e: Exception) {
            println("StreamProxy start error: ${e.message}")
            isStarted = false
        }
        return proxyPort
    }

    fun getProxyUrl(targetUrl: String): String {
        val port = start()
        val encoded = URLEncoder.encode(targetUrl, "UTF-8")
        return "http://$PROXY_HOST:$port/stream.m4a?url=$encoded"
    }

    private fun isRunning(): Boolean {
        val socket = serverSocket
        return isStarted && socket != null && !socket.isClosed && proxyPort > 0
    }

    private fun acceptLoop(server: ServerSocket) {
        while (!shutdown.get() && !server.isClosed) {
            try {
                val client = server.accept()
                if (activeConnections.get() >= MAX_CONCURRENT_STREAMS) {
                    try { client.close() } catch (ignored: Exception) {}
                    continue
                }

                activeConnections.incrementAndGet()
                scope.launch { handleClient(client) }
            } catch (e: Exception) {
                if (!shutdown.get() && !server.isClosed) {
                    println("Accept error: ${e.message}")
                }
            }
        }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            // Read request
            val request = readRequest(socket) ?: return@withContext

            // Parse target URL from query
            val urlQuery = request.targetUrl.substringAfter("url=", "")
            if (urlQuery.isEmpty()) return@withContext

            val decodedUrl = URLDecoder.decode(urlQuery, "UTF-8")

            if (decodedUrl.startsWith("yt-dlp://")) {
                val videoId = decodedUrl.substringAfter("yt-dlp://")
                val ytDlpPath = com.melodify.shared.data.YtDlpStreamResolver.getYtDlpPath() ?: "yt-dlp"
                
                val process = ProcessBuilder(
                    ytDlpPath, 
                    "-f", "bestaudio[ext=m4a]/bestaudio", 
                    "-o", "-", 
                    "--no-warnings", 
                    "--no-playlist", 
                    "https://www.youtube.com/watch?v=$videoId"
                )
                    .redirectErrorStream(false)
                    .start()

                val responseStatus = "HTTP/1.1 200 OK\r\n"
                val headers = buildString {
                    append(responseStatus)
                    append("Content-Type: audio/mp4\r\n")
                    append("Accept-Ranges: none\r\n")
                    append("Connection: close\r\n")
                    append("\r\n")
                }

                val output = BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE)
                output.write(headers.toByteArray(Charsets.UTF_8))
                output.flush()

                if (!request.isHead) {
                    var totalBytes = 0L
                    val input = process.inputStream
                    val buffer = ByteArray(BUFFER_SIZE)
                    try {
                        while (true) {
                            if (shutdown.get()) {
                                process.destroy()
                                break
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            if (read == 0) continue
                            output.write(buffer, 0, read)
                            totalBytes += read
                        }
                        output.flush()
                    } finally {
                        process.destroy()
                    }
                    _stats.update {
                        it.copy(
                            totalBytesProxied = it.totalBytesProxied + totalBytes,
                            totalRequests = it.totalRequests + 1
                        )
                    }
                }
            } else {
                // Forward request upstream
                val upstreamResponse = httpClient.get(decodedUrl) {
                    header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    header(HttpHeaders.Accept, "*/*")
                    request.rangeHeader?.let { header(HttpHeaders.Range, it) }
                }

                val statusCode = upstreamResponse.status
                val contentLength = upstreamResponse.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                val contentType = upstreamResponse.headers[HttpHeaders.ContentType] ?: "audio/mp4"
                val contentRange = upstreamResponse.headers[HttpHeaders.ContentRange]
                val acceptRanges = upstreamResponse.headers[HttpHeaders.AcceptRanges] ?: "bytes"

                // Send response headers
                val responseStatus = when {
                    statusCode == HttpStatusCode.PartialContent -> "HTTP/1.1 206 Partial Content\r\n"
                    statusCode == HttpStatusCode.OK -> "HTTP/1.1 200 OK\r\n"
                    else -> "HTTP/1.1 ${statusCode.value} ${statusCode.description}\r\n"
                }

                val headers = buildString {
                    append(responseStatus)
                    append("Content-Type: $contentType\r\n")
                    append("Accept-Ranges: $acceptRanges\r\n")
                    contentLength?.let { append("Content-Length: $it\r\n") }
                    contentRange?.let { append("Content-Range: $it\r\n") }
                    append("Connection: close\r\n")
                    append("\r\n")
                }

                val output = BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE)
                output.write(headers.toByteArray(Charsets.UTF_8))
                output.flush()

                if (!request.isHead && statusCode.isSuccess()) {
                    var totalBytes = 0L
                    val channel = upstreamResponse.bodyAsChannel()
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        if (shutdown.get()) break
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read == -1) break
                        if (read == 0) continue
                        output.write(buffer, 0, read)
                        totalBytes += read
                    }
                    output.flush()
                    _stats.update {
                        it.copy(
                            totalBytesProxied = it.totalBytesProxied + totalBytes,
                            totalRequests = it.totalRequests + 1
                        )
                    }
                }
            }

        } catch (e: Exception) {
            if (e !is CancellationException && e !is IOException) {
                println("StreamProxy client error: ${e.message}")
            }
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
            activeConnections.decrementAndGet()
            _stats.update { it.copy(activeStreams = activeConnections.get()) }
        }
    }

    private fun readRequest(socket: Socket): ProxyRequest? {
        val input = BufferedInputStream(socket.getInputStream(), BUFFER_SIZE)
        val buffer = ByteArray(BUFFER_SIZE)
        val sb = StringBuilder()

        while (true) {
            val n = input.read(buffer)
            if (n <= 0) return null
            sb.append(String(buffer, 0, n, Charsets.US_ASCII))

            val endIdx = sb.indexOf("\r\n\r\n")
            if (endIdx >= 0) {
                val head = sb.substring(0, endIdx)
                val lines = head.split("\r\n")
                if (lines.isEmpty()) return null

                val requestLine = lines[0].split(" ")
                if (requestLine.size < 2) return null

                val method = requestLine[0].uppercase()
                var rangeHeader: String? = null
                lines.forEach { line ->
                    if (line.startsWith("Range:", ignoreCase = true)) {
                        rangeHeader = line.substringAfter("Range:").trim()
                    }
                }

                return ProxyRequest(method, requestLine[1], rangeHeader, method == "HEAD")
            }
        }
    }

    fun stop() {
        shutdown.set(true)
        try { serverSocket?.close() } catch (ignored: Exception) {}
        scope.cancel()
        try { httpClient.close() } catch (ignored: Exception) {}
        isStarted = false
        proxyPort = 0
        println("StreamProxy stopped")
    }
}