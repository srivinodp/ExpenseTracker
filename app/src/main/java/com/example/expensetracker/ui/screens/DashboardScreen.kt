package com.example.expensetracker.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.expensetracker.util.ExportUtils
import com.example.expensetracker.util.PreferenceUtils
import com.example.expensetracker.notification.ExpenseNotificationScheduler
import androidx.compose.ui.platform.LocalContext
import android.app.TimePickerDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.ExpenseViewModel
import com.example.expensetracker.data.ExpenseEntity
import com.example.expensetracker.ui.HeuristicEngine
import com.example.expensetracker.ui.HeuristicInsight
import com.example.expensetracker.ui.InsightSeverity
import com.example.expensetracker.ui.InsightType
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.text.SimpleDateFormat
import java.util.*

val categoryColors = listOf(
    Color(0xFF00BFA5), Color(0xFFFF5252), Color(0xFF448AFF),
    Color(0xFFFFC400), Color(0xFFE040FB), Color(0xFF00E676)
)

val categoryColorsMap = mapOf(
    "Food" to Color(0xFF00BFA5),          // Teal/Green
    "Transport" to Color(0xFFFF5252),     // Red
    "Utilities" to Color(0xFF448AFF),     // Blue
    "Entertainment" to Color(0xFFFFC400), // Amber/Yellow
    "Shopping" to Color(0xFFE040FB),      // Purple/Pink
    "Other" to Color(0xFF00E676)          // Light Green
)

fun getCategoryColor(category: String, index: Int): Color {
    return categoryColorsMap[category] ?: categoryColors[index % categoryColors.size]
}

