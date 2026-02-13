package com.telegrammer.android

import android.app.Application

class TelegrammerApp : Application() {
    lateinit var dependencies: AppDependencies
        private set

    override fun onCreate() {
        super.onCreate()
        dependencies = AppDependencies(this)
    }
}
