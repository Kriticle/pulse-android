package com.pulse.app.ui.screens.levelselect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.UserSession
import com.pulse.app.SupabaseClient
import com.pulse.app.data.BestAttemptDto
import com.pulse.app.data.LevelDto
import com.pulse.app.data.RetrofitClient
import com.pulse.app.data.UserDto
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class LevelSelectViewModel : ViewModel() {
    var levels: List<LevelDto> by mutableStateOf(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var expandedLevelId: String? by mutableStateOf(null)
        private set
    var bestAttempts: Map<String, BestAttemptDto> by mutableStateOf(emptyMap())
        private set
    var currentUser: UserDto? by mutableStateOf(null)
        private set

    var selectedCategory by mutableStateOf("all")
        private set

    val filteredLevels: List<LevelDto>
        get() = if (selectedCategory == "all")
            levels
        else
            levels.filter { it.categories.contains(selectedCategory) }

    fun selectCategory(category: String) {
        selectedCategory = category
    }

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val userId = UserSession.userId
                    ?: SupabaseClient.client.auth.currentUserOrNull()?.id
                    ?: throw IllegalStateException("No signed-in user")
                val fetchedLevels = RetrofitClient.api.getLevels()
                val fetchedAttempts = RetrofitClient.api.getBestAttempts(userId)
                val fetchedUser = RetrofitClient.api.getUser(userId)
                levels = fetchedLevels
                bestAttempts = fetchedAttempts.associateBy { it.levelId }
                currentUser = fetchedUser
                UserSession.currentUser = fetchedUser
            } catch (e: Exception) {
                error = "Could not load levels. Is the server running?"
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleExpanded(levelId: String) {
        expandedLevelId = if (expandedLevelId == levelId) null else levelId
    }
}
