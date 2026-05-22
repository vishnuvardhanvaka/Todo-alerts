package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val reminderTime: Long? = null,
    val isReminderSet: Boolean = false,
    val category: String = "Personal",
    val priority: Int = 2, // 1 = Low, 2 = Medium, 3 = High
    val createdTime: Long = System.currentTimeMillis()
)
