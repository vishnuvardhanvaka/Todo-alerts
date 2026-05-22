package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TodoItem
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainTodoScreen()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainTodoScreen(
    viewModel: TodoViewModel = viewModel()
) {
    val context = LocalContext.current
    val todos by viewModel.filteredTodos.collectAsStateWithLifecycle()
    val allTodos by viewModel.allTodos.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedPriorityFilter by viewModel.selectedPriorityFilter.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoItem?>(null) }
    var currentTab by remember { mutableStateOf("Tasks") }

    // Navigation and status permission request
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Notifications enabled! You will now receive alert reminders.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied. Phone alerts will not trigger.", Toast.LENGTH_LONG).show()
        }
    }

    // Categories definition
    val categories = listOf("All", "Work", "Personal", "Health", "Shopping", "Finance")

    // Stats calculations
    val totalCount = allTodos.size
    val pendingCount = allTodos.count { !it.isCompleted }
    val completedCount = allTodos.count { it.isCompleted }
    val activeAlertsCount = allTodos.count { !it.isCompleted && it.isReminderSet && it.reminderTime != null && it.reminderTime > System.currentTimeMillis() }
    val forgottenOrOverdueCount = allTodos.count { !it.isCompleted && it.isReminderSet && it.reminderTime != null && it.reminderTime <= System.currentTimeMillis() }

    // Filter list based on selected Tab channel in single view
    val displayTodos = remember(todos, currentTab, allTodos) {
        when (currentTab) {
            "Alerts" -> allTodos.filter { !it.isCompleted && it.isReminderSet && it.reminderTime != null }
            "Calendar" -> allTodos.filter { !it.isCompleted && it.reminderTime != null }.sortedBy { it.reminderTime }
            else -> todos
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            CustomBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                activeAlertsCount = activeAlertsCount + forgottenOrOverdueCount
            )
        },
        floatingActionButton = {
            if (currentTab != "Settings") {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_todo_fab"),
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF21005D),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Schedule a task")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New task", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFEF7FF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header (Top bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        Toast.makeText(context, "$activeAlertsCount pending alerts configured.", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color(0xFF1D1B20)
                        )
                    }

                    Text(
                        text = "Tasks",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1D1B20),
                        letterSpacing = (-0.5).sp
                    )

                    // User avatar with "VV"
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFEADDFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VV",
                            color = Color(0xFF21005D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable container holding headers & body
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Summary dashboard progress card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                val greeting = remember {
                                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                                    when (hour) {
                                        in 0..11 -> "Good morning"
                                        in 12..16 -> "Good afternoon"
                                        else -> "Good evening"
                                    }
                                }
                                Text(
                                    text = "$greeting, Vishnu",
                                    color = Color(0xFF21005D),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (pendingCount == 1) {
                                        "You have 1 task\nremaining for today."
                                    } else {
                                        "You have $pendingCount tasks\nremaining for today."
                                    },
                                    color = Color(0xFF21005D),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 28.sp
                                )
                            }

                            // Geometric balanced custom progress indicator bar
                            val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD0BCFF))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                            .background(Color(0xFF6750A4))
                                    )
                                }
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    color = Color(0xFF21005D),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Permission banner
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9DEDC)),
                            border = BorderStroke(1.dp, Color(0xFFF2B8B5)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Phone Alerts Disabled",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF410E0B),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Grant notification rules to hear alarms.",
                                        color = Color(0xFF410E0B).copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Grant", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Urgent highlight card if any alerts are overdue/forgotten
                    if (forgottenOrOverdueCount > 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9DEDC)),
                            border = BorderStroke(1.dp, Color(0xFFF2B8B5)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFB3261E), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Urgent reminder",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Active Alerts",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF410E0B),
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "$forgottenOrOverdueCount Overdue",
                                            color = Color(0xFFB3261E),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "You missed $forgottenOrOverdueCount alarm reminders! Take action now to get back on track.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF410E0B).copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // If Settings page is clicked, show settings layout directly in column
                    if (currentTab == "Settings") {
                        SettingsSection(
                            context = context,
                            viewModel = viewModel,
                            hasNotificationPermission = hasNotificationPermission,
                            onRequestPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            activeAlertsCount = activeAlertsCount + forgottenOrOverdueCount
                        )
                    } else {
                        // Regular Filters & Scrollable Task Lists
                        Text(
                            text = when (currentTab) {
                                "Alerts" -> "Alert Reminders"
                                "Calendar" -> "Calendar Schedule"
                                else -> "Today's Schedule"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF49454F),
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )

                        // Top filter bars (Only shown for General "Tasks" Tab)
                        if (currentTab == "Tasks") {
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search todo item details...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Categories Filter Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(categories) { category ->
                                    val isSelected = selectedCategory == category
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setSelectedCategory(category) },
                                        label = { Text(category) },
                                        modifier = Modifier.testTag("category_chip_$category"),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF6750A4),
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFFE7E0EC),
                                            labelColor = Color(0xFF49454F)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Priority Filter Selection
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Priority:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF49454F))
                                listOf("All" to 0, "Low" to 1, "Medium" to 2, "High" to 3).forEach { (label, value) ->
                                    val isSelected = selectedPriorityFilter == value
                                    InputChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setSelectedPriorityFilter(value) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = InputChipDefaults.inputChipColors(
                                            selectedContainerColor = when(value) {
                                                1 -> Color(0xFFE8F1FF)
                                                2 -> Color(0xFFFFF4E5)
                                                3 -> Color(0xFFFFDAD7)
                                                else -> Color(0xFF6750A4)
                                            },
                                            selectedLabelColor = when(value) {
                                                1 -> Color(0xFF004481)
                                                2 -> Color(0xFF663C00)
                                                3 -> Color(0xFF410002)
                                                else -> Color.White
                                            },
                                            containerColor = Color(0xFFFEF7FF),
                                            labelColor = Color(0xFF49454F)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // List results rendering
                        if (displayTodos.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "No tasks",
                                        modifier = Modifier.size(64.dp),
                                        tint = Color(0xFFCAC4D0)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (totalCount == 0) "No tasks scheduled!" else "No matching tasks found.",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF49454F)
                                    )
                                    Text(
                                        text = if (totalCount == 0) "Tap the '+' icon to configure and schedule your task alert reminders." else "Adjust search query filters above.",
                                        color = Color(0xFF49454F).copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 32.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("todo_list"),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp, top = 4.dp)
                            ) {
                                items(displayTodos) { item ->
                                    TodoCard(
                                        item = item,
                                        onToggleCompletion = { viewModel.toggleTodoCompletion(item) },
                                        onEditClick = { editingTodo = item },
                                        onDeleteClick = { viewModel.deleteTodo(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Add / Edit Dialog
    if (showAddDialog || editingTodo != null) {
        val originalItem = editingTodo
        TodoInputDialog(
            onDismiss = {
                showAddDialog = false
                editingTodo = null
            },
            onSave = { updatedItem ->
                if (originalItem != null) {
                    viewModel.updateTodo(updatedItem)
                } else {
                    viewModel.insertTodo(updatedItem)
                }
                showAddDialog = false
                editingTodo = null
            },
            todoItem = originalItem
        )
    }
}

@Composable
fun TodoCard(
    item: TodoItem,
    onToggleCompletion: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isOverdue = item.reminderTime != null && item.reminderTime <= System.currentTimeMillis() && !item.isCompleted
    val friendlyAlarmText = if (item.isReminderSet && item.reminderTime != null) {
        formatReminderTime(item.reminderTime)
    } else null

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) {
                Color(0xFFFFFFFF).copy(alpha = 0.5f)
            } else if (isOverdue) {
                Color(0xFFFFDAD7)
            } else {
                Color(0xFFFFFFFF)
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isCompleted) {
                Color(0xFFCAC4D0).copy(alpha = 0.1f)
            } else if (isOverdue) {
                Color(0xFFF2B8B5)
            } else {
                Color(0xFFCAC4D0).copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("todo_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox styled as modern round-corner square matching design HTML
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (item.isCompleted) Color(0xFF6750A4) else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = Color(0xFF6750A4),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onToggleCompletion() }
                    .testTag("checkbox_${item.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (item.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark Complete",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Task content and category
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (item.isCompleted) {
                            Color(0xFF1D1B20).copy(alpha = 0.6f)
                        } else if (isOverdue) {
                            Color(0xFF410E0B)
                        } else {
                            Color(0xFF1D1B20)
                        },
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Compact Category indicator badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFEADDFF).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D)
                        )
                    }
                }

                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = Color(0xFF49454F).copy(alpha = if (item.isCompleted) 0.6f else 1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Alert details indicator
                if (friendlyAlarmText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isOverdue) Icons.Default.NotificationImportant else Icons.Default.Alarm,
                            contentDescription = null,
                            tint = if (isOverdue) Color(0xFFB3261E) else Color(0xFF6750A4),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isOverdue) "Overdue Alert!" else friendlyAlarmText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isOverdue) Color(0xFFB3261E) else Color(0xFF6750A4)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Items
            Row {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Task Details",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Task completely",
                        tint = Color(0xFFB3261E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    context: Context,
    viewModel: TodoViewModel,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit,
    activeAlertsCount: Int
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Preferences & Alert Tools",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D1B20)
            )

            HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f))

            // Permission Checker Tool
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notification Permission",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (hasNotificationPermission) "Granted - Alerts will sound" else "Denied - Sound of alerts is silenced",
                        color = if (hasNotificationPermission) Color(0xFF6750A4) else Color(0xFFB3261E),
                        fontSize = 12.sp
                    )
                }
                if (!hasNotificationPermission) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Grant", fontSize = 12.sp)
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = Color(0xFF6750A4)
                    )
                }
            }

            // Real physical alarm sound test tool
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Instant 5-Second Alarm Test",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Launches an actual system alert broadcast in 5 seconds to verify trigger sound and toast alerts.",
                        color = Color(0xFF49454F),
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = {
                        val testTime = System.currentTimeMillis() + 5000
                        val testTodo = TodoItem(
                            title = "🔔 Test Alert Success!",
                            description = "Your scheduled phone notification reminder is working perfectly!",
                            isCompleted = false,
                            isReminderSet = true,
                            reminderTime = testTime,
                            category = "Personal",
                            priority = 3
                        )
                        viewModel.insertTodo(testTodo)
                        Toast.makeText(context, "Test alarm scheduled! Wait 5 seconds...", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEADDFF), contentColor = Color(0xFF21005D)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Trigger Test", fontSize = 12.sp)
                }
            }

            // Instructions Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF7FF), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Active Alerts Count: $activeAlertsCount",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1D1B20)
                )
                Text(
                    text = "If you don't receive notifications, verify that your device is not in Do Not Disturb mode and has battery saver disabled.",
                    fontSize = 11.sp,
                    color = Color(0xFF49454F)
                )
            }
        }
    }
}

