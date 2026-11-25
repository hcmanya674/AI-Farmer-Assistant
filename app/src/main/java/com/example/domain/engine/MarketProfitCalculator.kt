package com.example.domain.engine

import com.example.domain.model.MandiPrice
import com.example.domain.model.MarketComparisonResult

/**
 * Calculates Net Realizable Profit for each Mandi using:
 * Net Profit = (Market Price per Quintal * Quantity) - (Distance in km * Transport Cost per km)
 * and ranks markets descending by Net Profit.
 */
object MarketProfitCalculator {

    fun calculateAndRank(
        mandiPrices: List<MandiPrice>,
        quantityQuintals: Double,
        transportCostPerKm: Double
    ): List<MarketComparisonResult> {
        if (mandiPrices.isEmpty()) return emptyList()

        val safeQuantity = quantityQuintals.coerceAtLeast(0.5)
        val safeRate = transportCostPerKm.coerceAtLeast(0.0)

        val closestMandiId = mandiPrices.minByOrNull { it.distanceKm }?.id

        val preliminary = mandiPrices.map { mandi ->
            val grossRevenue = mandi.modalPricePerQuintal * safeQuantity
            val transportCost = mandi.distanceKm * safeRate
            val netProfit = grossRevenue - transportCost
            Triple(mandi, grossRevenue, transportCost to netProfit)
        }.sortedByDescending { it.third.second }

        val closestNetProfit = preliminary
            .firstOrNull { it.first.id == closestMandiId }
            ?.third?.second
            ?: preliminary.last().third.second

        return preliminary.mapIndexed { index, (mandi, gross, costs) ->
            val (transportCost, netProfit) = costs
            MarketComparisonResult(
                mandiPrice = mandi,
                quantityQuintals = safeQuantity,
                transportCostPerKm = safeRate,
                grossRevenue = gross,
                transportCost = transportCost,
                netProfit = netProfit,
                rank = index + 1,
                isBestProfit = index == 0,
                isClosest = mandi.id == closestMandiId,
                extraProfitOverClosest = netProfit - closestNetProfit
            )
        }
    }
}
