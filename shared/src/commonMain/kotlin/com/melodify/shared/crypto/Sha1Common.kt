package com.melodify.shared.crypto

expect fun sha1(input: ByteArray): ByteArray

@OptIn(ExperimentalStdlibApi::class)
fun sha1Hex(input: String): String {
    val bytes = sha1(input.encodeToByteArray())
    return bytes.toHexString()
}
