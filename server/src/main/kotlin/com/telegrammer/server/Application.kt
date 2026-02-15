package com.telegrammer.server

import com.telegrammer.server.auth.*
import com.telegrammer.server.chat.ChatRepository
import com.telegrammer.server.chat.*
import com.telegrammer.server.chat.MessageRepository
import com.telegrammer.server.chat.chatRoutes
import com.telegrammer.server.contact.ContactRepository
import com.telegrammer.server.contact.contactRoutes
import com.telegrammer.server.keys.KeyRepository
import com.telegrammer.server.keys.keyRoutes
import com.telegrammer.server.user.UserRepository
import com.telegrammer.server.user.userRoutes
import com.telegrammer.server.ws.ConnectionManager
import com.telegrammer.server.ws.WsHandler
import com.telegrammer.server.ws.wsRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val config = loadConfig()

    // Initialize database
    Database.init(config.mongodb)

    // JSON config
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Services
    val jwtService = JwtService(config.jwt)
    val otpService = OtpService()
    val smsGateway: SmsGateway = ConsoleSmsGateway()
    val userRepository = UserRepository()
    val chatRepository = ChatRepository()
    val messageRepository = MessageRepository()
    val contactRepository = ContactRepository()
    val keyRepository = KeyRepository()
    val connectionManager = ConnectionManager()
    val wsHandler = WsHandler(connectionManager, chatRepository, messageRepository, json)

    // Plugins
    install(ContentNegotiation) {
        json(json)
    }

    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Bad request")))
        }
        exception<Exception> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Internal server error"))
        }
    }

    install(Authentication) {
        jwt {
            verifier(jwtService.verifier)
            validate { credential ->
                if (credential.payload.subject != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired token"))
            }
        }
    }

    // Create indexes on startup
    environment.monitor.subscribe(ApplicationStarted) {
        kotlinx.coroutines.runBlocking {
            Database.createIndexes()
        }
    }

    // Routes
    routing {
        authRoutes(otpService, smsGateway, jwtService, userRepository)
        userRoutes(userRepository)
        contactRoutes(contactRepository, userRepository)
        chatRoutes(chatRepository, messageRepository)
        keyRoutes(keyRepository)
        wsRoutes(jwtService, connectionManager, wsHandler, userRepository, json)

        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
    }
}

// Extension to extract userId from JWT principal
fun io.ktor.server.application.ApplicationCall.userId(): String =
    principal<JWTPrincipal>()!!.payload.subject
