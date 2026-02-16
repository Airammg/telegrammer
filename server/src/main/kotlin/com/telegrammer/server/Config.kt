package com.telegrammer.server

import io.ktor.server.application.*

data class AppConfig(
    val mongodb: MongoConfig,
    val jwt: JwtConfig,
    val sms: SmsConfig
)

data class MongoConfig(
    val connectionString: String,
    val database: String
)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessTokenExpireMinutes: Long,
    val refreshTokenExpireDays: Long
)

data class SmsConfig(
    val provider: String,
    val twilioAccountSid: String = "",
    val twilioAuthToken: String = "",
    val twilioFromNumber: String = ""
)

fun Application.loadConfig(): AppConfig {
    val config = environment.config
    return AppConfig(
        mongodb = MongoConfig(
            connectionString = config.property("app.mongodb.connectionString").getString(),
            database = config.property("app.mongodb.database").getString()
        ),
        jwt = JwtConfig(
            secret = config.property("app.jwt.secret").getString(),
            issuer = config.property("app.jwt.issuer").getString(),
            audience = config.property("app.jwt.audience").getString(),
            accessTokenExpireMinutes = config.property("app.jwt.accessTokenExpireMinutes").getString().toLong(),
            refreshTokenExpireDays = config.property("app.jwt.refreshTokenExpireDays").getString().toLong()
        ),
        sms = SmsConfig(
            provider = config.property("app.sms.provider").getString(),
            twilioAccountSid = config.propertyOrNull("app.sms.twilio.accountSid")?.getString() ?: "",
            twilioAuthToken = config.propertyOrNull("app.sms.twilio.authToken")?.getString() ?: "",
            twilioFromNumber = config.propertyOrNull("app.sms.twilio.fromNumber")?.getString() ?: ""
        )
    )
}
