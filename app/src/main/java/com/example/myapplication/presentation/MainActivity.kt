package com.example.myapplication.presentation

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.myapplication.R
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private var heartRateSensorListener: SensorEventListener? = null

    fun scheduleReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderBroadcast::class.java).apply {
            action = "com.example.myapplication.SEND_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
//        val repeatInterval: Long = 5000 // 5 seconds
        val repeatInterval: Long = 14400000 // 4 hours
        //debugging - Make sure it's on 4 hours normally and if not just use
        // the 5 seconds for making sure it works.

        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + repeatInterval,
            repeatInterval,
            pendingIntent
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleReminder()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        setContent {
            SensorDataApp()
        }
    }

    @Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
    @Composable
    fun SensorDataApp() {
        var heartRate by remember { mutableStateOf(0) }
        var currentTime by remember {
            mutableStateOf(
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            )
        }
        var stopwatchTime by remember { mutableStateOf(0L) } // Stopwatch time in seconds
        var isStopwatchRunning by remember { mutableStateOf(false) }
        val heartRateImage = if (heartRate > 133) R.drawable.pulsehigh else R.drawable.pulseicon
        //change image when fat burning zone is achieved which is 133bpm
        LaunchedEffect(key1 = Unit) {
            while (true) {
                currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                delay(60000)
            }
        }

        LaunchedEffect(key1 = isStopwatchRunning) {
            while (isStopwatchRunning) {
                delay(1000)
                stopwatchTime++
            }
        }

        MaterialTheme {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(16.dp)
            ) {
                Text(
                    text = currentTime,
                    modifier = Modifier.offset(y = (-10).dp) // normal time adjustment from top
                )
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Image(
                        painter = painterResource(id = heartRateImage),
                        contentDescription = "Heart Rate",
                        modifier = Modifier
                            .size(50.dp)
                            .offset(y = 10.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$heartRate bpm",
                        textAlign = TextAlign.Center,
                        color = Color.White
                        )
                    Spacer(modifier = Modifier.height(10.dp)) // stop watch spacing dont look
                    // at compsoe preview not the same and changes
                    Text(
                        text = String.format("%02d:%02d:%02d", stopwatchTime/3600,
                            (stopwatchTime/60) % 60, stopwatchTime%60),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        style = MaterialTheme.typography.body1
                    )
                    Spacer(modifier = Modifier.height(10.dp)) //increase height to lower button
                    // dont look at compose preview it's not the same as emulator
                    Row(
                        modifier = Modifier.fillMaxWidth() ,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                isStopwatchRunning = !isStopwatchRunning
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text(if (isStopwatchRunning) "Pause" else "Start")
                        }
                        Button(
                            onClick = {
                                stopwatchTime = 0
                                isStopwatchRunning = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }
        }


        LaunchedEffect(key1 = heartRateSensor) {
            startHeartRateUpdates { newHeartRate ->
                heartRate = newHeartRate
            }
        }
    }

    private fun startHeartRateUpdates(onHeartRateUpdated: (Int) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BODY_SENSORS),
                PERMISSION_REQUEST_BODY_SENSORS
            )
            return
        }

        heartRateSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_HEART_RATE) {
                        val heartRateValue = it.values[0].toInt()
                        Log.d(TAG, "The New Heart Rate Data in logcat: $heartRateValue")
                        onHeartRateUpdated(heartRateValue)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        sensorManager.registerListener(
            heartRateSensorListener,
            heartRateSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        heartRateSensorListener?.let {
            sensorManager.unregisterListener(it)
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_BODY_SENSORS = 225
        private const val TAG = "SensorDataApp"
    }
}

