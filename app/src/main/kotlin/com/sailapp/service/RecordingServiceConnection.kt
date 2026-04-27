package com.sailapp.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecordingServiceConnection(private val context: Context) {

    private val _service = MutableStateFlow<RecordingService?>(null)
    val service: StateFlow<RecordingService?> = _service.asStateFlow()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            _service.value = (binder as RecordingService.RecordingBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            _service.value = null
        }
    }

    fun bind() {
        val intent = Intent(context, RecordingService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        try {
            context.unbindService(connection)
        } catch (_: IllegalArgumentException) { }
        _service.value = null
    }

    fun startRecording(tripId: Long) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_TRIP_ID, tripId)
        }
        context.startForegroundService(intent)
    }

    fun pauseRecording() {
        context.startService(
            Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_PAUSE
            }
        )
    }

    fun resumeRecording() {
        context.startService(
            Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_RESUME
            }
        )
    }

    fun stopRecording() {
        context.startService(
            Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
        )
    }
}
