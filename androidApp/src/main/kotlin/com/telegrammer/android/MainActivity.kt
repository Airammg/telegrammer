package com.telegrammer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.telegrammer.android.navigation.NavGraph
import com.telegrammer.android.ui.theme.TelegrammerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deps = (application as TelegrammerApp).dependencies
        setContent {
            TelegrammerTheme {
                NavGraph(deps)
            }
        }
    }
}
