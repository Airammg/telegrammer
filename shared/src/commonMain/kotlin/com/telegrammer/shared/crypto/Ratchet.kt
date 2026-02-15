package com.telegrammer.shared.crypto

import com.ionspin.kotlin.crypto.aead.AuthenticatedEncryptionWithAssociatedData
import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.scalarmult.ScalarMultiplication
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Double Ratchet implementation for forward-secret message encryption.
 * Each message uses a unique key derived from the chain key.
 * DH ratchet steps happen when the direction of conversation changes.
 */
@OptIn(ExperimentalEncodingApi::class)
class Ratchet {

    data class RatchetState(
        var rootKey: ByteArray,
        var sendChainKey: ByteArray?,
        var recvChainKey: ByteArray?,
        var sendRatchetKeyPair: Pair<ByteArray, ByteArray>?, // (private, public)
        var recvRatchetPublicKey: ByteArray?,
        var sendMessageNumber: Int = 0,
        var recvMessageNumber: Int = 0
    )

    @Serializable
    data class MessageHeader(
        val ratchetPublicKey: String, // Base64
        val messageNumber: Int,
        val previousChainLength: Int
    )

    data class EncryptedMessage(
        val header: MessageHeader,
        val ciphertext: String, // Base64
        val nonce: String       // Base64
    )

    companion object {
        private const val NONCE_SIZE = 24 // XChaCha20-Poly1305

        /**
         * Initialize ratchet as the initiator (Alice).
         * Alice has the shared secret and Bob's signed prekey as the first ratchet key.
         */
        fun initAlice(sharedSecret: ByteArray, bobRatchetPublicKey: ByteArray): RatchetState {
            val sendKeyPair = Box.keypair()
            val dhOutput = dh(sendKeyPair.secretKey.toByteArray(), bobRatchetPublicKey)
            val (newRootKey, sendChainKey) = kdfRk(sharedSecret, dhOutput)

            return RatchetState(
                rootKey = newRootKey,
                sendChainKey = sendChainKey,
                recvChainKey = null,
                sendRatchetKeyPair = sendKeyPair.secretKey.toByteArray() to sendKeyPair.publicKey.toByteArray(),
                recvRatchetPublicKey = bobRatchetPublicKey
            )
        }

        /**
         * Initialize ratchet as the responder (Bob).
         * Bob uses his signed prekey as the initial ratchet key pair.
         */
        fun initBob(sharedSecret: ByteArray, bobRatchetKeyPair: Pair<ByteArray, ByteArray>): RatchetState {
            return RatchetState(
                rootKey = sharedSecret,
                sendChainKey = null,
                recvChainKey = null,
                sendRatchetKeyPair = bobRatchetKeyPair,
                recvRatchetPublicKey = null
            )
        }

        fun encrypt(state: RatchetState, plaintext: ByteArray): EncryptedMessage {
            val (chainKey, messageKey) = kdfCk(state.sendChainKey!!)
            state.sendChainKey = chainKey

            val nonce = com.ionspin.kotlin.crypto.util.LibsodiumRandom.bufDeterministic(
                NONCE_SIZE,
                messageKey.copyOfRange(0, 32).toUByteArray()
            ).toByteArray()

            val ciphertext = AuthenticatedEncryptionWithAssociatedData.xChaCha20Poly1305IetfEncrypt(
                plaintext.toUByteArray(),
                "".encodeToByteArray().toUByteArray(),
                nonce.toUByteArray(),
                messageKey.toUByteArray()
            ).toByteArray()

            val header = MessageHeader(
                ratchetPublicKey = Base64.encode(state.sendRatchetKeyPair!!.second),
                messageNumber = state.sendMessageNumber,
                previousChainLength = 0
            )
            state.sendMessageNumber++

            return EncryptedMessage(
                header = header,
                ciphertext = Base64.encode(ciphertext),
                nonce = Base64.encode(nonce)
            )
        }

        fun decrypt(state: RatchetState, message: EncryptedMessage): ByteArray {
            val theirRatchetKey = Base64.decode(message.header.ratchetPublicKey)

            // If new ratchet key from sender, perform DH ratchet step
            if (state.recvRatchetPublicKey == null || !theirRatchetKey.contentEquals(state.recvRatchetPublicKey!!)) {
                dhRatchetStep(state, theirRatchetKey)
            }

            val (chainKey, messageKey) = kdfCk(state.recvChainKey!!)
            state.recvChainKey = chainKey
            state.recvMessageNumber++

            val nonce = Base64.decode(message.nonce)
            val ciphertext = Base64.decode(message.ciphertext)

            return AuthenticatedEncryptionWithAssociatedData.xChaCha20Poly1305IetfDecrypt(
                ciphertext.toUByteArray(),
                "".encodeToByteArray().toUByteArray(),
                nonce.toUByteArray(),
                messageKey.toUByteArray()
            ).toByteArray()
        }

        private fun dhRatchetStep(state: RatchetState, theirRatchetKey: ByteArray) {
            state.recvRatchetPublicKey = theirRatchetKey

            // Derive receiving chain
            val dhRecv = dh(state.sendRatchetKeyPair!!.first, theirRatchetKey)
            val (rootKey1, recvChainKey) = kdfRk(state.rootKey, dhRecv)
            state.rootKey = rootKey1
            state.recvChainKey = recvChainKey

            // Generate new sending ratchet key pair
            val newKeyPair = Box.keypair()
            state.sendRatchetKeyPair = newKeyPair.secretKey.toByteArray() to newKeyPair.publicKey.toByteArray()

            // Derive sending chain
            val dhSend = dh(newKeyPair.secretKey.toByteArray(), theirRatchetKey)
            val (rootKey2, sendChainKey) = kdfRk(state.rootKey, dhSend)
            state.rootKey = rootKey2
            state.sendChainKey = sendChainKey
            state.sendMessageNumber = 0
        }

        private fun dh(privateKey: ByteArray, publicKey: ByteArray): ByteArray =
            ScalarMultiplication.scalarMultiplication(
                privateKey.toUByteArray(),
                publicKey.toUByteArray()
            ).toByteArray()

        /** KDF for root key: (rootKey, dhOutput) -> (newRootKey, chainKey) */
        private fun kdfRk(rootKey: ByteArray, dhOutput: ByteArray): Pair<ByteArray, ByteArray> {
            val input = rootKey + dhOutput
            val derived = sha256(input + "RootKeyDerivation".encodeToByteArray())
            return derived.copyOfRange(0, 32) to sha256(derived + "ChainKey".encodeToByteArray()).copyOfRange(0, 32)
        }

        /** KDF for chain key: chainKey -> (newChainKey, messageKey) */
        private fun kdfCk(chainKey: ByteArray): Pair<ByteArray, ByteArray> {
            val newChainKey = sha256(chainKey + byteArrayOf(0x01))
            val messageKey = sha256(chainKey + byteArrayOf(0x02)).copyOfRange(0, 32)
            return newChainKey to messageKey
        }
    }
}
