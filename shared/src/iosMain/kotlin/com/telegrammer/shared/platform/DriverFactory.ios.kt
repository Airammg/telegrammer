package com.telegrammer.shared.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.telegrammer.shared.db.TelegrammerDatabase

actual class DriverFactory {
    actual fun create(): SqlDriver =
        NativeSqliteDriver(TelegrammerDatabase.Schema, "telegrammer.db")
}
