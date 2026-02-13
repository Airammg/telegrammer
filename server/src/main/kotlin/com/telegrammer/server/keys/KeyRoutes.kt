package com.telegrammer.server.keys

import com.telegrammer.server.model.PreKeyBundle
import com.telegrammer.server.model.PreKeyBundleRequest
import com.telegrammer.server.model.ReplenishPreKeysRequest
import com.telegrammer.server.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bson.types.ObjectId

fun Route.keyRoutes(keyRepository: KeyRepository) {
    authenticate {
        route("/keys") {
            post("/bundle") {
                val body = call.receive<PreKeyBundleRequest>()
                val userId = ObjectId(call.userId())

                val bundle = PreKeyBundle(
                    userId = userId,
                    identityKey = body.identityKey,
                    signedPreKey = body.signedPreKey,
                    oneTimePreKeys = body.oneTimePreKeys
                )
                keyRepository.upsertBundle(userId, bundle)
                call.respond(HttpStatusCode.Created, mapOf("message" to "Bundle uploaded"))
            }

            get("/bundle/{userId}") {
                val targetUserId = call.parameters["userId"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing userId"))
                    return@get
                }
                val bundle = keyRepository.fetchAndConsumeOneTimeKey(ObjectId(targetUserId))
                if (bundle == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "No key bundle found"))
                    return@get
                }
                call.respond(bundle)
            }

            post("/replenish") {
                val body = call.receive<ReplenishPreKeysRequest>()
                val userId = ObjectId(call.userId())
                keyRepository.addOneTimePreKeys(userId, body.oneTimePreKeys)
                val count = keyRepository.getOneTimeKeyCount(userId)
                call.respond(mapOf("message" to "Keys added", "totalOneTimeKeys" to count))
            }
        }
    }
}
