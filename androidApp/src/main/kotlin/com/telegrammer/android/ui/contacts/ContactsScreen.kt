package com.telegrammer.android.ui.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.telegrammer.shared.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    onContactClick: (userId: String) -> Unit,
    onBack: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No contacts found.\nAdd contacts by syncing your phone book.",
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
                items(contacts, key = { it.id }) { contact ->
                    ContactItem(
                        user = contact,
                        onClick = { onContactClick(contact.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactItem(user: User, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(user.displayName.ifEmpty { user.phoneNumber })
        },
        supportingContent = {
            if (user.displayName.isNotEmpty()) {
                Text(
                    text = user.phoneNumber,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                        text = (user.displayName.firstOrNull()
                            ?: user.phoneNumber.firstOrNull()
                            ?: '?').uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
}
