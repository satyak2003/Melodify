package com.melodify.shared.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.melodify.db.MelodifyDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbDir = File(System.getProperty("user.home"), ".melodify")
        dbDir.mkdirs()
        val dbFile = File(dbDir, "melodify.db")
        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        MelodifyDatabase.Schema.create(driver)
        return driver
    }
}
