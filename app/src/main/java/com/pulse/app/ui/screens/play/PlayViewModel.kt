package com.pulse.app.ui.screens.play

import android.content.Context
import android.media.audiofx.Visualizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.pulse.app.data.LevelDto
import com.pulse.app.data.RetrofitClient
import com.pulse.app.data.SegmentDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayViewModel : ViewModel() {

    var level: LevelDto? by mutableStateOf(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set

    var currentSegmentIndex by mutableIntStateOf(0)
        private set
    var currentLoopIndex by mutableIntStateOf(0)
        private set
    var segmentTimeRemaining by mutableIntStateOf(0)
        private set
    var showNextPreview by mutableStateOf(false)
        private set

    var score by mutableIntStateOf(0)
        private set
    var combo by mutableIntStateOf(0)
        private set
    var totalElapsedSeconds by mutableIntStateOf(0)
        private set

    var fftData by mutableStateOf(FloatArray(0))
        private set
    var isComplete by mutableStateOf(false)
        private set
    var showVisualizer by mutableStateOf(true)
        private set
    var isPaused by mutableStateOf(false)
        private set

    private var timerJob: Job? = null
    private var exoPlayer: ExoPlayer? = null
    private var visualizer: Visualizer? = null
    private var lastFftUpdate = 0L
    private var appContext: Context? = null

    val currentSegment: SegmentDto?
        get() {
            val lvl = level ?: return null
            return if (currentSegmentIndex < lvl.segments.size)
                lvl.segments[currentSegmentIndex]
            else null
        }

    val nextSegment: SegmentDto?
        get() {
            val lvl = level ?: return null
            val nextIndex = currentSegmentIndex + 1
            return if (nextIndex < lvl.segments.size)
                lvl.segments[nextIndex]
            else null
        }

    fun toggleVisualizer() {
        showVisualizer = !showVisualizer
    }

    fun pause() {
        if (isPaused) return
        isPaused = true
        timerJob?.cancel()
        exoPlayer?.pause()
        visualizer?.enabled = false
    }

    fun resume() {
        if (!isPaused) return
        isPaused = false
        exoPlayer?.play()
        visualizer?.enabled = true
        resumeSegment()
    }

    fun restart(context: Context) {
        timerJob?.cancel()
        exoPlayer?.release()
        visualizer?.release()
        currentSegmentIndex = 0
        currentLoopIndex = 0
        segmentTimeRemaining = 0
        score = 0
        combo = 0
        totalElapsedSeconds = 0
        showNextPreview = false
        isPaused = false
        isComplete = false
        fftData = FloatArray(0)
        setupPlayer(context)
        startSegment()
    }

    fun restartSegment() {
        isPaused = false
        currentLoopIndex = 0
        exoPlayer?.play()
        visualizer?.enabled = true
        startSegment()
    }

    fun loadLevel(levelId: String, context: Context) {
        appContext = context
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                level = RetrofitClient.api.getLevel(levelId)
                setupPlayer(context)
                startSegment()
            } catch (e: Exception) {
                error = "Could not load level."
            } finally {
                isLoading = false
            }
        }
    }

    private fun setupPlayer(context: Context) {
        val segment = level?.segments?.firstOrNull { it.type == "active" } ?: return
        val songFile = segment.song?.audioFile ?: return
        exoPlayer = ExoPlayer.Builder(context).build().also { player ->
            val uri = "android.resource://${context.packageName}/raw/${songFile.removeSuffix(".mp3")}"
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()
            val audioSessionId = player.audioSessionId
            if (audioSessionId != 0) setupVisualizer(audioSessionId)
        }
    }

    private fun setupVisualizer(audioSessionId: Int) {
        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer, waveform: ByteArray, samplingRate: Int) {}
                    override fun onFftDataCapture(v: Visualizer, fft: ByteArray, samplingRate: Int) {
                        val now = System.currentTimeMillis()
                        if (now - lastFftUpdate < 33) return
                        lastFftUpdate = now
                        fftData = FloatArray(fft.size / 2) { i ->
                            val real = fft[2 * i].toFloat()
                            val imag = fft[2 * i + 1].toFloat()
                            Math.sqrt((real * real + imag * imag).toDouble()).toFloat()
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
        } catch (e: Exception) {
            // fail silently
        }
    }

    fun startSegment() {
        val segment = currentSegment ?: return
        segmentTimeRemaining = segment.durationSeconds
        showNextPreview = false
        resumeSegment()
    }

    fun resumeSegment() {
        val segment = currentSegment ?: return
        showNextPreview = false
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (segmentTimeRemaining > 0) {
                delay(1000)
                segmentTimeRemaining--
                totalElapsedSeconds++
                if (segment.type == "active") {
                    score += 10
                    combo++
                }
                if (segmentTimeRemaining == 5 && nextSegment != null) {
                    showNextPreview = true
                }
            }
            showNextPreview = false
            advanceSegment()
        }
    }

    private fun advanceSegment() {
        val lvl = level ?: return
        val segment = currentSegment ?: return
        if (currentLoopIndex < segment.loopCount - 1) {
            currentLoopIndex++
            startSegment()
            return
        }
        currentLoopIndex = 0
        currentSegmentIndex++
        if (currentSegmentIndex >= lvl.segments.size) {
            isComplete = true
            return
        }
        if (currentSegment?.type == "rest") {
            combo = 0
            exoPlayer?.pause()
        } else {
            exoPlayer?.play()
        }
        startSegment()
    }

    fun cleanup() {
        timerJob?.cancel()
        visualizer?.release()
        exoPlayer?.release()
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}