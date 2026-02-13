package com.telegrammer.server.user

import com.telegrammer.server.model.UpdateProfileRequest
import com.telegrammer.server.model.UserResponse
import com.telegrammer.server.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.types.ObjectId

fun Route.userRoutes(userRepository: UserRepository) {
    authenticate {
        route("/users") {
            get("/me") {
                val user = userRepository.findById(ObjectId(call.userId()))
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    return@get
                }
                call.respond(UserResponse.from(user))
            }

            put("/me") {
                val body = call.receive<UpdateProfileRequest>()
                val user = userRepository.updateProfile(
                    id = ObjectId(call.userId()),
                    displayName = body.displayName,
                    avatarUrl = body.avatarUrl
                )
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    return@put
                }
                call.respond(UserResponse.from(user))
            }

            get("/{id}") {
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
                    return@get
                }
                val user = userRepository.findById(ObjectId(id))
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                    return@get
                }
                call.respond(UserResponse.from(user))
            }
        }
    }
}
