package com.pulse.app.ui.screens.results

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.pulse.app.ui.ResultsSkeleton

@Composable
fun ResultsScreen(
    levelId: String,
    score: Int,
    durationSeconds: Int,
    accuracyPct: Float,
    attemptId: String,
    onPlayAgain: () -> Unit,
    onLevelSelect: () -> Unit
) {
    val viewModel: ResultsViewModel = viewModel()

    LaunchedEffect(attemptId) {
        viewModel.submitAttempt(
            clientAttemptId = attemptId,
            levelId = levelId,
            score = score,
            durationSeconds = durationSeconds,
            accuracyPct = accuracyPct
        )
    }

    val accentColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxSize()) {

        // Top banner
        AnimatedVisibility(
            visible = viewModel.isNewBest && !viewModel.isLoading,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = accentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🏆  New personal best!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                viewModel.isLoading -> {
                    ResultsSkeleton()
                }
                viewModel.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = viewModel.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onPlayAgain) { Text("Try again") }
                    }
                }
                viewModel.attemptResponse != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 80.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top spacer to push score to center
                        Spacer(modifier = Modifier.height(1.dp))

                        // Score block
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "WORKOUT COMPLETE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = accentColor,
                                letterSpacing = 3.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            var displayedScore by remember { mutableStateOf(0) }
                            LaunchedEffect(score) {
                                val duration = 1500L
                                val steps = 60
                                val stepDelay = duration / steps
                                val increment = score.toFloat() / steps
                                for (i in 1..steps) {
                                    delay(stepDelay)
                                    displayedScore = (increment * i).toInt().coerceAtMost(score)
                                }
                            }

                            Text(
                                text = "$displayedScore",
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )

                        }

                        // Stats row
                        Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (accuracyPct > 0f) "${accuracyPct.toInt()}%" else "—",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "camera accuracy",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = formatDuration(durationSeconds),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "time completed",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = viewModel.previousBest?.let { "${it.score} pts" } ?: "-",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "personal best",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Buttons row

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onPlayAgain,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = "Play again",
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = onLevelSelect,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = "Level select",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
