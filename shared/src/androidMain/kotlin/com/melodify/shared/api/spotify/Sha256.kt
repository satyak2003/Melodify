package com.melodify.shared.api.spotify

import java.security.MessageDigest

actual fun sha256(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(input)
