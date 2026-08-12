package com.melodify.shared.crypto

import java.security.MessageDigest

actual fun sha1(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-1").digest(input)
