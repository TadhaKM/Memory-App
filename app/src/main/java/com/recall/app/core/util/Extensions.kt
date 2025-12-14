package com.recall.app.core.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Format timestamp to readable date string
 */
fun Long.toDateString(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * Format timestamp to readable date-time string
 */
fun Long.toDateTimeString(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * Check if timestamp is today
 */
fun Long.isToday(): Boolean {
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_YEAR)
    calendar.time = Date(this)
    val dateDay = calendar.get(Calendar.DAY_OF_YEAR)
    return today == dateDay
}

/**
 * Generate UUID string
 */
fun generateUUID(): String = UUID.randomUUID().toString()
