package com.telegrammer.server.auth

import com.telegrammer.server.user.UserRepository
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class RequestOtpBody(val phoneNumber: String)

@Serializable
data class VerifyOtpBody(val phoneNumber: String, val code: String)

@Serializable
data class RefreshBody(val refreshToken: String)

@Serializable
data class AuthTokens(val accessToken: String, val refreshToken: String, val userId: String)

fun Route.authRoutes(
    otpService: OtpService,
    smsGateway: SmsGateway,
    jwtService: JwtService,
    userRepository: UserRepository
) {
    route("/auth") {
        post("/request-otp") {
            val body = call.receive<RequestOtpBody>()
            if (!body.phoneNumber.matches(Regex("^\\+[1-9]\\d{6,14}$"))) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid phone number format"))
                return@post
            }
            val code = otpService.generateCode()
            otpService.storeOtp(body.phoneNumber, code)
            smsGateway.sendOtp(body.phoneNumber, code)
            call.respond(HttpStatusCode.OK, mapOf("message" to "OTP sent"))
        }

        post("/verify-otp") {
            val body = call.receive<VerifyOtpBody>()
            if (!otpService.verifyOtp(body.phoneNumber, body.code)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired OTP"))
                return@post
            }
            val user = userRepository.findOrCreateByPhone(body.phoneNumber)
            val userId = user.id.toHexString()
            call.respond(
                AuthTokens(
                    accessToken = jwtService.generateAccessToken(userId),
                    refreshToken = jwtService.generateRefreshToken(userId),
                    userId = userId
                )
            )
        }

        post("/refresh") {
            val body = call.receive<RefreshBody>()
            val decoded = jwtService.validateRefreshToken(body.refreshToken)
            if (decoded == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid refresh token"))
                return@post
            }
            val userId = decoded.subject
            call.respond(
                AuthTokens(
                    accessToken = jwtService.generateAccessToken(userId),
                    refreshToken = jwtService.generateRefreshToken(userId),
                    userId = userId
                )
            )
        }
    }
}
