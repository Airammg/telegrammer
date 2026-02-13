package com.telegrammer.android.ui.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.telegrammer.shared.model.Conversation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    viewModel: ConversationListViewModel,
    onConversationClick: (chatId: String, recipientId: String) -> Unit,
    onNewChat: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Telegrammer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = "New chat")
            }
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No conversations yet.\nTap + to start a new chat.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = {
                            val recipientId = conversation.otherUser?.id ?: ""
                            onConversationClick(conversation.id, recipientId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(conversation: Conversation, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = conversation.otherUser?.displayName?.ifEmpty {
                    conversation.otherUser?.phoneNumber ?: "Unknown"
                } ?: "Unknown",
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = conversation.lastMessagePreview ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (conversation.otherUser?.displayName?.firstOrNull()
                            ?: conversation.otherUser?.phoneNumber?.firstOrNull()
                            ?: '?').uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        trailingContent = {
            if (conversation.unreadCount > 0) {
                Badge {
                    Text(conversation.unreadCount.toString())
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
}
