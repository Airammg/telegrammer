package com.telegrammer.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.telegrammer.android.ui.profile.ProfileEditScreen
import com.telegrammer.android.ui.profile.ProfileEditViewModel
import kotlinx.coroutines.launch

object Routes {
    const val PHONE_INPUT = "phone_input"
    const val OTP = "otp/{phoneNumber}"
    const val CONVERSATIONS = "conversations"
    const val CHAT = "chat/{chatId}/{recipientId}"
    const val CONTACTS = "contacts"
    const val PROFILE_EDIT = "profile_edit"

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
                onNewChat = { navController.navigate(Routes.CONTACTS) },
                onProfileClick = { navController.navigate(Routes.PROFILE_EDIT) }
            )
        }

        composable(Routes.PROFILE_EDIT) {
            val viewModel = ProfileEditViewModel(deps.userApi)
            ProfileEditScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
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
            val scope = rememberCoroutineScope()
            ContactsScreen(
                viewModel = viewModel,
                onContactClick = { userId ->
                    scope.launch {
                        val result = deps.chatApi.createChat(userId)
                        result.onSuccess { chat ->
                            navController.navigate(Routes.chat(chat.id, userId)) {
                                popUpTo(Routes.CONVERSATIONS)
                            }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
