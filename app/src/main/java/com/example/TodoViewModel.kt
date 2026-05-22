package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TodoDatabase
import com.example.data.TodoItem
import com.example.data.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TodoRepository
    private val alarmScheduler: TodoAlarmScheduler

    init {
        val database = TodoDatabase.getInstance(application)
        repository = TodoRepository(database.todoDao())
        alarmScheduler = TodoAlarmScheduler(application)
    }

    // Filter and search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedPriorityFilter = MutableStateFlow(0) // 0 = All, 1 = Low, 2 = Medium, 3 = High
    val selectedPriorityFilter = _selectedPriorityFilter.asStateFlow()

    val allTodos: StateFlow<List<TodoItem>> = repository.allTodos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredTodos: StateFlow<List<TodoItem>> = combine(
        allTodos,
        _searchQuery,
        _selectedCategory,
        _selectedPriorityFilter
    ) { todos, query, category, priority ->
        todos.filter { item ->
            val matchesQuery = item.title.contains(query, ignoreCase = true) || 
                               item.description.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || item.category.equals(category, ignoreCase = true)
            val matchesPriority = priority == 0 || item.priority == priority
            matchesQuery && matchesCategory && matchesPriority
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedPriorityFilter(priority: Int) {
        _selectedPriorityFilter.value = priority
    }

    fun insertTodo(todo: TodoItem) {
        viewModelScope.launch {
            val id = repository.insertTodo(todo)
            // Need the correct ID to schedule the alarm.
            val itemWithId = todo.copy(id = id.toInt())
            if (itemWithId.isReminderSet && itemWithId.reminderTime != null && !itemWithId.isCompleted) {
                alarmScheduler.schedule(itemWithId)
            }
        }
    }

    fun updateTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.updateTodo(todo)
            if (todo.isCompleted) {
                alarmScheduler.cancel(todo)
            } else if (todo.isReminderSet && todo.reminderTime != null) {
                alarmScheduler.schedule(todo)
            } else {
                alarmScheduler.cancel(todo)
            }
        }
    }

    fun toggleTodoCompletion(todo: TodoItem) {
        val updated = todo.copy(isCompleted = !todo.isCompleted)
        updateTodo(updated)
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            alarmScheduler.cancel(todo)
            repository.deleteTodo(todo)
        }
    }
}