@Composable
fun CustomBottomNavBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    activeAlertsCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3EDF7))
            .border(width = 1.dp, color = Color(0xFFCAC4D0).copy(alpha = 0.3f))
            .navigationBarsPadding()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 1: Tasks
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { onTabSelected("Tasks") },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (currentTab == "Tasks") {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8DEF8), RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Tasks",
                            tint = Color(0xFF1D192B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Tasks",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Tasks",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == "Tasks") FontWeight.Bold else FontWeight.Medium,
                    color = if (currentTab == "Tasks") Color(0xFF1D192B) else Color(0xFF49454F)
                )
            }
        }

        // Tab 2: Alerts
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { onTabSelected("Alerts") },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box {
                    if (currentTab == "Alerts") {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE8DEF8), RoundedCornerShape(16.dp))
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts",
                                tint = Color(0xFF1D192B),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Alerts",
                            tint = Color(0xFF49454F),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (activeAlertsCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-2).dp)
                                .background(Color(0xFFB3261E), CircleShape)
                                .size(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$activeAlertsCount",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = "Alerts",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == "Alerts") FontWeight.Bold else FontWeight.Medium,
                    color = if (currentTab == "Alerts") Color(0xFF1D192B) else Color(0xFF49454F)
                )
            }
        }

        // Tab 3: Calendar
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { onTabSelected("Calendar") },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (currentTab == "Calendar") {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8DEF8), RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Calendar",
                            tint = Color(0xFF1D192B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Calendar",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Calendar",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == "Calendar") FontWeight.Bold else FontWeight.Medium,
                    color = if (currentTab == "Calendar") Color(0xFF1D192B) else Color(0xFF49454F)
                )
            }
        }

        // Tab 4: Settings
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { onTabSelected("Settings") },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (currentTab == "Settings") {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8DEF8), RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF1D192B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = "Settings",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == "Settings") FontWeight.Bold else FontWeight.Medium,
                    color = if (currentTab == "Settings") Color(0xFF1D192B) else Color(0xFF49454F)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodoInputDialog(
    onDismiss: () -> Unit,
    onSave: (TodoItem) -> Unit,
    todoItem: TodoItem? = null
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(todoItem?.title ?: "") }
    var description by remember { mutableStateOf(todoItem?.description ?: "") }
    var category by remember { mutableStateOf(todoItem?.category ?: "Personal") }
    var priority by remember { mutableStateOf(todoItem?.priority ?: 2) }
    var isReminderSet by remember { mutableStateOf(todoItem?.isReminderSet ?: false) }

    val calendar = remember {
        Calendar.getInstance().apply {
            todoItem?.reminderTime?.let { timeInMillis = it } ?: add(Calendar.HOUR_OF_DAY, 1)
        }
    }

    var reminderTimeState by remember { mutableStateOf(calendar.timeInMillis) }
    val categories = listOf("Work", "Personal", "Health", "Shopping", "Finance")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = if (todoItem == null) "Schedule New Task" else "Edit Scheduled Task",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Title Input
                Text("Task Title*", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What needs to be done?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                Text("Notes & Details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Add instructions or alert details...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category chips
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    maxItemsInEachRow = 3,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        InputChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = Color(0xFF6750A4),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Priority selector
                Text("Priority Level", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Low" to 1, "Medium" to 2, "High" to 3).forEach { (label, pr) ->
                        val isSelected = priority == pr
                        Button(
                            onClick = { priority = pr },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) {
                                    when (pr) {
                                        3 -> Color(0xFFFFDAD7)
                                        2 -> Color(0xFFFFF4E5)
                                        else -> Color(0xFFE8F1FF)
                                    }
                                } else {
                                    Color(0xFFE7E0EC)
                                },
                                contentColor = if (isSelected) {
                                    when (pr) {
                                        3 -> Color(0xFF410002)
                                        2 -> Color(0xFF663C00)
                                        else -> Color(0xFF004481)
                                    }
                                } else {
                                    Color(0xFF49454F)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Alarm switch block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isReminderSet = !isReminderSet }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notification Switch",
                            tint = if (isReminderSet) Color(0xFF6750A4) else Color(0xFF49454F),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Phone Alert Notification", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Trigger alarm sound & visual banner", fontSize = 11.sp, color = Color(0xFF49454F))
                        }
                    }
                    Switch(
                        checked = isReminderSet,
                        onCheckedChange = { isReminderSet = it }
                    )
                }

                // Picker buttons
                AnimatedVisibility(
                    visible = isReminderSet,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE7E0EC).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Alert scheduled: ${formatReminderTime(reminderTimeState)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6750A4),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showDatePicker(context, reminderTimeState) { year, month, day ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, day)
                                        reminderTimeState = calendar.timeInMillis
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Pick Date", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    showTimePicker(context, reminderTimeState) { hour, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        calendar.set(Calendar.SECOND, 0)
                                        calendar.set(Calendar.MILLISECOND, 0)
                                        reminderTimeState = calendar.timeInMillis
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Pick Time", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val finalItem = (todoItem ?: TodoItem(
                                title = title,
                                description = description,
                                category = category,
                                priority = priority,
                                isReminderSet = isReminderSet,
                                reminderTime = if (isReminderSet) reminderTimeState else null
                            )).copy(
                                title = title,
                                description = description,
                                category = category,
                                priority = priority,
                                isReminderSet = isReminderSet,
                                reminderTime = if (isReminderSet) reminderTimeState else null
                            )
                            onSave(finalItem)
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_todo_button")
                    ) {
                        Text("Save Task")
                    }
                }
            }
        }
    }
}

