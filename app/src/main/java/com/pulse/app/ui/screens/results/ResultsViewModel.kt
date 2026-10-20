package com.pulse.app.ui.screens.results

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.SupabaseClient
import com.pulse.app.UserSession
import com.pulse.app.data.AttemptResponse
import com.pulse.app.data.BestAttemptDto
import com.pulse.app.data.CreateAttemptRequest
import com.pulse.app.data.RetrofitClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import retrofit2.HttpException

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
    private var submittedAttemptKey: String? = null

    fun submitAttempt(
        clientAttemptId: String,
        levelId: String,
        score: Int,
        durationSeconds: Int,
        accuracyPct: Float
    ) {
        if (submittedAttemptKey == clientAttemptId) return
        submittedAttemptKey = clientAttemptId

        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                // Fetch previous best before submitting
                val userId = UserSession.userId
                    ?: SupabaseClient.client.auth.currentUserOrNull()?.id
                    ?: throw IllegalStateException("No signed-in user")
                val bestAttempts = RetrofitClient.api.getBestAttempts(userId)
                previousBest = bestAttempts.find { it.levelId == levelId }

                // Submit attempt
                val response = RetrofitClient.api.postAttempt(
                    CreateAttemptRequest(
                        clientAttemptId = clientAttemptId,
                        userId = userId,
                        levelId = levelId,
                        score = score,
                        durationSeconds = durationSeconds,
                        accuracyPct = accuracyPct.coerceIn(0f, 100f)
                    )
                )
                attemptResponse = response
                UserSession.currentUser = response.user

                // Check if new best
                isNewBest = previousBest == null || score > (previousBest?.score ?: 0)
            } catch (e: HttpException) {
                error = when (e.code()) {
                    400 -> "Could not save your result because the attempt data was invalid."
                    401 -> "Your session expired. Please sign in again."
                    403 -> "Could not save this result for the signed-in user."
                    in 500..599 -> "The server could not save your result. Please try again."
                    else -> "Could not save your result."
                }
            } catch (e: Exception) {
                error = "Could not save your result."
            } finally {
                isLoading = false
            }
        }
    }
}
