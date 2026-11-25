package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FarmerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val preferredLanguage: String = "en",
    val selectedCropId: String = "sugarcane",
    val preferredVehicleRate: Double = 22.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedHarvestLot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val cropId: String = "",
    val cropName: String = "",
    val mandiName: String = "",
    val quantityQuintals: Double = 0.0,
    val modalPricePerQuintal: Double = 0.0,
    val netProfit: Double = 0.0,
    val transportCost: Double = 0.0,
    val verdict: String = "SELL_NOW",
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
