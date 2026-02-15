package com.telegrammer.android.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegrammer.shared.model.Message
import com.telegrammer.shared.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepo: ChatRepository,
    private val chatId: String,
    private val recipientId: String
) : ViewModel() {

    val messages: StateFlow<List<Message>> = chatRepo.getMessages(chatId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                chatRepo.sendMessage(chatId, recipientId, text.trim())
                _sendError.value = null
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Send failed", e)
                _sendError.value = e.message ?: "Failed to send"
            }
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
