package com.melodify.shared.data

/**
 * Android implementation: returns null because Android uses ExoPlayer
 * which handles YouTube streams through a different mechanism.
 */
actual suspend fun platformResolveStreamUrl(videoId: String): String? {
    return null
}
