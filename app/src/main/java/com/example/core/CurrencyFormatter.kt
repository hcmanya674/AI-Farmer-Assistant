package com.example.core

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

object CurrencyFormatter {
    private val indianLocale = Locale.forLanguageTag("en-IN")

    fun formatInr(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(indianLocale).apply {
            maximumFractionDigits = 0
        }
        val rounded = amount.roundToLong()
        return if (rounded < 0) {
            "-₹${formatter.format(kotlin.math.abs(rounded))}"
        } else {
            "₹${formatter.format(rounded)}"
        }
    }

    fun formatSignedInr(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(indianLocale).apply {
            maximumFractionDigits = 0
        }
        val rounded = amount.roundToLong()
        return when {
            rounded > 0 -> "+₹${formatter.format(rounded)}"
            rounded < 0 -> "-₹${formatter.format(kotlin.math.abs(rounded))}"
            else -> "₹0"
        }
    }

    fun formatPercent(pct: Double): String {
        val sign = if (pct > 0.0) "+" else ""
        return "$sign${String.format(Locale.US, "%.1f", pct)}%"
    }
}
