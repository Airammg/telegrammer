package com.telegrammer.server.auth

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.telegrammer.server.Database
import com.telegrammer.server.model.OtpCode
import kotlinx.coroutines.flow.firstOrNull
import java.security.MessageDigest
import java.security.SecureRandom

class OtpService {
    private val collection = Database.getDatabase().getCollection<OtpCode>("otp_codes")
    private val random = SecureRandom()

    fun generateCode(): String {
        val code = random.nextInt(900_000) + 100_000
        return code.toString()
    }

    fun hashCode(code: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(code.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    suspend fun storeOtp(phoneNumber: String, code: String) {
        // Delete any existing OTP for this phone
        collection.deleteMany(Filters.eq("phoneNumber", phoneNumber))
        // Store new hashed OTP
        collection.insertOne(
            OtpCode(
                phoneNumber = phoneNumber,
                codeHash = hashCode(code)
            )
        )
    }

    suspend fun verifyOtp(phoneNumber: String, code: String): Boolean {
        val otp = collection.find(Filters.eq("phoneNumber", phoneNumber))
            .firstOrNull() ?: return false

        if (otp.attempts >= 5) {
            collection.deleteMany(Filters.eq("phoneNumber", phoneNumber))
            return false
        }

        if (System.currentTimeMillis() > otp.expiresAt) {
            collection.deleteMany(Filters.eq("phoneNumber", phoneNumber))
            return false
        }

        val codeHash = hashCode(code)
        if (codeHash != otp.codeHash) {
            collection.updateOne(
                Filters.eq("phoneNumber", phoneNumber),
                Updates.inc("attempts", 1)
            )
            return false
        }

        // Valid — clean up
        collection.deleteMany(Filters.eq("phoneNumber", phoneNumber))
        return true
    }
}
