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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.ExpenseViewModel
import com.example.expensetracker.data.ExpenseEntity
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

@Composable
fun DashboardScreen(viewModel: ExpenseViewModel, onEditExpense: (Int) -> Unit) {
    val expenses by viewModel.allExpenses.collectAsState()
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var selectedPeriod by remember { mutableStateOf(TrendPeriod.DAY) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Dashboard", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No expenses logged yet.", color = Color.Gray)
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
                        
                        // Beautiful Custom Segmented Selector Switch
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

                items(expenses) { expense ->
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
