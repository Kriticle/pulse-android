package com.pulse.app.ui.screens.prelevel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pulse.app.data.SegmentDto

@Composable
fun PreLevelScreen(
    levelId: String,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: PreLevelViewModel = viewModel()

    LaunchedEffect(levelId) {
        viewModel.loadLevel(levelId)
    }

    // Navigate when countdown hits 0
    LaunchedEffect(viewModel.countdown, viewModel.countdownActive) {
        if (viewModel.countdownActive && viewModel.countdown == 0) {
            onStart()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            viewModel.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            viewModel.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadLevel(levelId) }) {
                        Text("Retry")
                    }
                }
            }
            viewModel.level != null -> {
                val level = viewModel.level!!
                val totalSeconds = level.segments.sumOf {
                    it.durationSeconds * it.loopCount
                }

                Row(modifier = Modifier.fillMaxSize()) {

                    // Left panel — level info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(40.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top — title + info
                        Column {
                            Text(
                                text = "READY?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 3.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = level.title,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                DifficultyChip(level.difficulty)
                                Text(
                                    text = formatDuration(totalSeconds),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Middle — countdown
                        AnimatedContent(
                            targetState = viewModel.countdown,
                            transitionSpec = {
                                (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                            },
                            label = "countdown"
                        ) { count ->
                            Text(
                                text = if (count > 0) count.toString() else "GO!",
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Bottom — cancel button
                        OutlinedButton(
                            onClick = {
                                viewModel.cancelCountdown()
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth(0.6f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    // Divider
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Right panel — sequence
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(40.dp)
                    ) {
                        Text(
                            text = "SEQUENCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(level.segments) { segment ->
                                SegmentPreviewRow(segment = segment)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentPreviewRow(segment: SegmentDto) {
    val isRest = segment.type == "rest"
    val bgColor = if (isRest)
        MaterialTheme.colorScheme.surfaceVariant
    else
        MaterialTheme.colorScheme.secondaryContainer

    val textColor = if (isRest)
        MaterialTheme.colorScheme.onSurfaceVariant
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (segment.loopCount > 1)
                        "${segment.exercise.name} ×${segment.loopCount}"
                    else
                        segment.exercise.name,
                    fontSize = 14.sp,
                    fontWeight = if (isRest) FontWeight.Normal else FontWeight.Medium,
                    color = textColor
                )
                if (!isRest && segment.song != null) {
                    Text(
                        text = "${segment.song.bpm} BPM",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "${segment.durationSeconds}s",
                fontSize = 13.sp,
                color = textColor
            )
        }
    }
}

@Composable
fun DifficultyChip(difficulty: Int) {
    val label = when (difficulty) {
        1 -> "Easy"
        2 -> "Medium"
        3 -> "Hard"
        else -> "Level $difficulty"
    }
    val color = when (difficulty) {
        1 -> MaterialTheme.colorScheme.secondary
        2 -> MaterialTheme.colorScheme.primary
        3 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}