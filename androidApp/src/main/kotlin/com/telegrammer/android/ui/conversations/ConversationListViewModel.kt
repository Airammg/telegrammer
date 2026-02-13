package com.telegrammer.android.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegrammer.shared.model.Conversation
import com.telegrammer.shared.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ConversationListViewModel(
    private val chatRepo: ChatRepository
) : ViewModel() {

    val conversations: StateFlow<List<Conversation>> = chatRepo.getConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        chatRepo.connect()
    }

    override fun onCleared() {
        super.onCleared()
        chatRepo.disconnect()
    }
}
