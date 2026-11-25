package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MandiDao {
    @Query("SELECT * FROM mandi_prices")
    fun observeAllMandiPrices(): Flow<List<MandiPriceEntity>>

    @Query("SELECT * FROM mandi_prices WHERE cropId = :cropId")
    fun observeMandiPricesForCrop(cropId: String): Flow<List<MandiPriceEntity>>

    @Query("SELECT COUNT(*) FROM mandi_prices")
    suspend fun getMandiCount(): Int

    @Query("SELECT DISTINCT cropId FROM mandi_prices")
    suspend fun getSeededCropIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMandiPrices(prices: List<MandiPriceEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingMandiPrices(prices: List<MandiPriceEntity>)

    @Query("DELETE FROM mandi_prices WHERE cropId = :cropId AND isLiveWeb = 1")
    suspend fun clearLivePricesForCrop(cropId: String)

    @Query("SELECT * FROM historical_prices WHERE cropId = :cropId ORDER BY dayOffset ASC")
    fun observeHistoryForCrop(cropId: String): Flow<List<HistoricalPriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistoricalPrices(history: List<HistoricalPriceEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissingHistoricalPrices(history: List<HistoricalPriceEntity>)

    @Query("SELECT * FROM web_sources WHERE cropId = :cropId")
    fun observeWebSourcesForCrop(cropId: String): Flow<List<WebSourceEntity>>

    @Query("DELETE FROM web_sources WHERE cropId = :cropId")
    suspend fun clearWebSourcesForCrop(cropId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebSources(sources: List<WebSourceEntity>)
}

@Dao
interface FarmerAccountDao {
    @Query("SELECT * FROM farmer_accounts WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): FarmerAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: FarmerAccountEntity)

    @Query("SELECT * FROM local_saved_lots WHERE ownerUsername = :ownerUsername ORDER BY updatedAtMs DESC")
    fun observeLocalSavedLots(ownerUsername: String): Flow<List<LocalSavedLotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalSavedLot(lot: LocalSavedLotEntity)

    @Query("DELETE FROM local_saved_lots WHERE id = :lotId AND ownerUsername = :ownerUsername")
    suspend fun deleteLocalSavedLot(lotId: String, ownerUsername: String)
}
