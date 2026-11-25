package com.example.domain.engine

import com.example.domain.model.Crop
import com.example.domain.model.ForecastHorizon
import com.example.domain.model.Perishability
import com.example.domain.model.PricePoint
import com.example.domain.model.PricePrediction
import com.example.domain.model.SellVerdict
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Time-series forecasting engine using Weighted Linear Regression + Damped Trend
 * + Crop Seasonal Elasticity + Perishability Risk Thresholding.
 */
object PriceForecastEngine {

    fun generateForecast(
        crop: Crop,
        historicalPrices: List<Double>, // oldest -> newest (today)
        currentBestModalPrice: Double,
        quantityQuintals: Double,
        horizon: ForecastHorizon
    ): PricePrediction {
        val cleanHistory = if (historicalPrices.size >= 7) {
            historicalPrices.takeLast(14)
        } else {
            List(7) { currentBestModalPrice }
        }

        // Anchor latest point to current best modal price
        val recent7 = cleanHistory.takeLast(6) + currentBestModalPrice
        val n = recent7.size

        // Weighted linear regression slope (more recent days carry higher weight)
        var sumW = 0.0
        var sumWX = 0.0
        var sumWY = 0.0
        var sumWXX = 0.0
        var sumWXY = 0.0

        recent7.forEachIndexed { idx, price ->
            val w = (idx + 1).toDouble()
            val x = idx.toDouble()
            sumW += w
            sumWX += w * x
            sumWY += w * price
            sumWXX += w * x * x
            sumWXY += w * x * price
        }

        val denom = (sumW * sumWXX) - (sumWX * sumWX)
        val rawDailySlope = if (denom != 0.0) {
            ((sumW * sumWXY) - (sumWX * sumWY)) / denom
        } else 0.0

        // Cap daily slope to +/- 1.8% of current price to avoid unrealistic runaway forecasts
        val maxDailyDelta = currentBestModalPrice * 0.018
        val clampedDailySlope = rawDailySlope.coerceIn(-maxDailyDelta, maxDailyDelta)

        // Seasonal bias based on crop market cycle
        val seasonalDailyLift = when (crop.id) {
            "sugarcane" -> currentBestModalPrice * 0.0014
            "arecanut" -> currentBestModalPrice * 0.0028
            "black_pepper" -> currentBestModalPrice * 0.0024
            "onion" -> currentBestModalPrice * 0.0032
            "tomato" -> -currentBestModalPrice * 0.0025 // peak arrival softening
            "paddy" -> currentBestModalPrice * 0.0012
            "wheat" -> currentBestModalPrice * 0.0018
            "maize" -> currentBestModalPrice * 0.0019
            "soybean" -> currentBestModalPrice * 0.0021
            "groundnut" -> currentBestModalPrice * 0.0022
            "coconut" -> currentBestModalPrice * 0.0018
            "turmeric" -> currentBestModalPrice * 0.0025
            "cotton" -> -currentBestModalPrice * 0.0008
            else -> 0.0
        }

        val blendedDailyTrend = (clampedDailySlope * 0.72) + (seasonalDailyLift * 0.28)

        // Historical volatility for confidence interval calculation
        val dailyReturns = recent7.zipWithNext { a, b -> if (a > 0) (b - a) / a else 0.0 }
        val meanReturn = dailyReturns.average()
        val variance = dailyReturns.map { (it - meanReturn) * (it - meanReturn) }.average()
        val stdDev = sqrt(variance).coerceIn(0.006, 0.055)

        val points = mutableListOf<PricePoint>()

        // Past 7 days (-6 .. 0)
        recent7.forEachIndexed { index, price ->
            val offset = index - (n - 1)
            val label = if (offset == 0) "Today" else "${offset}d"
            points.add(
                PricePoint(
                    dayOffset = offset,
                    label = label,
                    pricePerQuintal = price,
                    lowBound = price,
                    highBound = price,
                    isForecast = false
                )
            )
        }

        // Future horizon days (1 .. horizon.days) with damping factor phi = 0.93
        val phi = 0.93
        var cumulativeDampedMultiplier = 0.0
        var lastForecastPrice = currentBestModalPrice

        for (day in 1..horizon.days) {
            cumulativeDampedMultiplier += Math.pow(phi, day.toDouble())
            val cyclicWave = sin(day * 0.55) * (currentBestModalPrice * stdDev * 0.22)
            val projected = max(
                currentBestModalPrice * 0.65,
                currentBestModalPrice + (blendedDailyTrend * cumulativeDampedMultiplier) + cyclicWave
            )
            val bandSpread = currentBestModalPrice * stdDev * sqrt(day.toDouble()) * 0.65
            lastForecastPrice = projected

            val label = when {
                day == horizon.days -> "+${day}d"
                horizon == ForecastHorizon.DAYS_7 -> "+${day}d"
                day % 5 == 0 -> "+${day}d"
                else -> "+${day}d"
            }

            points.add(
                PricePoint(
                    dayOffset = day,
                    label = label,
                    pricePerQuintal = projected,
                    lowBound = max(0.0, projected - bandSpread),
                    highBound = projected + bandSpread,
                    isForecast = true
                )
            )
        }

        val changePercent = if (currentBestModalPrice > 0.0) {
            ((lastForecastPrice - currentBestModalPrice) / currentBestModalPrice) * 100.0
        } else 0.0

        val projectedGrossGain = (lastForecastPrice - currentBestModalPrice) * quantityQuintals

        // Perishability-aware decision rule
        val minRequiredGainPctToHold = when (crop.perishability) {
            Perishability.HIGH -> 11.0 // Tomatoes spoil fast; only hold if huge surge & short wait
            Perishability.MEDIUM -> 4.0
            Perishability.LOW -> 2.2
        }

        val verdict = when {
            crop.perishability == Perishability.HIGH && horizon.days > crop.perishability.maxSafeHoldDays -> {
                SellVerdict.SELL_NOW
            }
            changePercent >= minRequiredGainPctToHold -> SellVerdict.HOLD_WAIT
            else -> SellVerdict.SELL_NOW
        }

        val confidence = (92.0 - (stdDev * 350.0) - (horizon.days * 0.45))
            .roundToInt()
            .coerceIn(64, 94)

        val verdictSummary = when (verdict) {
            SellVerdict.HOLD_WAIT ->
                "Upward trend (+${String.format("%.1f", changePercent)}%) outweighs storage cost over ${horizon.days} days."
            SellVerdict.SELL_NOW ->
                if (crop.perishability == Perishability.HIGH && changePercent > 0) {
                    "High spoilage risk makes holding past ${crop.perishability.maxSafeHoldDays} days risky—lock in today's best Mandi rate."
                } else if (changePercent < 0) {
                    "Prices are softening (${String.format("%.1f", changePercent)}%). Selling now protects your margin."
                } else {
                    "Expected gain (+${String.format("%.1f", changePercent)}%) is modest; selling at today's peak Mandi maximizes certainty."
                }
        }

        return PricePrediction(
            crop = crop,
            horizon = horizon,
            currentModalPrice = currentBestModalPrice,
            predictedPrice = lastForecastPrice,
            expectedChangePercent = changePercent,
            projectedNetGainForQuantity = projectedGrossGain,
            verdict = verdict,
            confidencePercent = confidence,
            verdictSummary = verdictSummary,
            seriesPoints = points
        )
    }
}
