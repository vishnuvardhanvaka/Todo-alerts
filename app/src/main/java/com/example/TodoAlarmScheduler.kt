package com.example

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.TodoItem

class TodoAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(item: TodoItem) {
        if (item.reminderTime == null || item.isCompleted) return

        // If the reminder time is in the past, don't schedule
        if (item.reminderTime < System.currentTimeMillis()) return

        val intent = Intent(context, TodoAlarmReceiver::class.java).apply {
            putExtra("TODO_ID", item.id)
            putExtra("TODO_TITLE", item.title)
            putExtra("TODO_DESC", item.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        item.reminderTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        item.reminderTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    item.reminderTime,
                    pendingIntent
                )
            }
            Log.d("TodoAlarmScheduler", "Alarm scheduled for ${item.title} (ID: ${item.id}) at ${item.reminderTime}")
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                item.reminderTime,
                pendingIntent
            )
            Log.e("TodoAlarmScheduler", "SecurityException during scheduleExact", e)
        } catch (e: Exception) {
            Log.e("TodoAlarmScheduler", "Exception during schedule", e)
        }
    }

    fun cancel(item: TodoItem) {
        val intent = Intent(context, TodoAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("TodoAlarmScheduler", "Alarm canceled for ${item.title} (ID: ${item.id})")
        }
    }
}
