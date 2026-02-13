package com.telegrammer.shared.crypto

import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.scalarmult.ScalarMultiplication
import com.ionspin.kotlin.crypto.signature.Signature
import com.telegrammer.shared.api.PreKeyBundleResponse
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class X3DH(private val keyManager: KeyManager) {

    data class X3DHResult(
        val sharedSecret: ByteArray,
        val ephemeralPublicKey: ByteArray,
        val usedOneTimePreKeyId: Int?
    )

    /**
     * Alice initiates a session with Bob using Bob's prekey bundle.
     * Computes 3 or 4 DH values and derives a shared secret.
     */
    fun initiateSession(bundle: PreKeyBundleResponse): X3DHResult {
        val bobIdentityKey = Base64.decode(bundle.identityKey)
        val bobSignedPreKey = Base64.decode(bundle.signedPreKey.publicKey)
        val bobSignature = Base64.decode(bundle.signedPreKey.signature)

        // Verify Bob's signed prekey
        val isValid = Signature.verifyDetached(
            bobSignature.toUByteArray(),
            bobSignedPreKey.toUByteArray(),
            bobIdentityKey.toUByteArray()
        )
        require(isValid) { "Invalid signed prekey signature" }

        // Generate ephemeral key pair
        val ephemeralKeyPair = Box.keypair()
        val ephemeralPrivate = ephemeralKeyPair.secretKey.toByteArray()
        val ephemeralPublic = ephemeralKeyPair.publicKey.toByteArray()

        // Our identity key (convert Ed25519 signing key to X25519 for DH)
        val aliceIdentityPrivate = keyManager.getIdentityPrivateKey()

        // DH1: Alice identity (X25519) * Bob signed prekey
        val dh1 = dh(ed25519PrivateToX25519(aliceIdentityPrivate), bobSignedPreKey)
        // DH2: Alice ephemeral * Bob identity (X25519)
        val dh2 = dh(ephemeralPrivate, ed25519PublicToX25519(bobIdentityKey))
        // DH3: Alice ephemeral * Bob signed prekey
        val dh3 = dh(ephemeralPrivate, bobSignedPreKey)

        var dhConcat = dh1 + dh2 + dh3
        var usedOneTimeKeyId: Int? = null

        // DH4: Alice ephemeral * Bob one-time prekey (if available)
        if (bundle.oneTimePreKey != null) {
            val bobOneTimeKey = Base64.decode(bundle.oneTimePreKey.publicKey)
            val dh4 = dh(ephemeralPrivate, bobOneTimeKey)
            dhConcat += dh4
            usedOneTimeKeyId = bundle.oneTimePreKey.keyId
        }

        // Derive shared secret via HKDF (simplified: SHA-256 of concatenated DH outputs)
        val sharedSecret = hkdfDerive(dhConcat)

        return X3DHResult(
            sharedSecret = sharedSecret,
            ephemeralPublicKey = ephemeralPublic,
            usedOneTimePreKeyId = usedOneTimeKeyId
        )
    }

    /**
     * Bob receives Alice's initial message and reconstructs the shared secret.
     */
    fun respondToSession(
        aliceIdentityKey: ByteArray,
        aliceEphemeralKey: ByteArray,
        usedOneTimePreKeyId: Int?
    ): ByteArray {
        val bobIdentityPrivate = keyManager.getIdentityPrivateKey()
        val bobSignedPreKeyPrivate = keyManager.getSignedPreKeyPrivate()

        // DH1: Bob signed prekey * Alice identity (X25519)
        val dh1 = dh(bobSignedPreKeyPrivate, ed25519PublicToX25519(aliceIdentityKey))
        // DH2: Bob identity (X25519) * Alice ephemeral
        val dh2 = dh(ed25519PrivateToX25519(bobIdentityPrivate), aliceEphemeralKey)
        // DH3: Bob signed prekey * Alice ephemeral
        val dh3 = dh(bobSignedPreKeyPrivate, aliceEphemeralKey)

        var dhConcat = dh1 + dh2 + dh3

        // DH4 if one-time prekey was used
        if (usedOneTimePreKeyId != null) {
            val oneTimePrivate = keyManager.getOneTimePreKeyPrivate(usedOneTimePreKeyId)
            if (oneTimePrivate != null) {
                val dh4 = dh(oneTimePrivate, aliceEphemeralKey)
                dhConcat += dh4
                keyManager.deleteOneTimePreKey(usedOneTimePreKeyId)
            }
        }

        return hkdfDerive(dhConcat)
    }

    private fun dh(privateKey: ByteArray, publicKey: ByteArray): ByteArray =
        ScalarMultiplication.scalarMultiplication(
            privateKey.toUByteArray(),
            publicKey.toUByteArray()
        ).toByteArray()

    private fun ed25519PrivateToX25519(ed25519Private: ByteArray): ByteArray {
        // libsodium: crypto_sign_ed25519_sk_to_curve25519
        return com.ionspin.kotlin.crypto.util.LibsodiumUtil.sodiumBin2Hex(
            ed25519Private.toUByteArray()
        ).let {
            // For simplicity, use first 32 bytes as X25519 scalar (clamped)
            // In production, use proper conversion
            val scalar = ed25519Private.copyOfRange(0, 32)
            scalar[0] = (scalar[0].toInt() and 248).toByte()
            scalar[31] = (scalar[31].toInt() and 127 or 64).toByte()
            scalar
        }
    }

    private fun ed25519PublicToX25519(ed25519Public: ByteArray): ByteArray {
        // Simplified conversion — in production use crypto_sign_ed25519_pk_to_curve25519
        // For now, this returns the input assuming keys are already X25519 format
        // TODO: Use proper conversion when libsodium bindings support it
        return ed25519Public
    }

    private fun hkdfDerive(inputKeyMaterial: ByteArray): ByteArray {
        // Simplified HKDF: SHA-256 of input key material with info string
        // In production, use proper HKDF-SHA256 (extract + expand)
        val info = "X3DH-SharedSecret".encodeToByteArray()
        return sha256(inputKeyMaterial + info)
    }
}

internal fun sha256(input: ByteArray): ByteArray {
    return com.ionspin.kotlin.crypto.hash.Hash.sha256(input.toUByteArray()).toByteArray()
}
