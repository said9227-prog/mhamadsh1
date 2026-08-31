package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object FormatUtils {
    private val decimalSymbols = DecimalFormatSymbols(Locale.US)
    private val cleanFormat = DecimalFormat("0.##", decimalSymbols)

    /**
     * Formats a number cleanly without unnecessary trailing zeros.
     * e.g. 12000.00 -> "12000"
     * e.g. 12000.50 -> "12000.5"
     * e.g. 12000.75 -> "12000.75"
     * e.g. 0.0 -> "0"
     */
    fun formatAmount(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        return cleanFormat.format(value)
    }

    fun formatAmount(value: Float): String = formatAmount(value.toDouble())
    fun formatAmount(value: Number): String = formatAmount(value.toDouble())
    fun formatAmount(value: Int): String = value.toString()
    fun formatAmount(value: Long): String = value.toString()

    fun formatWithCurrency(value: Double, currency: String): String {
        return "${formatAmount(value)} $currency"
    }

    fun formatWithCurrency(value: Float, currency: String): String {
        return "${formatAmount(value)} $currency"
    }
}

// Global extension functions for convenient access across Jetpack Compose UI
fun Double.toCleanString(): String = FormatUtils.formatAmount(this)
fun Float.toCleanString(): String = FormatUtils.formatAmount(this)
fun Number.toCleanString(): String = FormatUtils.formatAmount(this)
