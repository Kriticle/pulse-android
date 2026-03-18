package com.pulse.app.ui.screens.results

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.UserSession
import com.pulse.app.data.AttemptResponse
import com.pulse.app.data.BestAttemptDto
import com.pulse.app.data.CreateAttemptRequest
import com.pulse.app.data.RetrofitClient
import kotlinx.coroutines.launch

class ResultsViewModel : ViewModel() {
    var isLoading by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set
    var attemptResponse: AttemptResponse? by mutableStateOf(null)
        private set
    var previousBest: BestAttemptDto? by mutableStateOf(null)
        private set
    var isNewBest by mutableStateOf(false)
        private set

    fun submitAttempt(levelId: String, score: Int, durationSeconds: Int) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                // Fetch previous best before submitting
                val userId = UserSession.userId
                val bestAttempts = RetrofitClient.api.getBestAttempts(userId)
                previousBest = bestAttempts.find { it.levelId == levelId }

                // Submit attempt
                val response = RetrofitClient.api.postAttempt(
                    CreateAttemptRequest(
                        userId = userId,
                        levelId = levelId,
                        durationSeconds = durationSeconds,
                        accuracyPct = 0f
                    )
                )
                attemptResponse = response
                UserSession.currentUser = response.user

                // Check if new best
                isNewBest = previousBest == null || score > (previousBest?.score ?: 0)
            } catch (e: Exception) {
                error = "Could not save your result."
            } finally {
                isLoading = false
            }
        }
    }
}