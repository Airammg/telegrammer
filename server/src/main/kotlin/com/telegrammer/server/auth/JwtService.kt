package com.telegrammer.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.telegrammer.server.JwtConfig
import java.util.*

class JwtService(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    val verifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun generateAccessToken(userId: String): String =
        JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId)
            .withClaim("type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() + config.accessTokenExpireMinutes * 60 * 1000))
            .sign(algorithm)

    fun generateRefreshToken(userId: String): String =
        JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + config.refreshTokenExpireDays * 24 * 60 * 60 * 1000))
            .sign(algorithm)

    fun validateRefreshToken(token: String): DecodedJWT? =
        try {
            val decoded = verifier.verify(token)
            if (decoded.getClaim("type").asString() == "refresh") decoded else null
        } catch (e: Exception) {
            null
        }
}
