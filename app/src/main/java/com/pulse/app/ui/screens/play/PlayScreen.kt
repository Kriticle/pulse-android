package com.pulse.app.ui.screens.play

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

@Composable
fun PlayScreen(
    levelId: String,
    onComplete: (score: Int, durationSeconds: Int, accuracyPct: Float) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: PlayViewModel = viewModel()
    val context = LocalContext.current
    val cameraAlreadyGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    var permissionChoiceMade by remember { mutableStateOf(cameraAlreadyGranted) }
    var hasCameraPermission by remember { mutableStateOf(cameraAlreadyGranted) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        viewModel.enableCameraJudging(hasCameraPermission)
        permissionChoiceMade = true
    }

    LaunchedEffect(levelId, permissionChoiceMade) {
        if (!permissionChoiceMade || viewModel.level != null || viewModel.isLoading) return@LaunchedEffect
        viewModel.enableCameraJudging(hasCameraPermission)
        viewModel.loadLevel(levelId, context)
    }

    LaunchedEffect(viewModel.isComplete) {
        if (viewModel.isComplete) {
            onComplete(viewModel.score, viewModel.totalElapsedSeconds, viewModel.accuracyPct)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.cleanup() }
    }

    val accentColor = MaterialTheme.colorScheme.primary

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !permissionChoiceMade -> {
                CameraPermissionChoice(
                    onEnable = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        )
                    },
                    onSkip = {
                        hasCameraPermission = false
                        viewModel.enableCameraJudging(false)
                        permissionChoiceMade = true
                    }
                )
            }
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

                Box(modifier = Modifier.fillMaxSize()) {

                    val activeCamera = hasCameraPermission &&
                        viewModel.cameraJudgingEnabled &&
                        viewModel.currentSegment?.type == "active" &&
                        !viewModel.isPaused

                    if (activeCamera) {
                        CameraCoach(
                            exerciseName = viewModel.currentSegment?.exercise?.name.orEmpty(),
                            onAssessment = viewModel::updateMovementAssessment,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { scaleX = -1f }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.22f))
                        )
                    }

                    // Full screen crossfade — camera, animation, or visualizer
                    Crossfade(
                        targetState = viewModel.currentSegment?.id,
                        animationSpec = tween(600),
                        label = "segmentTransition",
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(
                                viewModel.currentSegment?.id,
                                viewModel.showVisualizer,
                                viewModel.isPaused,
                                activeCamera
                            ) {
                                detectTapGestures {
                                    val activeSegment = viewModel.currentSegment?.type == "active"
                                    if (activeSegment && !viewModel.isPaused && !activeCamera) {
                                        viewModel.toggleVisualizer()
                                    }
                                }
                            }
                    ) { segmentId ->
                        val segment = viewModel.level?.segments?.find { it.id == segmentId }
                        val isRest = segment?.type == "rest"

                        if (activeCamera) {
                            Box(Modifier.fillMaxSize())
                        } else if (viewModel.showVisualizer && !isRest) {
                            CircularVisualizer(
                                fftData = viewModel.fftData,
                                accentColor = accentColor,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                segment?.exercise?.animationFile?.let { animFile ->
                                    val resourceId = context.resources.getIdentifier(
                                        animFile.substringBeforeLast('.'), "raw", context.packageName
                                    )
                                    if (resourceId != 0) {
                                        val gentleScale by rememberInfiniteTransition(
                                            label = "exerciseMotion"
                                        ).animateFloat(
                                            initialValue = 0.97f,
                                            targetValue = 1.03f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(900),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "exerciseScale"
                                        )
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                model = resourceId,
                                                imageLoader = imageLoader
                                            ),
                                            contentDescription = segment.exercise.name,
                                            modifier = Modifier
                                                .size(300.dp)
                                                .graphicsLayer {
                                                    scaleX = gentleScale
                                                    scaleY = gentleScale
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (activeCamera) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 16.dp)
                                .width(210.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    text = if (viewModel.bodyVisible) "FORM ${viewModel.accuracyPct.toInt()}%" else "CAMERA COACH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewModel.bodyVisible) accentColor else MaterialTheme.colorScheme.error,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = viewModel.movementFeedback,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // HUD — top left score
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

                    // HUD — top right timer
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

                    // HUD — top center pause
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

                    // HUD — bottom center exercise name with crossfade
                    Crossfade(
                        targetState = viewModel.currentSegment?.id,
                        animationSpec = tween(600),
                        label = "exerciseNameTransition",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                    ) { segmentId ->
                        val segment = viewModel.level?.segments?.find { it.id == segmentId }
                        val isRest = segment?.type == "rest"
                        Column(
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
                            if (isRest == false && segment?.song != null) {
                                Text(
                                    text = segment.song.title,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            if (isRest == false) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        if (hasCameraPermission) {
                                            viewModel.enableCameraJudging(!viewModel.cameraJudgingEnabled)
                                        } else {
                                            viewModel.toggleVisualizer()
                                        }
                                    }
                                ) {
                                    Text(
                                        text = when {
                                            hasCameraPermission && viewModel.cameraJudgingEnabled -> "Use demo view"
                                            hasCameraPermission -> "Use camera coach"
                                            viewModel.showVisualizer -> "Show exercise"
                                            else -> "Show visualizer"
                                        },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    // HUD — bottom right next preview
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

@Composable
private fun CameraPermissionChoice(
    onEnable: () -> Unit,
    onSkip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.width(420.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CAMERA COACH",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Get live movement feedback",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Pulse checks full-body pose on this device to adjust your score. Camera frames are not saved or uploaded. Audio access only powers the music visualizer.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable camera coaching", modifier = Modifier.padding(vertical = 5.dp))
                }
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue without camera")
                }
            }
        }
    }
}
