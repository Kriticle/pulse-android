package com.pulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.pulse.app.ui.screens.levelselect.LevelSelectScreen
import com.pulse.app.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LevelSelectScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}