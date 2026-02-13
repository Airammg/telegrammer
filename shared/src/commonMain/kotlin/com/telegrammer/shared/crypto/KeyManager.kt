package com.telegrammer.shared.crypto

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.signature.Signature
import com.telegrammer.shared.api.OneTimePreKeyData
import com.telegrammer.shared.api.SignedPreKeyData
import com.telegrammer.shared.api.UploadBundleRequest
import com.telegrammer.shared.platform.SecureStorage
import com.telegrammer.shared.platform.StorageKeys
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalEncodingApi::class)
class KeyManager(private val storage: SecureStorage) {

    suspend fun initialize() {
        LibsodiumInitializer.initialize()
    }

    fun hasIdentityKey(): Boolean =
        storage.getString(StorageKeys.IDENTITY_KEY_PUBLIC) != null

    fun generateIdentityKeyPair() {
        val keyPair = Signature.keypair()
        storage.putString(StorageKeys.IDENTITY_KEY_PUBLIC, Base64.encode(keyPair.publicKey.toByteArray()))
        storage.putString(StorageKeys.IDENTITY_KEY_PRIVATE, Base64.encode(keyPair.secretKey.toByteArray()))
    }

    fun getIdentityPublicKey(): ByteArray =
        Base64.decode(storage.getString(StorageKeys.IDENTITY_KEY_PUBLIC)!!)

    fun getIdentityPrivateKey(): ByteArray =
        Base64.decode(storage.getString(StorageKeys.IDENTITY_KEY_PRIVATE)!!)

    fun generateSignedPreKey(): Pair<Int, ByteArray> {
        val keyPair = Box.keypair()
        val keyId = Random.nextInt(1, Int.MAX_VALUE)
        storage.putString(StorageKeys.SIGNED_PREKEY_PUBLIC, Base64.encode(keyPair.publicKey.toByteArray()))
        storage.putString(StorageKeys.SIGNED_PREKEY_PRIVATE, Base64.encode(keyPair.secretKey.toByteArray()))
        storage.putString(StorageKeys.SIGNED_PREKEY_ID, keyId.toString())
        return keyId to keyPair.publicKey.toByteArray()
    }

    fun getSignedPreKeyPublic(): ByteArray =
        Base64.decode(storage.getString(StorageKeys.SIGNED_PREKEY_PUBLIC)!!)

    fun getSignedPreKeyPrivate(): ByteArray =
        Base64.decode(storage.getString(StorageKeys.SIGNED_PREKEY_PRIVATE)!!)

    fun signData(data: ByteArray): ByteArray {
        val identityPrivate = getIdentityPrivateKey()
        return Signature.detached(data.toUByteArray(), identityPrivate.toUByteArray()).toByteArray()
    }

    data class OneTimeKeyPair(val keyId: Int, val publicKey: ByteArray, val privateKey: ByteArray)

    fun generateOneTimePreKeys(startId: Int, count: Int): List<OneTimeKeyPair> {
        val keys = mutableListOf<OneTimeKeyPair>()
        for (i in 0 until count) {
            val keyPair = Box.keypair()
            val keyId = startId + i
            // Store private keys for later session establishment
            storage.putString(
                "one_time_prekey_private_$keyId",
                Base64.encode(keyPair.secretKey.toByteArray())
            )
            keys.add(
                OneTimeKeyPair(
                    keyId = keyId,
                    publicKey = keyPair.publicKey.toByteArray(),
                    privateKey = keyPair.secretKey.toByteArray()
                )
            )
        }
        return keys
    }

    fun getOneTimePreKeyPrivate(keyId: Int): ByteArray? {
        val encoded = storage.getString("one_time_prekey_private_$keyId") ?: return null
        return Base64.decode(encoded)
    }

    fun deleteOneTimePreKey(keyId: Int) {
        storage.remove("one_time_prekey_private_$keyId")
    }

    fun generateUploadBundle(oneTimeKeyCount: Int = 100): UploadBundleRequest {
        if (!hasIdentityKey()) generateIdentityKeyPair()

        val (signedKeyId, signedKeyPublic) = generateSignedPreKey()
        val signature = signData(signedKeyPublic)
        val oneTimeKeys = generateOneTimePreKeys(1, oneTimeKeyCount)

        return UploadBundleRequest(
            identityKey = Base64.encode(getIdentityPublicKey()),
            signedPreKey = SignedPreKeyData(
                keyId = signedKeyId,
                publicKey = Base64.encode(signedKeyPublic),
                signature = Base64.encode(signature)
            ),
            oneTimePreKeys = oneTimeKeys.map {
                OneTimePreKeyData(keyId = it.keyId, publicKey = Base64.encode(it.publicKey))
            }
        )
    }
}
