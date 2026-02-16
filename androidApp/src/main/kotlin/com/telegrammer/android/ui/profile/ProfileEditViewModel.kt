package com.telegrammer.android.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.telegrammer.shared.api.UserApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileEditState(
    val displayName: String = "",
    val phoneNumber: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

class ProfileEditViewModel(
    private val userApi: UserApi
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileEditState())
    val state: StateFlow<ProfileEditState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            userApi.getMe()
                .onSuccess { user ->
                    _state.value = _state.value.copy(
                        displayName = user.displayName,
                        phoneNumber = user.phoneNumber,
                        loading = false
                    )
                }
                .onFailure { e ->
                    Log.e("ProfileEditVM", "Failed to load profile", e)
                    _state.value = _state.value.copy(
                        loading = false,
                        error = e.message ?: "Failed to load profile"
                    )
                }
        }
    }

    fun onDisplayNameChange(name: String) {
        _state.value = _state.value.copy(displayName = name, error = null)
    }

    fun save() {
        val name = _state.value.displayName.trim()
        if (name.isEmpty()) {
            _state.value = _state.value.copy(error = "Display name cannot be empty")
            return
        }
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            userApi.updateProfile(displayName = name)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false, saved = true)
                }
                .onFailure { e ->
                    Log.e("ProfileEditVM", "Failed to save profile", e)
                    _state.value = _state.value.copy(
                        saving = false,
                        error = e.message ?: "Failed to save"
                    )
                }
        }
    }
}
