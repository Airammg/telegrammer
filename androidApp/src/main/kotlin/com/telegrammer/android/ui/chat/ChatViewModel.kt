package com.telegrammer.android.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegrammer.shared.model.Message
import com.telegrammer.shared.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepo: ChatRepository,
    private val chatId: String,
    private val recipientId: String
) : ViewModel() {

    val messages: StateFlow<List<Message>> = chatRepo.getMessages(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepo.sendMessage(chatId, recipientId, text.trim())
        }
    }

    fun sendTyping() {
        viewModelScope.launch {
            chatRepo.sendTyping(chatId)
        }
    }

    fun markRead(messageId: String) {
        viewModelScope.launch {
            chatRepo.markRead(messageId, chatId)
        }
    }
}
