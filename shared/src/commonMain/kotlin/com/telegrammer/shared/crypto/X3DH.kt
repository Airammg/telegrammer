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

        // Verify Bob's signed prekey (throws InvalidSignatureException on failure)
        try {
            Signature.verifyDetached(
                bobSignature.toUByteArray(),
                bobSignedPreKey.toUByteArray(),
                bobIdentityKey.toUByteArray()
            )
        } catch (e: Exception) {
            throw IllegalStateException("Invalid signed prekey signature", e)
        }

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

    private fun ed25519PrivateToX25519(ed25519Private: ByteArray): ByteArray =
        Signature.ed25519SkToCurve25519(ed25519Private.toUByteArray()).toByteArray()

    private fun ed25519PublicToX25519(ed25519Public: ByteArray): ByteArray =
        Signature.ed25519PkToCurve25519(ed25519Public.toUByteArray()).toByteArray()

    private fun hkdfDerive(inputKeyMaterial: ByteArray): ByteArray {
        val info = "X3DH-SharedSecret".encodeToByteArray()
        return sha256(inputKeyMaterial + info)
    }
}

internal fun sha256(input: ByteArray): ByteArray {
    return com.ionspin.kotlin.crypto.hash.Hash.sha256(input.toUByteArray()).toByteArray()
}
