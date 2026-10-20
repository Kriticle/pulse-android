package com.pulse.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pulse.app.data.UserDto

object UserSession {
    var currentUser: UserDto? by mutableStateOf(null)
    val userId: String? get() = currentUser?.id
}
