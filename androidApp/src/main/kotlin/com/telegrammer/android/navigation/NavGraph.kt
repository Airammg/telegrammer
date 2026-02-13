package com.telegrammer.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.telegrammer.android.AppDependencies
import com.telegrammer.android.ui.auth.AuthViewModel
import com.telegrammer.android.ui.auth.OtpScreen
import com.telegrammer.android.ui.auth.PhoneInputScreen
import com.telegrammer.android.ui.chat.ChatScreen
import com.telegrammer.android.ui.chat.ChatViewModel
import com.telegrammer.android.ui.contacts.ContactsScreen
import com.telegrammer.android.ui.contacts.ContactsViewModel
import com.telegrammer.android.ui.conversations.ConversationListScreen
import com.telegrammer.android.ui.conversations.ConversationListViewModel

object Routes {
    const val PHONE_INPUT = "phone_input"
    const val OTP = "otp/{phoneNumber}"
    const val CONVERSATIONS = "conversations"
    const val CHAT = "chat/{chatId}/{recipientId}"
    const val CONTACTS = "contacts"

    fun otp(phone: String) = "otp/$phone"
    fun chat(chatId: String, recipientId: String) = "chat/$chatId/$recipientId"
}

@Composable
fun NavGraph(deps: AppDependencies) {
    val navController = rememberNavController()
    val startDestination = if (deps.authRepo.isLoggedIn()) Routes.CONVERSATIONS else Routes.PHONE_INPUT

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.PHONE_INPUT) {
            val viewModel = AuthViewModel(deps.authRepo)
            PhoneInputScreen(
                viewModel = viewModel,
                onOtpSent = { phone -> navController.navigate(Routes.otp(phone)) }
            )
        }

        composable(
            Routes.OTP,
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStack ->
            val phone = backStack.arguments?.getString("phoneNumber") ?: ""
            val viewModel = AuthViewModel(deps.authRepo)
            OtpScreen(
                viewModel = viewModel,
                phoneNumber = phone,
                onAuthenticated = {
                    navController.navigate(Routes.CONVERSATIONS) {
                        popUpTo(Routes.PHONE_INPUT) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CONVERSATIONS) {
            val viewModel = ConversationListViewModel(deps.chatRepo)
            ConversationListScreen(
                viewModel = viewModel,
                onConversationClick = { chatId, recipientId ->
                    navController.navigate(Routes.chat(chatId, recipientId))
                },
                onNewChat = { navController.navigate(Routes.CONTACTS) }
            )
        }

        composable(
            Routes.CHAT,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("recipientId") { type = NavType.StringType }
            )
        ) { backStack ->
            val chatId = backStack.arguments?.getString("chatId") ?: ""
            val recipientId = backStack.arguments?.getString("recipientId") ?: ""
            val viewModel = ChatViewModel(deps.chatRepo, chatId, recipientId)
            ChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CONTACTS) {
            val viewModel = ContactsViewModel(deps.contactRepo)
            ContactsScreen(
                viewModel = viewModel,
                onContactClick = { userId ->
                    // TODO: Create chat first, then navigate
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
