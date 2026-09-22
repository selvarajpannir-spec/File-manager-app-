package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioPlayerState(
    val currentFileId: Long? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val title: String = ""
)

class AudioPreviewManager(private val context: Context) {

    private val TAG = "AudioPreviewManager"
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState: StateFlow<AudioPlayerState> = _playerState.asStateFlow()

    fun togglePlayPause(uri: Uri, fileId: Long, title: String) {
        if (_playerState.value.currentFileId == fileId && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                pause()
            } else {
                mediaPlayer?.start()
                _playerState.value = _playerState.value.copy(isPlaying = true)
                startProgressUpdates()
            }
        } else {
            playNew(uri, fileId, title)
        }
    }

    private fun playNew(uri: Uri, fileId: Long, title: String) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (uri.scheme == "file") {
                    setDataSource(uri.path)
                } else {
                    setDataSource(context, uri)
                }
                prepare()
                start()
                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        currentPositionMs = 0L
                    )
                    progressJob?.cancel()
                }
            }

            val duration = mediaPlayer?.duration?.toLong() ?: 0L
            _playerState.value = AudioPlayerState(
                currentFileId = fileId,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = duration,
                title = title
            )
            startProgressUpdates()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}")
            stop()
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
            _playerState.value = _playerState.value.copy(isPlaying = false)
            progressJob?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio: ${e.message}")
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio: ${e.message}")
        }
    }

    fun stop() {
        try {
            progressJob?.cancel()
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
            _playerState.value = AudioPlayerState()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio: ${e.message}")
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val current = mediaPlayer?.currentPosition?.toLong() ?: 0L
                _playerState.value = _playerState.value.copy(currentPositionMs = current)
                delay(200)
            }
        }
    }
}
