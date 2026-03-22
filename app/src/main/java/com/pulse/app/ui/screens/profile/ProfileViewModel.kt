package com.pulse.app.ui.screens.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.UserSession
import com.pulse.app.data.RetrofitClient
import com.pulse.app.data.UserDto
import kotlinx.coroutines.launch
import com.pulse.app.SupabaseClient
import io.github.jan.supabase.auth.auth

class ProfileViewModel : ViewModel() {
    var user: UserDto? by mutableStateOf(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val userId = UserSession.userId
                user = RetrofitClient.api.getUser(userId)
                UserSession.currentUser = user
            } catch (e: Exception) {
                error = "Could not load profile."
            } finally {
                isLoading = false
            }
        }
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
                UserSession.currentUser = null
            } catch (e: Exception) {
                // fail silently
            } finally {
                onComplete()
            }
        }
    }
}