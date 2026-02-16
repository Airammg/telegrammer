package com.telegrammer.server.auth

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
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

class TwilioSmsGateway(
    accountSid: String,
    authToken: String,
    private val fromNumber: String
) : SmsGateway {
    private val log = LoggerFactory.getLogger(TwilioSmsGateway::class.java)

    init {
        Twilio.init(accountSid, authToken)
        log.info("Twilio SMS gateway initialized (from: $fromNumber)")
    }

    override suspend fun sendOtp(phoneNumber: String, code: String) {
        try {
            Message.creator(
                PhoneNumber(phoneNumber),
                PhoneNumber(fromNumber),
                "Your Telegrammer verification code is: $code"
            ).create()
            log.info("OTP sent via Twilio to $phoneNumber")
        } catch (e: Exception) {
            log.error("Failed to send OTP via Twilio to $phoneNumber", e)
            throw e
        }
    }
}
