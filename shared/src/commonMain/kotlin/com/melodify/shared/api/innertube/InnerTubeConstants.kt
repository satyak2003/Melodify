package com.melodify.shared.api.innertube

object InnerTubeConstants {
    const val BASE_URL = "https://music.youtube.com/youtubei/v1/"
    const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-KLET5YdCE"

    val WEB_REMIX_CLIENT = mapOf(
        "clientName" to "WEB_REMIX",
        "clientVersion" to "1.20241121.01.00",
        "gl" to "US",
        "hl" to "en"
    )

    val ANDROID_VR_CLIENT = mapOf(
        "clientName" to "ANDROID_VR",
        "clientVersion" to "1.56.21",
        "androidSdkVersion" to 32,
        "gl" to "US",
        "hl" to "en"
    )

    val ANDROID_MUSIC_CLIENT = mapOf(
        "clientName" to "ANDROID_MUSIC",
        "clientVersion" to "6.47.52",
        "androidSdkVersion" to 32,
        "gl" to "US",
        "hl" to "en"
    )

    const val ITAG_AAC_256 = 141
    const val ITAG_OPUS_160 = 251
    const val ITAG_AAC_128 = 140
    const val ITAG_OPUS_70 = 250

    val ITAG_QUALITY_MAP = mapOf(
        ITAG_AAC_256 to "AAC 256kbps",
        ITAG_OPUS_160 to "Opus 160kbps",
        ITAG_AAC_128 to "AAC 128kbps",
        ITAG_OPUS_70 to "Opus 70kbps"
    )
}
