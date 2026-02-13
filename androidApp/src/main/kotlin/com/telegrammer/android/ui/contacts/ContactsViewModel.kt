package com.telegrammer.android.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegrammer.shared.model.User
import com.telegrammer.shared.repository.ContactRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val contactRepo: ContactRepository
) : ViewModel() {

    val contacts: StateFlow<List<User>> = contactRepo.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            contactRepo.refreshContacts()
        }
    }

    fun searchByPhone(phoneNumbers: List<String>) {
        viewModelScope.launch {
            contactRepo.resolveContacts(phoneNumbers)
        }
    }
}
