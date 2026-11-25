package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.MandiPrice
import com.example.domain.model.WebSourceLink

@Entity(tableName = "mandi_prices")
data class MandiPriceEntity(
    @PrimaryKey val id: String,
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
    val sourceSite: String,
    val isLiveWeb: Boolean
) {
    fun toDomain(): MandiPrice = MandiPrice(
        id = id,
        cropId = cropId,
        mandiName = mandiName,
        district = district,
        state = state,
        distanceKm = distanceKm,
        minPricePerQuintal = minPricePerQuintal,
        maxPricePerQuintal = maxPricePerQuintal,
        modalPricePerQuintal = modalPricePerQuintal,
        arrivalsTonnes = arrivalsTonnes,
        dayChangePercent = dayChangePercent,
        updatedAtEpochMs = updatedAtEpochMs,
        sourceSite = sourceSite,
        isLiveWeb = isLiveWeb
    )
}

@Entity(tableName = "historical_prices")
data class HistoricalPriceEntity(
    @PrimaryKey val id: String,
    val cropId: String,
    val dayOffset: Int,
    val modalPricePerQuintal: Double
)

@Entity(tableName = "web_sources")
data class WebSourceEntity(
    @PrimaryKey val id: String,
    val cropId: String,
    val title: String,
    val domain: String,
    val uri: String
) {
    fun toDomain(): WebSourceLink = WebSourceLink(
        title = title,
        domain = domain,
        uri = uri
    )
}

@Entity(tableName = "farmer_accounts")
data class FarmerAccountEntity(
    @PrimaryKey val username: String,
    val displayName: String,
    val saltHex: String,
    val passwordHashHex: String,
    val createdAtMs: Long
)

@Entity(tableName = "local_saved_lots")
data class LocalSavedLotEntity(
    @PrimaryKey val id: String,
    val ownerUsername: String,
    val cropId: String,
    val cropName: String,
    val mandiName: String,
    val quantityQuintals: Double,
    val modalPricePerQuintal: Double,
    val netProfit: Double,
    val transportCost: Double,
    val verdict: String,
    val notes: String?,
    val updatedAtMs: Long
)
