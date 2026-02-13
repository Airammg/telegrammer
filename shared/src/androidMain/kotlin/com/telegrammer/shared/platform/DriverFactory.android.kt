package com.telegrammer.shared.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.telegrammer.shared.db.TelegrammerDatabase

actual class DriverFactory(private val context: Context) {
    actual fun create(): SqlDriver =
        AndroidSqliteDriver(TelegrammerDatabase.Schema, context, "telegrammer.db")
}
