package com.example

import com.example.data.local.CropCatalog
import com.example.data.local.SeedMandiData
import com.example.domain.engine.MarketProfitCalculator
import com.example.domain.engine.PriceForecastEngine
import com.example.domain.model.ForecastHorizon
import com.example.domain.model.MandiPrice
import com.example.domain.model.SellVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgriMitraEngineTest {

    @Test
    fun netProfitCalculationRanksFartherHigherPayingMandiAboveCloserLowerMandi() {
        val closeMandi = MandiPrice(
            id = "close",
            cropId = "arecanut",
            mandiName = "Close APMC",
            district = "Davanagere",
            state = "Karnataka",
            distanceKm = 15.0,
            minPricePerQuintal = 44000.0,
            maxPricePerQuintal = 48000.0,
            modalPricePerQuintal = 47000.0,
            arrivalsTonnes = 100.0,
            dayChangePercent = 1.0,
            updatedAtEpochMs = 0L
        )
        val bestMandi = MandiPrice(
            id = "best",
            cropId = "arecanut",
            mandiName = "Best APMC",
            district = "Shimoga",
            state = "Karnataka",
            distanceKm = 50.0,
            minPricePerQuintal = 46000.0,
            maxPricePerQuintal = 51000.0,
            modalPricePerQuintal = 49500.0,
            arrivalsTonnes = 140.0,
            dayChangePercent = 2.5,
            updatedAtEpochMs = 0L
        )

        val ranked = MarketProfitCalculator.calculateAndRank(
            mandiPrices = listOf(closeMandi, bestMandi),
            quantityQuintals = 10.0,
            transportCostPerKm = 20.0
        )

        assertEquals("best", ranked.first().mandiPrice.id)
        assertEquals(494000.0, ranked.first().netProfit, 0.01)
        assertEquals(469700.0, ranked[1].netProfit, 0.01)
        assertTrue(ranked.first().extraProfitOverClosest > 24000.0)
    }

    @Test
    fun highPerishabilityTomatoTriggersSellNowOnFallingTrend() {
        val tomato = CropCatalog.byId("tomato")
        val fallingHistory = listOf(2600.0, 2520.0, 2450.0, 2380.0, 2310.0, 2240.0, 2190.0)

        val prediction = PriceForecastEngine.generateForecast(
            crop = tomato,
            historicalPrices = fallingHistory,
            currentBestModalPrice = 2150.0,
            quantityQuintals = 15.0,
            horizon = ForecastHorizon.DAYS_7
        )

        assertEquals(SellVerdict.SELL_NOW, prediction.verdict)
        assertEquals(14, prediction.seriesPoints.size)
    }

    @Test
    fun allCatalogCropsHaveSeedMandisAndHistoricalSeriesIncludingSugarcane() {
        val cropIds = CropCatalog.allCrops.map { it.id }.toSet()
        assertTrue(cropIds.contains("sugarcane"))
        assertTrue(cropIds.contains("wheat"))
        assertTrue(cropIds.contains("maize"))
        assertTrue(cropIds.contains("soybean"))
        assertTrue(cropIds.contains("groundnut"))

        val mandisByCrop = SeedMandiData.buildInitialMandis(0L).groupBy { it.cropId }
        val historyByCrop = SeedMandiData.buildInitialHistory().groupBy { it.cropId }

        CropCatalog.allCrops.forEach { crop ->
            assertTrue("Missing mandis for ${crop.id}", (mandisByCrop[crop.id]?.size ?: 0) >= 3)
            assertTrue("Missing history for ${crop.id}", (historyByCrop[crop.id]?.size ?: 0) >= 7)
        }
    }

    @Test
    fun sugarcaneForecastRespectsPerishabilityAndCalculatesValidPrediction() {
        val sugarcane = CropCatalog.byId("sugarcane")
        val history = SeedMandiData.buildInitialHistory()
            .filter { it.cropId == "sugarcane" }
            .sortedBy { it.dayOffset }
            .map { it.modalPricePerQuintal }

        val prediction = PriceForecastEngine.generateForecast(
            crop = sugarcane,
            historicalPrices = history,
            currentBestModalPrice = 370.0,
            quantityQuintals = 80.0,
            horizon = ForecastHorizon.DAYS_15
        )

        assertEquals(SellVerdict.SELL_NOW, prediction.verdict)
        assertEquals(22, prediction.seriesPoints.size)
        assertTrue(prediction.predictedPrice > 0.0)
    }
}
