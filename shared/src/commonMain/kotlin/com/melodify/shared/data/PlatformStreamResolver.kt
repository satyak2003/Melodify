package com.melodify.shared.data

/**
 * Platform-specific stream URL resolver.
 * On desktop, this uses yt-dlp for signature decryption.
 * On Android, this returns null (Android uses ExoPlayer which handles streams differently).
 */
expect suspend fun platformResolveStreamUrl(videoId: String): String?
