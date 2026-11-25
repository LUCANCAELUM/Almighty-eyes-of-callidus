package com.example.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

data class DeviceTilt(val x: Float = 0f, val y: Float = 0f)

/**
 * Feeds subtle real-time device accelerometer tilt into floating 3D glass HUD panels
 * for physical 3D parallax without requiring any runtime permissions.
 */
@Composable
fun rememberDeviceTilt(): State<DeviceTilt> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(DeviceTilt()) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || accel == null) {
            return@DisposableEffect onDispose { }
        }

        var smoothX = 0f
        var smoothY = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rawX = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
                val rawY = ((event.values[1] - 4.5f) / 9.81f).coerceIn(-1f, 1f)
                smoothX += (rawX - smoothX) * 0.12f
                smoothY += (rawY - smoothY) * 0.12f
                tiltState.value = DeviceTilt(x = smoothX, y = smoothY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return tiltState
}
