package com.telegrammer.shared.repository

import com.telegrammer.shared.api.ContactApi
import com.telegrammer.shared.model.User
import com.telegrammer.shared.platform.FlowWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ContactRepository(private val contactApi: ContactApi) {

    private val _contacts = MutableStateFlow<List<User>>(emptyList())
    val contacts: Flow<List<User>> = _contacts.asStateFlow()

    fun contactsWrapped(): FlowWrapper<List<User>> =
        FlowWrapper(_contacts.asStateFlow())

    suspend fun resolveContacts(phoneNumbers: List<String>): List<User> {
        val result = contactApi.resolveContacts(phoneNumbers)
        return result.fold(
            onSuccess = { users ->
                _contacts.value = users
                users
            },
            onFailure = { emptyList() }
        )
    }

    suspend fun refreshContacts(): List<User> {
        val result = contactApi.getContacts()
        return result.fold(
            onSuccess = { users ->
                _contacts.value = users
                users
            },
            onFailure = { _contacts.value }
        )
    }
}
