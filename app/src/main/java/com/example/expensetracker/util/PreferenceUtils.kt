package com.example.expensetracker.util

import android.content.Context

object PreferenceUtils {
    private const val PREFS_NAME = "expense_tracker_prefs"
    
    // Notification keys
    private const val KEY_REMINDER_ENABLED = "reminder_enabled"
    private const val KEY_REMINDER_HOUR = "reminder_hour"
    private const val KEY_REMINDER_MINUTE = "reminder_minute"
    
    // Category keys
    private const val KEY_CATEGORIES = "custom_categories"
    private val DEFAULT_CATEGORIES = setOf("Food", "Transport", "Utilities", "Entertainment", "Shopping", "Other")

    fun isReminderEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_REMINDER_ENABLED, true)
    }

    fun setReminderEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REMINDER_ENABLED, enabled)
            .apply()
    }

    fun getReminderHour(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_REMINDER_HOUR, 20) // Default 8 PM (20)
    }

    fun getReminderMinute(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_REMINDER_MINUTE, 30) // Default 30
    }

    fun setReminderTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
    }

    fun getCategories(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val categoriesSet = prefs.getStringSet(KEY_CATEGORIES, null)
        
        if (categoriesSet == null) {
            // First run: save and return default categories
            prefs.edit().putStringSet(KEY_CATEGORIES, DEFAULT_CATEGORIES).apply()
            return DEFAULT_CATEGORIES.sorted()
        }
        
        return categoriesSet.sorted()
    }

    fun addCategory(context: Context, category: String): Boolean {
        val trimmed = category.trim()
        if (trimmed.isEmpty()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val categoriesSet = prefs.getStringSet(KEY_CATEGORIES, DEFAULT_CATEGORIES) ?: DEFAULT_CATEGORIES
        val newSet = categoriesSet.toMutableSet()
        if (newSet.add(trimmed)) {
            prefs.edit().putStringSet(KEY_CATEGORIES, newSet).apply()
            return true
        }
        return false
    }

    fun deleteCategory(context: Context, category: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val categoriesSet = prefs.getStringSet(KEY_CATEGORIES, DEFAULT_CATEGORIES) ?: DEFAULT_CATEGORIES
        val newSet = categoriesSet.toMutableSet()
        if (newSet.remove(category)) {
            prefs.edit().putStringSet(KEY_CATEGORIES, newSet).apply()
            return true
        }
        return false
    }
}
