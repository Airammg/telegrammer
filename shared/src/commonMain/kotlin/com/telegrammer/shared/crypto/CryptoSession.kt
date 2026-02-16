package com.telegrammer.shared.crypto

import com.telegrammer.shared.api.KeyApi
import com.telegrammer.shared.platform.SecureStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Manages per-conversation encryption sessions.
 * Handles X3DH session establishment and Double Ratchet message encryption/decryption.
 */
@OptIn(ExperimentalEncodingApi::class)
class CryptoSession(
    private val keyManager: KeyManager,
    private val keyApi: KeyApi,
    private val storage: SecureStorage
) {
    private val x3dh = X3DH(keyManager)
    private val json = Json { ignoreUnknownKeys = true }
    private val sessions = mutableMapOf<String, Ratchet.RatchetState>()

    @Serializable
    private data class SerializedRatchetState(
        val rootKey: String,
        val sendChainKey: String?,
        val recvChainKey: String?,
        val sendPrivateKey: String?,
        val sendPublicKey: String?,
        val recvRatchetPublicKey: String?,
        val sendMessageNumber: Int,
        val recvMessageNumber: Int
    )

    private fun saveSession(recipientId: String, state: Ratchet.RatchetState) {
        sessions[recipientId] = state
        val serialized = SerializedRatchetState(
            rootKey = Base64.encode(state.rootKey),
            sendChainKey = state.sendChainKey?.let { Base64.encode(it) },
            recvChainKey = state.recvChainKey?.let { Base64.encode(it) },
            sendPrivateKey = state.sendRatchetKeyPair?.first?.let { Base64.encode(it) },
            sendPublicKey = state.sendRatchetKeyPair?.second?.let { Base64.encode(it) },
            recvRatchetPublicKey = state.recvRatchetPublicKey?.let { Base64.encode(it) },
            sendMessageNumber = state.sendMessageNumber,
            recvMessageNumber = state.recvMessageNumber
        )
        storage.putString("ratchet_session_$recipientId", json.encodeToString(serialized))
    }

    private fun loadSession(recipientId: String): Ratchet.RatchetState? {
        sessions[recipientId]?.let { return it }
        val data = storage.getString("ratchet_session_$recipientId") ?: return null
        val s = json.decodeFromString<SerializedRatchetState>(data)
        val state = Ratchet.RatchetState(
            rootKey = Base64.decode(s.rootKey),
            sendChainKey = s.sendChainKey?.let { Base64.decode(it) },
            recvChainKey = s.recvChainKey?.let { Base64.decode(it) },
            sendRatchetKeyPair = if (s.sendPrivateKey != null && s.sendPublicKey != null) {
                Base64.decode(s.sendPrivateKey) to Base64.decode(s.sendPublicKey)
            } else null,
            recvRatchetPublicKey = s.recvRatchetPublicKey?.let { Base64.decode(it) },
            sendMessageNumber = s.sendMessageNumber,
            recvMessageNumber = s.recvMessageNumber
        )
        sessions[recipientId] = state
        return state
    }

    /**
     * Encrypt a plaintext message for a recipient.
     * Establishes a new X3DH session if one doesn't exist.
     */
    suspend fun encrypt(recipientId: String, plaintext: String): Pair<String, String> {
        var state = loadSession(recipientId)
        var x3dhHeader: Ratchet.X3DHHeader? = null

        if (state == null) {
            // Initiate X3DH session
            val bundle = keyApi.fetchBundle(recipientId).getOrThrow()
            val x3dhResult = x3dh.initiateSession(bundle)
            val bobSignedPreKey = Base64.decode(bundle.signedPreKey.publicKey)
            state = Ratchet.initAlice(x3dhResult.sharedSecret, bobSignedPreKey)

            // Include X3DH metadata so Bob can derive the same shared secret
            x3dhHeader = Ratchet.X3DHHeader(
                identityKey = Base64.encode(keyManager.getIdentityPublicKey()),
                ephemeralKey = Base64.encode(x3dhResult.ephemeralPublicKey),
                oneTimePreKeyId = x3dhResult.usedOneTimePreKeyId
            )
        }

        val encrypted = Ratchet.encrypt(state, plaintext.encodeToByteArray())

        // Attach X3DH header to the first message only
        val header = if (x3dhHeader != null) {
            encrypted.header.copy(x3dh = x3dhHeader)
        } else {
            encrypted.header
        }

        saveSession(recipientId, state)

        // Combine header + ciphertext into ciphertext field, nonce goes to iv
        val headerJson = json.encodeToString(header)
        val combined = Base64.encode(headerJson.encodeToByteArray()) + "." + encrypted.ciphertext

        return combined to encrypted.nonce
    }

    /**
     * Decrypt a received message from a sender.
     */
    fun decrypt(senderId: String, ciphertext: String, iv: String): String {
        val parts = ciphertext.split(".")
        require(parts.size == 2) { "Invalid ciphertext format" }
        val headerJson = Base64.decode(parts[0]).decodeToString()
        val header = json.decodeFromString<Ratchet.MessageHeader>(headerJson)

        var state = loadSession(senderId)

        if (state == null) {
            // Bob receiving first message — use X3DH metadata from header to derive shared secret
            val x3dhMeta = header.x3dh
                ?: throw IllegalStateException("First message from $senderId missing X3DH header")

            val aliceIdentityKey = Base64.decode(x3dhMeta.identityKey)
            val aliceEphemeralKey = Base64.decode(x3dhMeta.ephemeralKey)
            val sharedSecret = x3dh.respondToSession(aliceIdentityKey, aliceEphemeralKey, x3dhMeta.oneTimePreKeyId)

            val bobSignedPrivate = keyManager.getSignedPreKeyPrivate()
            val bobSignedPublic = keyManager.getSignedPreKeyPublic()
            state = Ratchet.initBob(
                sharedSecret = sharedSecret,
                bobRatchetKeyPair = bobSignedPrivate to bobSignedPublic
            )
        }

        val encrypted = Ratchet.EncryptedMessage(
            header = header,
            ciphertext = parts[1],
            nonce = iv
        )
        val plaintext = Ratchet.decrypt(state, encrypted)
        saveSession(senderId, state)

        return plaintext.decodeToString()
    }
}
