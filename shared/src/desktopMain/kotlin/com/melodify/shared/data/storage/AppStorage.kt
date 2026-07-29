package com.melodify.shared.data.storage

import java.io.File

actual object AppStorage {
    actual fun getStorageDir(): File {
        val userHome = System.getProperty("user.home") ?: "."
        val dir = File(userHome, ".melodify")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
