package com.example.data

import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    val allTodos: Flow<List<TodoItem>> = todoDao.getAllTodos()

    suspend fun getTodoById(id: Int): TodoItem? {
        return todoDao.getTodoById(id)
    }

    suspend fun getActiveReminders(currentTime: Long): List<TodoItem> {
        return todoDao.getActiveReminders(currentTime)
    }

    suspend fun insertTodo(todo: TodoItem): Long {
        return todoDao.insertTodo(todo)
    }

    suspend fun updateTodo(todo: TodoItem) {
        todoDao.updateTodo(todo)
    }

    suspend fun deleteTodo(todo: TodoItem) {
        todoDao.deleteTodo(todo)
    }
}
