package com.melodify.shared.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Uses yt-dlp (installed locally) to extract working stream URLs from YouTube.
 * 
 * This is necessary because YouTube's InnerTube API now requires signature cipher
 * decryption and n-parameter transformation that yt-dlp handles via JavaScript
 * runtime evaluation. Without this, the raw InnerTube API returns either
 * LOGIN_REQUIRED, UNPLAYABLE, or throttled URLs.
 */
object YtDlpStreamResolver {

    private var ytDlpPath: String? = null
    private var available: Boolean? = null

    /**
     * Check if yt-dlp is available on this system by trying multiple paths.
     */
    suspend fun isAvailable(): Boolean {
        available?.let { return it }
        return withContext(Dispatchers.IO) {
            val pathsToTry = listOf(
                "yt-dlp",
                "yt-dlp.exe",
                "C:\\Users\\satya\\AppData\\Local\\Programs\\Python\\Python313\\Scripts\\yt-dlp.exe",
                "C:\\Users\\satya\\AppData\\Local\\Programs\\Python\\Python312\\Scripts\\yt-dlp.exe",
                "C:\\Users\\satya\\AppData\\Local\\Programs\\Python\\Python311\\Scripts\\yt-dlp.exe",
                "C:\\Python313\\Scripts\\yt-dlp.exe"
            )

            for (path in pathsToTry) {
                try {
                    val process = ProcessBuilder(path, "--version")
                        .redirectErrorStream(true)
                        .start()
                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        ytDlpPath = path
                        available = true
                        println("Found yt-dlp at: $path")
                        return@withContext true
                    }
                } catch (e: Exception) {
                    // Ignore and try next path
                }
            }
            
            println("Failed to find yt-dlp on the system.")
            available = false
            false
        }
    }

    fun getYtDlpPath(): String? = ytDlpPath

    /**
     * Extract the best audio stream URL for a YouTube video using yt-dlp.
     * Returns null if extraction fails or yt-dlp is not available.
     *
     * @param videoId The YouTube video ID
     * @param preferM4a If true, prefer m4a format (AAC) for better compatibility with JavaFX Media
     */
    suspend fun getStreamUrl(videoId: String, preferM4a: Boolean = true): String? {
        if (!isAvailable()) return null

        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(15_000L) {
                try {
                    val formatSpec = if (preferM4a) {
                        "bestaudio[ext=m4a]/bestaudio"
                    } else {
                        "bestaudio"
                    }

                    val process = ProcessBuilder(
                        "python",
                        "-m", "yt_dlp",
                        "-f", formatSpec,
                        "--get-url",
                        "--no-warnings",
                        "--no-playlist",
                        "https://www.youtube.com/watch?v=$videoId"
                    )
                        .redirectErrorStream(false)
                        .start()

                    // Read stdout for the URL
                    val url = process.inputStream.bufferedReader().readLine()?.trim()

                    // Read stderr for errors (don't block)
                    val stderr = process.errorStream.bufferedReader().readText().trim()

                    val exitCode = process.waitFor()

                    if (exitCode == 0 && !url.isNullOrBlank() && url.startsWith("http")) {
                        println("yt-dlp resolved stream URL for $videoId (${url.take(80)}...)")
                        url
                    } else {
                        if (stderr.isNotBlank()) {
                            println("yt-dlp stderr for $videoId: $stderr")
                        }
                        null
                    }
                } catch (e: Exception) {
                    println("yt-dlp extraction failed for $videoId: ${e.message}")
                    null
                }
            }
        }
    }
}
