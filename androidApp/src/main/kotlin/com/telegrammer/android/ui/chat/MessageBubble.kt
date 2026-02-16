package com.telegrammer.android.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telegrammer.shared.model.Message
import com.telegrammer.shared.model.MessageStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(message: Message) {
    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val shape = if (isOutgoing) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    if (isOutgoing) {
                        val icon = when (message.status) {
                            MessageStatus.SENDING -> Icons.Default.Schedule
                            MessageStatus.SENT -> Icons.Default.Done
                            MessageStatus.DELIVERED, MessageStatus.READ -> Icons.Default.DoneAll
                            MessageStatus.FAILED -> Icons.Default.Schedule
                        }
                        val tint = when (message.status) {
                            MessageStatus.READ -> Color(0xFF34B7F1) // Blue check marks
                            else -> textColor.copy(alpha = 0.7f)
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = message.status.name,
                            modifier = Modifier.size(14.dp),
                            tint = tint
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
