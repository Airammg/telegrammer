package com.telegrammer.server.contact

import com.telegrammer.server.model.UserResponse
import com.telegrammer.server.user.UserRepository
import com.telegrammer.server.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class ResolveContactsRequest(val phoneNumbers: List<String>)

fun Route.contactRoutes(
    contactRepository: ContactRepository,
    userRepository: UserRepository
) {
    authenticate {
        route("/contacts") {
            post("/resolve") {
                val body = call.receive<ResolveContactsRequest>()
                val users = userRepository.findByPhoneNumbers(body.phoneNumbers)
                val currentUserId = ObjectId(call.userId())

                // Auto-add resolved users as contacts
                users.forEach { user ->
                    if (user.id != currentUserId) {
                        contactRepository.addContact(currentUserId, user.id)
                    }
                }

                call.respond(users.filter { it.id != currentUserId }.map { UserResponse.from(it) })
            }

            get {
                val currentUserId = ObjectId(call.userId())
                val contactIds = contactRepository.getContacts(currentUserId)
                val users = contactIds.mapNotNull { userRepository.findById(it) }
                call.respond(users.map { UserResponse.from(it) })
            }
        }
    }
}
