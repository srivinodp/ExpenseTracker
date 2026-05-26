package com.example.expensetracker.ui

import com.example.expensetracker.data.ExpenseEntity
import java.util.Calendar
import java.util.Locale

enum class InsightType {
    TOTAL_SPENT,
    CATEGORY_SPIKE,
    CATEGORY_SAVINGS,
    MISSING_CATEGORY,
    LARGEST_EXPENSE,
    NO_DATA
}

enum class InsightSeverity {
    INFO, WARNING, SUCCESS
}

data class HeuristicInsight(
    val title: String,
    val description: String,
    val type: InsightType,
    val severity: InsightSeverity
)

object HeuristicEngine {
    fun generateInsights(expenses: List<ExpenseEntity>, now: Calendar = Calendar.getInstance()): List<HeuristicInsight> {
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) // 0-indexed

        // This Month Expenses
        val thisMonthExpenses = expenses.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
        }

        // Last Month Expenses
        val lastMonthCal = (now.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val lastMonthYear = lastMonthCal.get(Calendar.YEAR)
        val lastMonthVal = lastMonthCal.get(Calendar.MONTH)

        val lastMonthExpenses = expenses.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.YEAR) == lastMonthYear && cal.get(Calendar.MONTH) == lastMonthVal
        }

        val insights = mutableListOf<HeuristicInsight>()

        if (thisMonthExpenses.isEmpty()) {
            insights.add(
                HeuristicInsight(
                    title = "Start your tracking journey!",
                    description = "No expenses logged this month yet. Tap the '+' button below to log your first transaction.",
                    type = InsightType.NO_DATA,
                    severity = InsightSeverity.INFO
                )
            )
            return insights
        }

        val totalThisMonth = thisMonthExpenses.sumOf { it.amount }
        val totalLastMonth = lastMonthExpenses.sumOf { it.amount }

        // 1. Total Spent insight
        val totalSpentTitle = "Total Spent: ₹${String.format(Locale.US, "%,.2f", totalThisMonth)}"
        val totalSpentDesc = if (totalLastMonth > 0.0) {
            val diffPct = ((totalThisMonth - totalLastMonth) / totalLastMonth) * 100.0
            if (diffPct > 0) {
                "That's ${String.format(Locale.US, "%.1f", diffPct)}% more than last month (₹${String.format(Locale.US, "%,.2f", totalLastMonth)})."
            } else {
                "Great job! That's ${String.format(Locale.US, "%.1f", Math.abs(diffPct))}% less than last month (₹${String.format(Locale.US, "%,.2f", totalLastMonth)})."
            }
        } else {
            "First month tracking or no expenses logged last month."
        }
        
        val totalSpentSeverity = if (totalLastMonth > 0.0 && totalThisMonth > totalLastMonth) {
            InsightSeverity.WARNING
        } else if (totalLastMonth > 0.0) {
            InsightSeverity.SUCCESS
        } else {
            InsightSeverity.INFO
        }

        insights.add(
            HeuristicInsight(
                title = totalSpentTitle,
                description = totalSpentDesc,
                type = InsightType.TOTAL_SPENT,
                severity = totalSpentSeverity
            )
        )

        // 2. Category Spikes & Savings
        val categories = listOf("Food", "Transport", "Utilities", "Entertainment", "Shopping", "Other")
        categories.forEach { cat ->
            val thisMonthCat = thisMonthExpenses.filter { it.category == cat }.sumOf { it.amount }
            val lastMonthCat = lastMonthExpenses.filter { it.category == cat }.sumOf { it.amount }

            if (thisMonthCat > 0.0 && lastMonthCat > 0.0) {
                val pctChange = ((thisMonthCat - lastMonthCat) / lastMonthCat) * 100.0
                if (pctChange >= 10.0) {
                    insights.add(
                        HeuristicInsight(
                            title = "$cat expenses spiked!",
                            description = "Your $cat spending increased by ${String.format(Locale.US, "%.1f", pctChange)}% compared to last month.",
                            type = InsightType.CATEGORY_SPIKE,
                            severity = InsightSeverity.WARNING
                        )
                    )
                } else if (pctChange <= -15.0) {
                    insights.add(
                        HeuristicInsight(
                            title = "Smart savings on $cat!",
                            description = "You saved ${String.format(Locale.US, "%.1f", Math.abs(pctChange))}% on $cat compared to last month.",
                            type = InsightType.CATEGORY_SAVINGS,
                            severity = InsightSeverity.SUCCESS
                        )
                    )
                }
            }
        }

        // 3. Missing Standard Categories
        val checkCategories = listOf("Transport", "Food", "Shopping", "Entertainment")
        checkCategories.forEach { cat ->
            val thisMonthCat = thisMonthExpenses.filter { it.category == cat }.sumOf { it.amount }
            val lastMonthCat = lastMonthExpenses.filter { it.category == cat }.sumOf { it.amount }

            if (thisMonthCat == 0.0 && lastMonthCat > 0.0) {
                val title = if (cat == "Transport") "Staying in this month?" else "No $cat expenses logged!"
                val desc = if (cat == "Transport") {
                    "No Transport expenses logged this month compared to last month (₹${String.format(Locale.US, "%,.2f", lastMonthCat)}). You are traveling less!"
                } else {
                    "You spent ₹0.00 on $cat this month compared to last month (₹${String.format(Locale.US, "%,.2f", lastMonthCat)})."
                }
                insights.add(
                    HeuristicInsight(
                        title = title,
                        description = desc,
                        type = InsightType.MISSING_CATEGORY,
                        severity = InsightSeverity.INFO
                    )
                )
            }
        }

        // 4. Largest Category
        val catGroups = thisMonthExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> exp.amount } }
        if (catGroups.isNotEmpty()) {
            val largestCat = catGroups.maxByOrNull { it.value }
            if (largestCat != null && largestCat.value > 0.0) {
                val pct = (largestCat.value / totalThisMonth) * 100.0
                insights.add(
                    HeuristicInsight(
                        title = "${largestCat.key} is your top expense",
                        description = "It accounts for ${String.format(Locale.US, "%.1f", pct)}% of your monthly budget (₹${String.format(Locale.US, "%,.2f", largestCat.value)}).",
                        type = InsightType.LARGEST_EXPENSE,
                        severity = InsightSeverity.INFO
                    )
                )
            }
        }

        // Sort: Spikes first, then Missing categories, then Savings, then Total spent, then Largest category
        return insights.sortedWith(compareBy {
            when (it.type) {
                InsightType.NO_DATA -> 0
                InsightType.CATEGORY_SPIKE -> 1
                InsightType.MISSING_CATEGORY -> 2
                InsightType.CATEGORY_SAVINGS -> 3
                InsightType.TOTAL_SPENT -> 4
                InsightType.LARGEST_EXPENSE -> 5
            }
        })
    }
}
