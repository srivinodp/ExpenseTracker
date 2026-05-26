package com.example.expensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expensetracker.data.ExpenseDatabase
import com.example.expensetracker.ui.ExpenseViewModel
import com.example.expensetracker.ui.ExpenseViewModelFactory
import com.example.expensetracker.ui.screens.DashboardScreen
import com.example.expensetracker.ui.screens.LogExpenseScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val database = ExpenseDatabase.getDatabase(context)
    val viewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModelFactory(database.expenseDao())
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onAddExpense = {
                    navController.navigate("log_expense")
                },
                onEditExpense = { expenseId ->
                    navController.navigate("log_expense?expenseId=$expenseId")
                }
            )
        }
        composable(
            route = "log_expense?expenseId={expenseId}",
            arguments = listOf(
                navArgument("expenseId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getInt("expenseId") ?: -1
            LogExpenseScreen(
                viewModel = viewModel,
                expenseId = if (expenseId != -1) expenseId else null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

