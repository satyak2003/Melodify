package com.melodify.shared.data.storage

import java.io.File

expect object AppStorage {
    fun getStorageDir(): File
}
