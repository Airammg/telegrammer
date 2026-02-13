package com.telegrammer.server.auth

import org.slf4j.LoggerFactory

interface SmsGateway {
    suspend fun sendOtp(phoneNumber: String, code: String)
}

class ConsoleSmsGateway : SmsGateway {
    private val log = LoggerFactory.getLogger(ConsoleSmsGateway::class.java)

    override suspend fun sendOtp(phoneNumber: String, code: String) {
        log.info("========================================")
        log.info("  OTP for $phoneNumber: $code")
        log.info("========================================")
    }
}
