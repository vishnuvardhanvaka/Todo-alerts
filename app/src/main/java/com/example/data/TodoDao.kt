package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items ORDER BY isCompleted ASC, priority DESC, createdTime DESC")
    fun getAllTodos(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items WHERE id = :id")
    suspend fun getTodoById(id: Int): TodoItem?

    @Query("SELECT * FROM todo_items WHERE isCompleted = 0 AND reminderTime > :currentTime")
    suspend fun getActiveReminders(currentTime: Long): List<TodoItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem): Long

    @Update
    suspend fun updateTodo(todo: TodoItem)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)
}
