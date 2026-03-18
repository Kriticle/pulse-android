package com.pulse.app.ui.screens.levelselect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.UserSession
import com.pulse.app.data.BestAttemptDto
import com.pulse.app.data.LevelDto
import com.pulse.app.data.RetrofitClient
import com.pulse.app.data.UserDto
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

    private val userId = UserSession.userId

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
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