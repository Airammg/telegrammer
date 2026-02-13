package com.telegrammer.android.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.telegrammer.shared.model.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    viewModel: AuthViewModel,
    phoneNumber: String,
    onAuthenticated: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var code by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is AuthState.Authenticated) {
            onAuthenticated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Verification Code",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We sent a code to $phoneNumber",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it },
                label = { Text("6-digit code") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.verifyOtp(phoneNumber, code) }
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
                onClick = { viewModel.verifyOtp(phoneNumber, code) },
                enabled = !isLoading && code.length == 6,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Verify")
                }
            }
        }
    }
}