enum class TrendPeriod {
    DAY, MONTH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onAddExpense: () -> Unit,
    onEditExpense: (Int) -> Unit
) {
    val expenses by viewModel.allExpenses.collectAsState()
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var selectedPeriod by remember { mutableStateOf(TrendPeriod.DAY) }
    var showInsightsSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    val currentMonthKey = remember {
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }
    
    val availablePeriods = remember(expenses) {
        val sdfMonthKey = SimpleDateFormat("yyyy-MM", Locale.US)
        val sdfMonthDisplay = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        
        val uniqueMonthsMap = mutableMapOf<String, String>()
        
        // Always include the current month
        val nowCal = Calendar.getInstance()
        val currentKey = sdfMonthKey.format(nowCal.time)
        val currentDisplay = sdfMonthDisplay.format(nowCal.time)
        uniqueMonthsMap[currentKey] = currentDisplay
        
        expenses.forEach { exp ->
            val date = Date(exp.timestamp)
            val key = sdfMonthKey.format(date)
            val display = sdfMonthDisplay.format(date)
            uniqueMonthsMap[key] = display
        }
        
        val sortedKeys = uniqueMonthsMap.keys.sortedDescending()
        
        val list = mutableListOf<Pair<String, String>>()
        list.add("Overall" to "Overall")
        
        sortedKeys.forEach { key ->
            list.add(key to uniqueMonthsMap[key]!!)
        }
        
        list
    }

    var selectedPiePeriodKey by remember { mutableStateOf(currentMonthKey) }

    // --- Heuristic Insights Computations at function top-level scope ---
    val nowCalendar = remember { Calendar.getInstance() }
    val insights = remember(expenses) { HeuristicEngine.generateInsights(expenses, nowCalendar) }
    val currentMonthSpent = remember(expenses) {
        val currentYear = nowCalendar.get(Calendar.YEAR)
        val currentMonth = nowCalendar.get(Calendar.MONTH)
        expenses.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }.sumOf { it.amount }
    }

    val lastMonthSpent = remember(expenses) {
        val lastMonthCal = (nowCalendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val lastMonthYear = lastMonthCal.get(Calendar.YEAR)
        val lastMonthVal = lastMonthCal.get(Calendar.MONTH)
        expenses.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.YEAR) == lastMonthYear && cal.get(Calendar.MONTH) == lastMonthVal
        }.sumOf { it.amount }
    }

    // Process data for charts
    val categoryTotals = remember(expenses, selectedPiePeriodKey) {
        val sdfMonthKey = SimpleDateFormat("yyyy-MM", Locale.US)
        
        val filtered = if (selectedPiePeriodKey == "Overall") {
            expenses
        } else {
            expenses.filter {
                sdfMonthKey.format(Date(it.timestamp)) == selectedPiePeriodKey
            }
        }
        
        filtered.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
    }

    val sdfDayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDayDisplay = SimpleDateFormat("dd MMM", Locale.getDefault())
    val sdfMonthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val sdfMonthDisplay = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    val chartData = remember(expenses, selectedPeriod) {
        if (selectedPeriod == TrendPeriod.DAY) {
            expenses.groupBy {
                sdfDayKey.format(Date(it.timestamp))
            }.toSortedMap().map { (key, value) ->
                val displayKey = try {
                    sdfDayDisplay.format(sdfDayKey.parse(key) ?: Date())
                } catch (e: Exception) {
                    key
                }
                displayKey to value.sumOf { it.amount }
            }
        } else {
            expenses.groupBy {
                sdfMonthKey.format(Date(it.timestamp))
            }.toSortedMap().map { (key, value) ->
                val displayKey = try {
                    sdfMonthDisplay.format(sdfMonthKey.parse(key) ?: Date())
                } catch (e: Exception) {
                    key
                }
                displayKey to value.sumOf { it.amount }
            }
        }
    }

    // Delete Confirmation Dialog
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense?") },
            text = { Text("Are you sure you want to permanently delete this expense of ₹${expenseToDelete?.amount} under '${expenseToDelete?.category}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it) }
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Report Dialog
    if (showExportDialog) {
        val context = LocalContext.current
        var exportStartDate by remember {
            mutableStateOf(
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
            )
        }
        var exportEndDate by remember {
            mutableStateOf(System.currentTimeMillis())
        }

        val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val startCal = Calendar.getInstance().apply { timeInMillis = exportStartDate }
        val startDatePickerDialog = DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                exportStartDate = cal.timeInMillis
            },
            startCal.get(Calendar.YEAR),
            startCal.get(Calendar.MONTH),
            startCal.get(Calendar.DAY_OF_MONTH)
        )

        val endCal = Calendar.getInstance().apply { timeInMillis = exportEndDate }
        val endDatePickerDialog = DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth, 23, 59, 59)
                exportEndDate = cal.timeInMillis
            },
            endCal.get(Calendar.YEAR),
            endCal.get(Calendar.MONTH),
            endCal.get(Calendar.DAY_OF_MONTH)
        )

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Expense Report", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Download your expenses in Excel-compatible CSV format. Select the date range below:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Start Date", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { startDatePickerDialog.show() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(displayFormat.format(Date(exportStartDate)))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("End Date", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { endDatePickerDialog.show() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(displayFormat.format(Date(exportEndDate)))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ExportUtils.exportAndShareExpenses(context, expenses, exportStartDate, exportEndDate)
                        showExportDialog = false
                    }
                ) {
                    Text("Export & Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Custom Settings Dialog
    if (showSettingsDialog) {
        val context = LocalContext.current
        
        // Reminder States
        var isReminderEnabled by remember { mutableStateOf(PreferenceUtils.isReminderEnabled(context)) }
        var reminderHour by remember { mutableStateOf(PreferenceUtils.getReminderHour(context)) }
        var reminderMinute by remember { mutableStateOf(PreferenceUtils.getReminderMinute(context)) }
        
        // Categories States
        var categoriesList by remember { mutableStateOf(PreferenceUtils.getCategories(context)) }
        var newCategoryName by remember { mutableStateOf("") }
        var categoryError by remember { mutableStateOf<String?>(null) }
        
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minuteOfHour ->
                reminderHour = hourOfDay
                reminderMinute = minuteOfHour
                PreferenceUtils.setReminderTime(context, hourOfDay, minuteOfHour)
                ExpenseNotificationScheduler.scheduleDailyNotification(context)
            },
            reminderHour,
            reminderMinute,
            false // Use 12-hour format with AM/PM picker
        )

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Settings", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showSettingsDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp) // Constrain height and make it scrollable if many categories are added
                ) {
                    TabRow(selectedTabIndex = 0, containerColor = Color.Transparent) {
                        Tab(
                            selected = true,
                            onClick = {},
                            text = { Text("Preferences", fontWeight = FontWeight.Bold) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable section for configurations
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Section 1: Notifications
                        item {
                            Text(
                                "Daily Reminder Notification",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable Daily Reminder", fontSize = 14.sp)
                                Switch(
                                    checked = isReminderEnabled,
                                    onCheckedChange = { isChecked ->
                                        isReminderEnabled = isChecked
                                        PreferenceUtils.setReminderEnabled(context, isChecked)
                                        ExpenseNotificationScheduler.scheduleDailyNotification(context)
                                    }
                                )
                            }
                            
                            if (isReminderEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Reminder Time", fontSize = 14.sp)
                                    
                                    val amPm = if (reminderHour >= 12) "PM" else "AM"
                                    val hour12 = when {
                                        reminderHour == 0 -> 12
                                        reminderHour > 12 -> reminderHour - 12
                                        else -> reminderHour
                                    }
                                    val timeStr = String.format(Locale.US, "%02d:%02d %s", hour12, reminderMinute, amPm)
                                    
                                    OutlinedButton(onClick = { timePickerDialog.show() }) {
                                        Text(timeStr)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // Section 2: Manage Categories
                        item {
                            Text(
                                "Manage Categories",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Add Category input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newCategoryName,
                                    onValueChange = {
                                        newCategoryName = it
                                        categoryError = null
                                    },
                                    placeholder = { Text("New Category") },
                                    isError = categoryError != null,
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val trimmed = newCategoryName.trim()
                                        if (trimmed.isEmpty()) {
                                            categoryError = "Cannot be empty"
                                        } else if (PreferenceUtils.addCategory(context, trimmed)) {
                                            newCategoryName = ""
                                            categoryError = null
                                            categoriesList = PreferenceUtils.getCategories(context)
                                        } else {
                                            categoryError = "Already exists"
                                        }
                                    }
                                ) {
                                    Text("Add")
                                }
                            }
                            
                            if (categoryError != null) {
                                Text(
                                    text = categoryError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        // List categories
                        items(categoriesList) { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                color = getCategoryColor(category, categoriesList.indexOf(category)),
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(category, fontSize = 14.sp)
                                }
                                
                                // Disable deleting "Other" or if only 1 category remains
                                val canDelete = categoriesList.size > 1
                                if (canDelete) {
                                    IconButton(
                                        onClick = {
                                            PreferenceUtils.deleteCategory(context, category)
                                            categoriesList = PreferenceUtils.getCategories(context)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpense,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Log Expense"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dashboard", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- Heuristic Summary Card ---

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                onClick = { showInsightsSheet = true }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Spent This Month",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${String.format(Locale.US, "%,.2f", currentMonthSpent)}",
                                    color = Color.White,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            
                            // Visual comparison badge against last month
                            if (lastMonthSpent > 0.0) {
                                val diffPct = ((currentMonthSpent - lastMonthSpent) / lastMonthSpent) * 100.0
                                val isIncrease = diffPct > 0.0
                                val badgeText = if (isIncrease) {
                                    String.format(Locale.US, "▲ +%.1f%%", diffPct)
                                } else {
                                    String.format(Locale.US, "▼ -%.1f%%", Math.abs(diffPct))
                                }
                                val badgeBgColor = if (isIncrease) Color(0xFFFF5252).copy(alpha = 0.3f) else Color(0xFF00BFA5).copy(alpha = 0.3f)
                                val badgeTextColor = if (isIncrease) Color(0xFFFF8A80) else Color(0xFFB9F6CA)
                                
                                Surface(
                                    color = badgeBgColor,
                                    shape = CircleShape,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = badgeTextColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "View Details",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        // Select the primary advisory insight (Prefer Category Spikes/Savings, fallback to general top insight)
                        val primaryAdvice = remember(insights) {
                            insights.firstOrNull { it.type == InsightType.CATEGORY_SPIKE || it.type == InsightType.CATEGORY_SAVINGS }
                                ?: insights.firstOrNull { it.type == InsightType.LARGEST_EXPENSE }
                                ?: insights.firstOrNull()
                        }
                        
                        if (primaryAdvice != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val (insightIcon, insightIconTint) = when (primaryAdvice.severity) {
                                InsightSeverity.WARNING -> Icons.Default.Warning to Color(0xFFFF8A80)
                                InsightSeverity.SUCCESS -> Icons.Default.TrendingDown to Color(0xFFB9F6CA)
                                InsightSeverity.INFO -> Icons.Default.Info to Color(0xFF82B1FF)
                                else -> Icons.Default.Info to Color(0xFF82B1FF)
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.15f), shape = MaterialTheme.shapes.medium)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = insightIcon,
                                    contentDescription = null,
                                    tint = insightIconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = primaryAdvice.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = primaryAdvice.description,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (expenses.isEmpty()) {
                Spacer(modifier = Modifier.height(40.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No transactions logged yet.", color = Color.Gray, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap the '+' button to log your first expense.", color = Color.Gray.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category Breakdown", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                            
                            Box {
                                val currentLabel = availablePeriods.find { it.first == selectedPiePeriodKey }?.second ?: "Overall"
                                
                                FilterChip(
                                    selected = false,
                                    onClick = { dropdownExpanded = true },
                                    label = { Text(currentLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Period",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                
                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    availablePeriods.forEach { (key, display) ->
                                        DropdownMenuItem(
                                            text = { Text(display) },
                                            onClick = {
                                                selectedPiePeriodKey = key
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        PieChart(categoryTotals)
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Spending Trend", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                            
                            Row(
                                modifier = Modifier
                                    .width(180.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TrendPeriod.values().forEach { period ->
                                    val isSelected = selectedPeriod == period
                                    val label = if (period == TrendPeriod.DAY) "Day" else "Month"
                                    
                                    Surface(
                                        onClick = { selectedPeriod = period },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp),
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (chartData.isNotEmpty()) {
                            val entries = chartData.mapIndexed { index, entry ->
                                FloatEntry(x = index.toFloat(), y = entry.second.toFloat())
                            }
                            
                            val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                                val index = value.toInt()
                                if (index in chartData.indices) {
                                    chartData[index].first
                                } else {
                                    ""
                                }
                            }

                            val startAxisValueFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
                                val amt = value.toInt()
                                when {
                                    amt >= 1000 -> "₹${String.format(Locale.US, "%.1fk", amt / 1000f)}"
                                    else -> "₹$amt"
                                }
                            }

                            Chart(
                                chart = columnChart(),
                                model = entryModelOf(entries),
                                startAxis = rememberStartAxis(
                                    valueFormatter = startAxisValueFormatter
                                ),
                                bottomAxis = rememberBottomAxis(
                                    valueFormatter = bottomAxisValueFormatter
                                )
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No trend data available.", color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("All Transactions", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                            IconButton(onClick = { showExportDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export to Excel/CSV",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(expenses, key = { it.id }) { expense ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteExpense(expense)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, shape = MaterialTheme.shapes.medium)
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            ExpenseRow(
                                expense = expense,
                                onEdit = { onEditExpense(expense.id) },
                                onDelete = { expenseToDelete = expense }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Insights Modal Bottom Sheet ---
    if (showInsightsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInsightsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Heuristic Monthly Insights",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Rules-based calculations comparing this month with last month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(insights) { insight ->
                        val bgColor = when (insight.severity) {
                            InsightSeverity.WARNING -> Color(0xFFFFEBEE)
                            InsightSeverity.SUCCESS -> Color(0xFFE8F5E9)
                            InsightSeverity.INFO -> Color(0xFFE3F2FD)
                            else -> Color(0xFFE3F2FD)
                        }
                        val contentColor = when (insight.severity) {
                            InsightSeverity.WARNING -> Color(0xFFC62828)
                            InsightSeverity.SUCCESS -> Color(0xFF2E7D32)
                            InsightSeverity.INFO -> Color(0xFF1565C0)
                            else -> Color(0xFF1565C0)
                        }
                        val iconColor = when (insight.severity) {
                            InsightSeverity.WARNING -> Color(0xFFD32F2F)
                            InsightSeverity.SUCCESS -> Color(0xFF388E3C)
                            InsightSeverity.INFO -> Color(0xFF1976D2)
                            else -> Color(0xFF1976D2)
                        }
                        val icon = when (insight.severity) {
                            InsightSeverity.WARNING -> Icons.Default.Warning
                            InsightSeverity.SUCCESS -> Icons.Default.TrendingDown
                            InsightSeverity.INFO -> Icons.Default.Info
                            else -> Icons.Default.Info
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = bgColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(iconColor.copy(alpha = 0.1f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = insight.title,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = insight.description,
                                        color = contentColor.copy(alpha = 0.8f),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(categoryTotals: Map<String, Double>) {
    val total = categoryTotals.values.sum()
    var startAngle = -90f

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(150.dp)) {
            var i = 0
            for ((category, amount) in categoryTotals) {
                val sweepAngle = ((amount / total) * 360).toFloat()
                drawArc(
                    color = getCategoryColor(category, i),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
                i++
            }
        }

        Spacer(modifier = Modifier.width(32.dp))

        Column {
            var i = 0
            for ((category, amount) in categoryTotals) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color = getCategoryColor(category, i), shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$category: ₹${amount.toInt()}", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                i++
            }
        }
    }
}

@Composable
fun ExpenseRow(
    expense: ExpenseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            // Compact Header Row (Always Visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored Category Indicator Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color = getCategoryColor(expense.category, 0), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                // Category (Type) Name
                Text(
                    text = expense.category,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                
                // Date (e.g. "27 May")
                Text(
                    text = dateFormat.format(Date(expense.timestamp)),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                // Amount
                Text(
                    text = "₹${expense.amount}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            }

            // Expanded Details Block
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    
                    // Description
                    if (expense.description.isNotEmpty()) {
                        Row(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(
                                text = "Description: ",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = expense.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    // Full timestamp
                    Row(modifier = Modifier.padding(bottom = 6.dp)) {
                        Text(
                            text = "Date & Time: ",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = fullDateFormat.format(Date(expense.timestamp)),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Category details
                    Row(modifier = Modifier.padding(bottom = 6.dp)) {
                        Text(
                            text = "Category: ",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = expense.category,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Amount details
                    Row(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(
                            text = "Amount: ",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${expense.amount}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Action icons row (Edit & Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onEdit,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