private fun unwrapContext(context: Context): Context {
    var current = context
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) {
            return current
        }
        current = current.baseContext
    }
    return context
}

private fun showDatePicker(
    context: Context,
    initialTime: Long,
    onDateSelected: (year: Int, month: Int, day: Int) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialTime }
    android.app.DatePickerDialog(
        unwrapContext(context),
        { _, year, month, day -> onDateSelected(year, month, day) },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showTimePicker(
    context: Context,
    initialTime: Long,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialTime }
    android.app.TimePickerDialog(
        unwrapContext(context),
        { _, hour, minute -> onTimeSelected(hour, minute) },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}

private fun formatReminderTime(timestamp: Long?): String {
    if (timestamp == null) return ""
    val now = Calendar.getInstance()
    val reminder = Calendar.getInstance().apply { timeInMillis = timestamp }

    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = sdf.format(reminder.time)

    return when {
        now.get(Calendar.YEAR) == reminder.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == reminder.get(Calendar.DAY_OF_YEAR) -> {
            "Today at $formattedTime"
        }
        now.get(Calendar.YEAR) == reminder.get(Calendar.YEAR) &&
        reminder.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Tomorrow at $formattedTime"
        }
        else -> {
            val dateSdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            dateSdf.format(reminder.time)
        }
    }
}
