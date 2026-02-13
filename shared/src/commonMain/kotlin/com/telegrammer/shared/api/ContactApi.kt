package com.telegrammer.shared.api

import com.telegrammer.shared.model.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

@Serializable
data class ResolveContactsRequest(val phoneNumbers: List<String>)

class ContactApi(private val client: HttpClient) {

    suspend fun resolveContacts(phoneNumbers: List<String>): Result<List<User>> = runCatching {
        client.post("/contacts/resolve") {
            setBody(ResolveContactsRequest(phoneNumbers))
        }.body()
    }

    suspend fun getContacts(): Result<List<User>> = runCatching {
        client.get("/contacts").body()
    }
}
