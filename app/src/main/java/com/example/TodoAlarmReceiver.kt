package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.TodoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getIntExtra("TODO_ID", -1)
        if (todoId == -1) return

        val goAsync = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TodoDatabase.getInstance(context)
                val todo = db.todoDao().getTodoById(todoId)

                if (todo != null && !todo.isCompleted) {
                    showNotification(context, todo.id, todo.title, todo.description)
                }
            } catch (e: Exception) {
                Log.e("TodoAlarmReceiver", "Error processing alarm broadcast for ID $todoId", e)
            } finally {
                goAsync.finish()
            }
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, desc: String) {
        val channelId = "todo_alerts_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incomplete and forgotten todo items"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Using safe built-in alarm icon or app icon
        val iconRes = try {
            com.example.R.mipmap.ic_launcher
        } catch (e: Exception) {
            android.R.drawable.ic_lock_idle_alarm
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle("⏰ Forgotten Task: $title")
            .setContentText(desc.ifBlank { "You haven't completed this task yet!" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(desc.ifBlank { "You haven't completed this task yet! Tap to view details and mark it done." }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
        Log.d("TodoAlarmReceiver", "Posted notification for task $id: $title")
    }
}
