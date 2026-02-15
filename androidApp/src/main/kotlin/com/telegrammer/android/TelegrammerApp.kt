package com.telegrammer.android

import android.app.Application
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class TelegrammerApp : Application() {
    lateinit var dependencies: AppDependencies
        private set

    override fun onCreate() {
        super.onCreate()
        dependencies = AppDependencies(this)
        MainScope().launch {
            dependencies.keyManager.initialize()
            // Upload keys if logged in but bundle not yet uploaded
            if (dependencies.authRepo.isLoggedIn() && !dependencies.keyManager.hasIdentityKey()) {
                try {
                    val bundle = dependencies.keyManager.generateUploadBundle()
                    dependencies.keyApi.uploadBundle(bundle)
                } catch (_: Exception) {
                    // Will retry on next launch
                }
            }
        }
    }
}
