package com.pulse.app.ui.screens.prelevel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.data.LevelDto
import com.pulse.app.data.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PreLevelViewModel : ViewModel() {
    var level: LevelDto? by mutableStateOf(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var countdown by mutableStateOf(3)
        private set
    var countdownActive by mutableStateOf(false)
        private set

    fun loadLevel(levelId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                level = RetrofitClient.api.getLevel(levelId)
                startCountdown()
            } catch (e: Exception) {
                error = "Could not load level."
            } finally {
                isLoading = false
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            countdownActive = true
            countdown = 3
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }
    }

    fun cancelCountdown() {
        countdownActive = false
        countdown = 3
    }
}