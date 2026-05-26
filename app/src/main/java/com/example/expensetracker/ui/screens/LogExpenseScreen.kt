package com.example.expensetracker.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

val predefinedCategories = listOf("Food", "Transport", "Utilities", "Entertainment", "Shopping", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogExpenseScreen(
    viewModel: ExpenseViewModel,
    expenseId: Int? = null,
    onNavigateBack: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(predefinedCategories.first()) }
    var expanded by remember { mutableStateOf(false) }
    
    // Date tracking
    val calendar = Calendar.getInstance()
    var selectedDateMillis by remember { mutableStateOf(calendar.timeInMillis) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val isEditing = expenseId != null

    // Load existing expense for editing
    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            val expense = viewModel.getExpenseById(expenseId)
            if (expense != null) {
                amount = expense.amount.toString()
                description = expense.description
                category = expense.category
                selectedDateMillis = expense.timestamp
            }
        }
    }

    val context = LocalContext.current
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(year, month, dayOfMonth)
            selectedDateMillis = newCalendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(if (isEditing) "Edit Expense" else "Log Expense", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Amount Input
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Text("₹", modifier = Modifier.padding(start = 12.dp)) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Category Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                predefinedCategories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            category = cat
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Description Input
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Date Picker
        OutlinedTextField(
            value = dateFormat.format(Date(selectedDateMillis)),
            onValueChange = {},
            readOnly = true,
            label = { Text("Date") },
            trailingIcon = {
                IconButton(onClick = { datePickerDialog.show() }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = {
                val amountDouble = amount.toDoubleOrNull() ?: 0.0
                if (amountDouble > 0) {
                    if (isEditing) {
                        viewModel.updateExpense(
                            com.example.expensetracker.data.ExpenseEntity(
                                id = expenseId!!,
                                amount = amountDouble,
                                category = category,
                                description = description,
                                timestamp = selectedDateMillis
                            )
                        )
                    } else {
                        viewModel.addExpense(
                            amount = amountDouble,
                            category = category,
                            description = description,
                            timestamp = selectedDateMillis
                        )
                    }
                    onNavigateBack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(if (isEditing) "Update Expense" else "Save Expense", fontSize = 18.sp)
        }
    }
}
