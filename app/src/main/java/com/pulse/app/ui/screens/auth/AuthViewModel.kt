package com.pulse.app.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.SupabaseClient
import com.pulse.app.UserSession
import com.pulse.app.data.RetrofitClient
import com.pulse.app.data.UpsertUserRequest
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var isLoggedIn by mutableStateOf(false)
        private set

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            try {
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                if (session != null) {
                    loadUser(session.user?.id ?: return@launch)
                    isLoggedIn = true
                }
            } catch (e: Exception) {
                // No session
            }
        }
    }

    fun signInWithGoogle(
        idToken: String,
        rawNonce: String,
        email: String,
        displayName: String
    ) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                SupabaseClient.client.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    this.nonce = rawNonce
                    this.provider = Google
                }
                val userId = SupabaseClient.client.auth.currentUserOrNull()?.id
                    ?: throw Exception("No user ID after sign in")
                loadOrCreateUser(userId, email, displayName)
                isLoggedIn = true
            } catch (e: Exception) {
                error = "Sign in failed. Please try again."
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun loadUser(userId: String) {
        try {
            val user = RetrofitClient.api.getUser(userId)
            UserSession.currentUser = user
        } catch (e: Exception) {
            // User doesn't exist in our DB yet
        }
    }

    private suspend fun loadOrCreateUser(authId: String, email: String, displayName: String) {
        val user = RetrofitClient.api.upsertUser(
            UpsertUserRequest(
                displayName = displayName.ifBlank { email.substringBefore("@") }
            )
        )
        UserSession.currentUser = user
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signOut()
                UserSession.currentUser = null
                isLoggedIn = false
            } catch (e: Exception) {
                error = "Sign out failed."
            }
        }
    }

    fun showSignInError(message: String) {
        error = message
    }
}
