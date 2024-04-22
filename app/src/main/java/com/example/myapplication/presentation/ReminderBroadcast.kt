package com.example.myapplication.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderBroadcast : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminder = ExerciseReminder()
        reminder.createNotificationChannel(context)
        reminder.sendExerciseReminder(context)
    }
}




