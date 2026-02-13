package com.telegrammer.android.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.telegrammer.shared.model.AuthState

@Composable
fun PhoneInputScreen(
    viewModel: AuthViewModel,
    onOtpSent: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var phoneNumber by remember { mutableStateOf("+") }

    LaunchedEffect(state) {
        if (state is AuthState.OtpSent) {
            onOtpSent((state as AuthState.OtpSent).phoneNumber)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Telegrammer",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your phone number to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone number") },
                placeholder = { Text("+14155551234") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.requestOtp(phoneNumber) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (state is AuthState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (state as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.requestOtp(phoneNumber) },
                enabled = !isLoading && phoneNumber.length > 4,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continue")
                }
            }
        }
    }
}
