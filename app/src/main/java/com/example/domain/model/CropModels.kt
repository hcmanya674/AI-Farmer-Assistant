package com.example.domain.model

import com.example.core.AppLanguage

enum class CropCategory {
    SPICE_PLANTATION,
    GRAIN,
    VEGETABLE,
    CASH_CROP
}

enum class Perishability(val label: String, val maxSafeHoldDays: Int) {
    LOW("Stores easily (30+ days)", 30),
    MEDIUM("Stores 10–15 days", 14),
    HIGH("Sell quickly (2–4 days)", 3)
}

enum class QuantityUnit {
    QUINTALS,
    BAGS_50KG
}

data class Crop(
    val id: String,
    val nameEn: String,
    val nameKn: String,
    val nameHi: String,
    val nameMr: String,
    val variety: String,
    val category: CropCategory,
    val perishability: Perishability,
    val seasonalFactorNote: String,
    val defaultQuantityQuintals: Double,
    val accentColorHex: Long
) {
    fun localizedName(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> nameEn
        AppLanguage.KANNADA -> nameKn
        AppLanguage.HINDI -> nameHi
        AppLanguage.MARATHI -> nameMr
    }
}

data class MandiPrice(
    val id: String,
    val cropId: String,
    val mandiName: String,
    val district: String,
    val state: String,
    val distanceKm: Double,
    val minPricePerQuintal: Double,
    val maxPricePerQuintal: Double,
    val modalPricePerQuintal: Double,
    val arrivalsTonnes: Double,
    val dayChangePercent: Double,
    val updatedAtEpochMs: Long,
    val sourceSite: String = "Multi-Web Mandi Index",
    val isLiveWeb: Boolean = false
)

data class WebSourceLink(
    val title: String,
    val domain: String,
    val uri: String
)

data class MarketComparisonResult(
    val mandiPrice: MandiPrice,
    val quantityQuintals: Double,
    val transportCostPerKm: Double,
    val grossRevenue: Double,
    val transportCost: Double,
    val netProfit: Double,
    val rank: Int,
    val isBestProfit: Boolean,
    val isClosest: Boolean,
    val extraProfitOverClosest: Double
)

enum class ForecastHorizon(val days: Int) {
    DAYS_7(7),
    DAYS_15(15),
    DAYS_30(30)
}

enum class SellVerdict {
    SELL_NOW,
    HOLD_WAIT
}

data class PricePoint(
    val dayOffset: Int,
    val label: String,
    val pricePerQuintal: Double,
    val lowBound: Double,
    val highBound: Double,
    val isForecast: Boolean
)

data class PricePrediction(
    val crop: Crop,
    val horizon: ForecastHorizon,
    val currentModalPrice: Double,
    val predictedPrice: Double,
    val expectedChangePercent: Double,
    val projectedNetGainForQuantity: Double,
    val verdict: SellVerdict,
    val confidencePercent: Int,
    val verdictSummary: String,
    val seriesPoints: List<PricePoint>
)

data class AiAdviceCard(
    val headlineVerdict: String,
    val bullets: List<String>,
    val marketDrivers: String,
    val riskNote: String,
    val sources: List<WebSourceLink> = emptyList(),
    val generatedAtEpochMs: Long,
    val language: AppLanguage
)

enum class DataSourceMode {
    LIVE_WEB_SEARCH,
    SAVED_ON_PHONE
}
