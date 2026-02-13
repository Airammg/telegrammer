package com.telegrammer.shared.platform

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun create(): SqlDriver
}
