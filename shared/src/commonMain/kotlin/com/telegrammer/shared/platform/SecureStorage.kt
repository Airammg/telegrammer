package com.telegrammer.shared.platform

expect class SecureStorage {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
    fun clear()
}

// Standard keys
object StorageKeys {
    const val ACCESS_TOKEN = "access_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val USER_ID = "user_id"
    const val IDENTITY_KEY_PUBLIC = "identity_key_public"
    const val IDENTITY_KEY_PRIVATE = "identity_key_private"
    const val SIGNED_PREKEY_PUBLIC = "signed_prekey_public"
    const val SIGNED_PREKEY_PRIVATE = "signed_prekey_private"
    const val SIGNED_PREKEY_ID = "signed_prekey_id"
}
