package com.pulse.app.ui.screens.play

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder

@Composable
fun PlayScreen(
    levelId: String,
    onComplete: (score: Int) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: PlayViewModel = viewModel()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(levelId) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        viewModel.loadLevel(levelId, context)
    }

    LaunchedEffect(viewModel.isComplete) {
        if (viewModel.isComplete) onComplete(viewModel.score)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.cleanup() }
    }

    val accentColor = MaterialTheme.colorScheme.primary

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) }
            .build()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            viewModel.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = accentColor
                )
            }
            viewModel.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = viewModel.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadLevel(levelId, context) }) {
                        Text("Retry")
                    }
                }
            }
            viewModel.level != null -> {
                val segment = viewModel.currentSegment
                val isRest = segment?.type == "rest"

                // Full screen animation / visualizer
                Box(modifier = Modifier.fillMaxSize()) {

                    if (viewModel.showVisualizer && !isRest) {
                        CircularVisualizer(
                            fftData = viewModel.fftData,
                            accentColor = accentColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        segment?.exercise?.animationFile?.let { animFile ->
                            val resourceId = context.resources.getIdentifier(
                                animFile, "raw", context.packageName
                            )
                            if (resourceId != 0) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = resourceId,
                                        imageLoader = imageLoader
                                    ),
                                    contentDescription = segment.exercise.name,
                                    modifier = Modifier
                                        .size(280.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }

                    // HUD overlay
                    // Top left — score
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "SCORE",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${viewModel.score}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Top right — timer
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${viewModel.segmentTimeRemaining}s",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.segmentTimeRemaining <= 5)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        if (viewModel.currentSegment?.loopCount != null &&
                            viewModel.currentSegment!!.loopCount > 1
                        ) {
                            Text(
                                text = "Round ${viewModel.currentLoopIndex + 1}/${viewModel.currentSegment!!.loopCount}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Bottom center — exercise name
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isRest) "REST" else segment?.exercise?.name?.uppercase() ?: "",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRest)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else
                                accentColor
                        )
                        if (!isRest && segment?.song != null) {
                            Text(
                                text = segment.song.title,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        if (!isRest) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.toggleVisualizer() }) {
                                Text(
                                    text = if (viewModel.showVisualizer) "Show exercise" else "Show visualizer",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    // Next segment preview — bottom left
                    AnimatedVisibility(
                        visible = viewModel.showNextPreview,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        viewModel.nextSegment?.let { next ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "NEXT UP",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = next.exercise.name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "${next.durationSeconds}s",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Pause button — top center
                    IconButton(
                        onClick = { viewModel.pause() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "⏸",
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    // Pause overlay
                    if (viewModel.isPaused) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.width(260.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "PAUSED",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = 3.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = { viewModel.resume() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text(
                                            text = "Resume",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.restartSegment() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Restart segment",
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.restart(context) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Restart level",
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            viewModel.cleanup()
                                            onBack()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Quit",
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}