package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.AppLanguage
import com.example.domain.model.QuantityUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.farmerDataStore: DataStore<Preferences> by preferencesDataStore(name = "agrimitra_prefs")

data class FarmerPreferences(
    val selectedCropId: String = "sugarcane",
    val quantityQuintals: Double = 80.0,
    val quantityUnit: QuantityUnit = QuantityUnit.QUINTALS,
    val transportCostPerKm: Double = 22.0,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val activeLocalUsername: String? = null
)

class FarmerPreferencesRepository(private val context: Context) {

    private object Keys {
        val SELECTED_CROP_ID = stringPreferencesKey("selected_crop_id")
        val QUANTITY_QUINTALS = doublePreferencesKey("quantity_quintals")
        val QUANTITY_UNIT = stringPreferencesKey("quantity_unit")
        val TRANSPORT_COST_PER_KM = doublePreferencesKey("transport_cost_per_km")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val ACTIVE_LOCAL_USERNAME = stringPreferencesKey("active_local_username")
    }

    val preferencesFlow: Flow<FarmerPreferences> = context.farmerDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val unitName = prefs[Keys.QUANTITY_UNIT] ?: QuantityUnit.QUINTALS.name
            val unit = runCatching { QuantityUnit.valueOf(unitName) }.getOrDefault(QuantityUnit.QUINTALS)
            FarmerPreferences(
                selectedCropId = prefs[Keys.SELECTED_CROP_ID] ?: "sugarcane",
                quantityQuintals = prefs[Keys.QUANTITY_QUINTALS] ?: 80.0,
                quantityUnit = unit,
                transportCostPerKm = prefs[Keys.TRANSPORT_COST_PER_KM] ?: 22.0,
                language = AppLanguage.fromCode(prefs[Keys.LANGUAGE_CODE] ?: "en"),
                activeLocalUsername = prefs[Keys.ACTIVE_LOCAL_USERNAME]?.takeIf { it.isNotBlank() }
            )
        }

    suspend fun updateSelectedCrop(cropId: String, defaultQty: Double? = null) {
        context.farmerDataStore.edit { prefs ->
            prefs[Keys.SELECTED_CROP_ID] = cropId
            if (defaultQty != null) {
                prefs[Keys.QUANTITY_QUINTALS] = defaultQty
            }
        }
    }

    suspend fun updateQuantity(quantityQuintals: Double) {
        context.farmerDataStore.edit { prefs ->
            prefs[Keys.QUANTITY_QUINTALS] = quantityQuintals.coerceIn(0.5, 1000.0)
        }
    }

    suspend fun updateQuantityUnit(unit: QuantityUnit) {
        context.farmerDataStore.edit { prefs ->
            prefs[Keys.QUANTITY_UNIT] = unit.name
        }
    }

    suspend fun updateTransportCostPerKm(ratePerKm: Double) {
        context.farmerDataStore.edit { prefs ->
            prefs[Keys.TRANSPORT_COST_PER_KM] = ratePerKm.coerceIn(0.0, 250.0)
        }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.farmerDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = language.code
        }
    }

    suspend fun setActiveLocalUsername(username: String?) {
        context.farmerDataStore.edit { prefs ->
            if (username.isNullOrBlank()) {
                prefs.remove(Keys.ACTIVE_LOCAL_USERNAME)
            } else {
                prefs[Keys.ACTIVE_LOCAL_USERNAME] = username.trim().lowercase()
            }
        }
    }
}
