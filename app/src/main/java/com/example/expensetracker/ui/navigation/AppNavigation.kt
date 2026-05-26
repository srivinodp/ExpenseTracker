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

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object LogExpense : Screen("log_expense", "Log Expense", Icons.Default.Add)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val database = ExpenseDatabase.getDatabase(context)
    val viewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModelFactory(database.expenseDao())
    )

    val items = listOf(Screen.Dashboard, Screen.LogExpense)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { 
                            it.route == screen.route || it.route?.startsWith("${screen.route}?") == true 
                        } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
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
}
