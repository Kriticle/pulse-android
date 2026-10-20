package com.pulse.app.ui.screens.play

import android.annotation.SuppressLint
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot

data class MovementAssessment(
    val bodyVisible: Boolean,
    val quality: Float,
    val feedback: String,
    val repetitions: Int
)

/**
 * Displays a local-only front-camera preview and emits deliberately conservative pose feedback.
 * Frames are passed directly to ML Kit and are never saved or uploaded by this class.
 */
@Composable
fun CameraCoach(
    exerciseName: String,
    modifier: Modifier = Modifier,
    onAssessment: (MovementAssessment) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analyzer = remember(exerciseName) { PoseMovementAnalyzer(onAssessment) }

    DisposableEffect(lifecycleOwner, exerciseName) {
        analyzer.exerciseName = exerciseName
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var cameraProvider: ProcessCameraProvider? = null

        val listener = Runnable {
            cameraProvider = cameraProviderFuture.get().also { provider ->
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(640, 480))
                    .build()
                    .also { it.setAnalyzer(analyzer.executor, analyzer) }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysis
                    )
                } catch (_: Exception) {
                    onAssessment(MovementAssessment(false, 0f, "Camera unavailable", 0))
                }
            }
        }
        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            cameraProvider?.unbindAll()
            analyzer.close()
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}

private class PoseMovementAnalyzer(
    private val onAssessment: (MovementAssessment) -> Unit
) : ImageAnalysis.Analyzer {
    val executor = Executors.newSingleThreadExecutor()
    var exerciseName: String = ""
    private val processing = AtomicBoolean(false)
    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )
    private var wasOpen = false
    private var repetitions = 0

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || !processing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener { pose -> onAssessment(evaluate(pose)) }
            .addOnFailureListener {
                onAssessment(MovementAssessment(false, 0f, "Finding your pose…", repetitions))
            }
            .addOnCompleteListener {
                processing.set(false)
                imageProxy.close()
            }
    }

    private fun evaluate(pose: Pose): MovementAssessment {
        val required = listOf(
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE,
            PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST
        ).mapNotNull(pose::getPoseLandmark)

        val confidence = required.map { it.inFrameLikelihood }.average().toFloat()
        if (required.size < 10 || confidence < 0.55f) {
            return MovementAssessment(false, 0f, "Step back so your full body is visible", repetitions)
        }

        return when {
            exerciseName.contains("jump", ignoreCase = true) -> jumpingJack(pose)
            exerciseName.contains("lunge", ignoreCase = true) -> lunge(pose)
            else -> MovementAssessment(true, 0.75f, "Body visible · keep moving", repetitions)
        }
    }

    private fun jumpingJack(pose: Pose): MovementAssessment {
        val leftWrist = point(pose, PoseLandmark.LEFT_WRIST)
        val rightWrist = point(pose, PoseLandmark.RIGHT_WRIST)
        val leftShoulder = point(pose, PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = point(pose, PoseLandmark.RIGHT_SHOULDER)
        val leftAnkle = point(pose, PoseLandmark.LEFT_ANKLE)
        val rightAnkle = point(pose, PoseLandmark.RIGHT_ANKLE)
        val shoulderWidth = distance(leftShoulder, rightShoulder).coerceAtLeast(1f)
        val ankleWidth = distance(leftAnkle, rightAnkle)

        val armsUp = leftWrist.y < leftShoulder.y && rightWrist.y < rightShoulder.y
        val legsOpen = ankleWidth > shoulderWidth * 1.35f
        val open = armsUp && legsOpen
        val closed = !armsUp && ankleWidth < shoulderWidth * 1.05f
        if (open && !wasOpen) repetitions++
        wasOpen = open

        val quality = when {
            open -> 1f
            closed -> 0.82f
            armsUp || legsOpen -> 0.58f
            else -> 0.4f
        }
        val message = when {
            open -> "Great extension · $repetitions reps"
            closed -> "Good reset · open on the beat"
            !armsUp -> "Reach both hands overhead"
            else -> "Step wider with the beat"
        }
        return MovementAssessment(true, quality, message, repetitions)
    }

    private fun lunge(pose: Pose): MovementAssessment {
        val leftAngle = kneeAngle(pose, true)
        val rightAngle = kneeAngle(pose, false)
        val bestAngle = minOf(leftAngle, rightAngle)
        val shoulderMid = midpoint(
            point(pose, PoseLandmark.LEFT_SHOULDER),
            point(pose, PoseLandmark.RIGHT_SHOULDER)
        )
        val hipMid = midpoint(
            point(pose, PoseLandmark.LEFT_HIP),
            point(pose, PoseLandmark.RIGHT_HIP)
        )
        val torsoLean = abs(shoulderMid.x - hipMid.x) /
            distance(shoulderMid, hipMid).coerceAtLeast(1f)
        val depthScore = (1f - abs(bestAngle - 95f) / 65f).coerceIn(0f, 1f)
        val postureScore = (1f - torsoLean * 1.8f).coerceIn(0f, 1f)
        val quality = (depthScore * 0.7f + postureScore * 0.3f).coerceIn(0f, 1f)

        val lowPosition = bestAngle < 125f
        if (lowPosition && !wasOpen) repetitions++
        wasOpen = lowPosition
        val message = when {
            torsoLean > 0.35f -> "Lift your chest"
            bestAngle > 130f -> "Lower with control"
            bestAngle < 65f -> "Ease the depth slightly"
            else -> "Strong lunge · $repetitions reps"
        }
        return MovementAssessment(true, quality, message, repetitions)
    }

    private fun kneeAngle(pose: Pose, left: Boolean): Float {
        val hip = point(pose, if (left) PoseLandmark.LEFT_HIP else PoseLandmark.RIGHT_HIP)
        val knee = point(pose, if (left) PoseLandmark.LEFT_KNEE else PoseLandmark.RIGHT_KNEE)
        val ankle = point(pose, if (left) PoseLandmark.LEFT_ANKLE else PoseLandmark.RIGHT_ANKLE)
        val first = distance(knee, ankle)
        val second = distance(knee, hip)
        val opposite = distance(hip, ankle)
        val cosine = ((first * first + second * second - opposite * opposite) /
            (2f * first * second).coerceAtLeast(0.001f)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosine).toDouble()).toFloat()
    }

    private fun point(pose: Pose, type: Int) =
        requireNotNull(pose.getPoseLandmark(type)).position

    private fun distance(a: android.graphics.PointF, b: android.graphics.PointF) =
        hypot(a.x - b.x, a.y - b.y)

    private fun midpoint(a: android.graphics.PointF, b: android.graphics.PointF) =
        android.graphics.PointF((a.x + b.x) / 2f, (a.y + b.y) / 2f)

    fun close() {
        detector.close()
        executor.shutdown()
    }
}
