package com.example.expensetracker.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.expensetracker.data.ExpenseEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun generateCsvContent(expenses: List<ExpenseEntity>, startDate: Long, endDate: Long): String {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDisplay = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        
        // Filter by timestamp range (inclusive)
        val filtered = expenses.filter { it.timestamp in startDate..endDate }.sortedByDescending { it.timestamp }
        val totalSpent = filtered.sumOf { it.amount }
        
        val categoryTotals = filtered.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
        
        val sb = java.lang.StringBuilder()
        
        // Document Header
        sb.append("EXPENSE TRACKER REPORT\n")
        sb.append("Generated On,${sdfDisplay.format(Date())}\n")
        sb.append("Date Range,${sdfDisplay.format(Date(startDate))} to ${sdfDisplay.format(Date(endDate))}\n")
        sb.append("Total Spent,₹${String.format(Locale.US, "%,.2f", totalSpent)}\n\n")
        
        // Category Summary Section
        sb.append("CATEGORY SUMMARY\n")
        sb.append("Category,Total Spent,Percentage\n")
        for ((category, amount) in categoryTotals) {
            val pct = if (totalSpent > 0) (amount / totalSpent) * 100 else 0.0
            sb.append("$category,₹${String.format(Locale.US, "%,.2f", amount)},${String.format(Locale.US, "%.1f", pct)}%\n")
        }
        sb.append("\n")
        
        // Detailed Transactions Section
        sb.append("INDIVIDUAL TRANSACTIONS\n")
        sb.append("Date,Category,Description,Amount\n")
        for (expense in filtered) {
            val escapedDesc = expense.description.replace("\"", "\"\"")
            sb.append("${sdfDate.format(Date(expense.timestamp))},${expense.category},\"$escapedDesc\",₹${expense.amount}\n")
        }
        
        return sb.toString()
    }

    fun exportAndShareExpenses(context: Context, expenses: List<ExpenseEntity>, startDate: Long, endDate: Long) {
        try {
            val csvContent = generateCsvContent(expenses, startDate, endDate)
            val sdfFile = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val fileName = "Expense_Report_${sdfFile.format(Date(startDate))}_${sdfFile.format(Date(endDate))}.csv"
            
            val file = File(context.cacheDir, fileName)
            file.writeText(csvContent, Charsets.UTF_8)
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Expense Tracker Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(intent, "Share Expense Report")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: java.lang.Exception) {
            Toast.makeText(context, "Failed to export expenses: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
