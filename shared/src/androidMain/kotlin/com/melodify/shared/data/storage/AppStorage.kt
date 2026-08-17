package com.melodify.shared.data.storage

import android.content.Context
import java.io.File

actual object AppStorage {
    internal var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    actual fun getStorageDir(): File {
        val baseDir = applicationContext?.filesDir
            ?: File(System.getProperty("java.io.tmpdir") ?: ".", "melodify_fallback")
        val dir = File(baseDir, ".melodify")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    actual fun getDownloadsDir(): File {
        val baseDir = applicationContext?.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
            ?: android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
        val dir = File(baseDir, "Melodify/downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
