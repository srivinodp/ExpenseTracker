package com.example.expensetracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    // Process data for charts
    val categoryTotals = expenses.groupBy { it.category }
        .mapValues { it.value.sumOf { exp -> exp.amount } }

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
            Text("Dashboard", fontSize = 28.sp, fontWeight = FontWeight.Bold)
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
                            Text(
                                text = "Spent This Month",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "View Details",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "₹${String.format(Locale.US, "%,.2f", currentMonthSpent)}",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val topInsight = insights.firstOrNull()
                        if (topInsight != null) {
                            val (insightIcon, insightIconTint) = when (topInsight.severity) {
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
                                        text = topInsight.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = topInsight.description,
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
                        Text("Category Breakdown", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
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

                            Chart(
                                chart = columnChart(),
                                model = entryModelOf(entries),
                                startAxis = rememberStartAxis(),
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
                        Text("All Transactions", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
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
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.category, fontWeight = FontWeight.Bold)
                if (expense.description.isNotEmpty()) {
                    Text(expense.description, fontSize = 12.sp, color = Color.Gray)
                }
                Text(dateFormat.format(Date(expense.timestamp)), fontSize = 12.sp, color = Color.Gray)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "₹${expense.amount}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
