package com.melodify.shared.data

/**
 * Desktop implementation: uses yt-dlp to extract working stream URLs.
 * yt-dlp handles YouTube's signature cipher decryption and n-parameter
 * transformation which the raw InnerTube API can no longer do anonymously.
 */
actual suspend fun platformResolveStreamUrl(videoId: String): String? {
    return YtDlpStreamResolver.getStreamUrl(videoId, preferM4a = true)
}
