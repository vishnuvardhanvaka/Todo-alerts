package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.TodoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val goAsync = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = TodoDatabase.getInstance(context)
                    val activeReminders = db.todoDao().getActiveReminders(System.currentTimeMillis())
                    val alarmScheduler = TodoAlarmScheduler(context)

                    for (item in activeReminders) {
                        alarmScheduler.schedule(item)
                    }
                    Log.d("BootReceiver", "Rescheduled ${activeReminders.size} active reminders after boot.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling reminders on boot", e)
                } finally {
                    goAsync.finish()
                }
            }
        }
    }
}
