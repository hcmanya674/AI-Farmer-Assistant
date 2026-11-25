package com.example.data.repository

import android.util.Log
import com.example.data.local.CropCatalog
import com.example.data.local.HistoricalPriceEntity
import com.example.data.local.MandiDao
import com.example.data.local.MandiPriceEntity
import com.example.data.local.SeedMandiData
import com.example.data.local.WebSourceEntity
import com.example.data.remote.WebMarketSearchService
import com.example.domain.model.DataSourceMode
import com.example.domain.model.MandiPrice
import com.example.domain.model.WebSourceLink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val TAG = "MandiRepository"

data class SyncResult(
    val sourceMode: DataSourceMode,
    val message: String
)

class MandiRepository(
    private val mandiDao: MandiDao,
    private val webSearchService: WebMarketSearchService
) {

    suspend fun ensureSeeded() {
        val existingCropIds = mandiDao.getSeededCropIds().toSet()
        val now = System.currentTimeMillis()
        val missingMandis = SeedMandiData.buildInitialMandis(now)
            .filter { it.cropId !in existingCropIds }
        if (missingMandis.isNotEmpty()) {
            mandiDao.insertMissingMandiPrices(missingMandis)
            val missingHistory = SeedMandiData.buildInitialHistory()
                .filter { it.cropId !in existingCropIds }
            if (missingHistory.isNotEmpty()) {
                mandiDao.insertMissingHistoricalPrices(missingHistory)
            }
        }
    }

    fun observeTopModalPriceByCrop(): Flow<Map<String, MandiPrice>> =
        mandiDao.observeAllMandiPrices().map { entities ->
            entities
                .map { it.toDomain() }
                .groupBy { it.cropId }
                .mapValues { (_, list) -> list.maxBy { it.modalPricePerQuintal } }
        }

    fun observeMandisForCrop(cropId: String): Flow<List<MandiPrice>> =
        mandiDao.observeMandiPricesForCrop(cropId).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeHistoricalSeries(cropId: String): Flow<List<Double>> =
        mandiDao.observeHistoryForCrop(cropId).map { entities ->
            entities.sortedBy { it.dayOffset }.map { it.modalPricePerQuintal }
        }

    fun observeWebSources(cropId: String): Flow<List<WebSourceLink>> =
        mandiDao.observeWebSourcesForCrop(cropId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun refreshCropPricesFromWeb(cropId: String): SyncResult {
        ensureSeeded()
        val crop = CropCatalog.byId(cropId)
        val existingMandis = mandiDao.observeMandiPricesForCrop(cropId).first().map { it.toDomain() }

        return try {
            val webResult = webSearchService.fetchLivePricesFromWeb(
                crop = crop,
                existingMandis = existingMandis
            )

            if (webResult.mandis.isNotEmpty()) {
                val entities = webResult.mandis.map { m ->
                    MandiPriceEntity(
                        id = m.id,
                        cropId = m.cropId,
                        mandiName = m.mandiName,
                        district = m.district,
                        state = m.state,
                        distanceKm = m.distanceKm,
                        minPricePerQuintal = m.minPricePerQuintal,
                        maxPricePerQuintal = m.maxPricePerQuintal,
                        modalPricePerQuintal = m.modalPricePerQuintal,
                        arrivalsTonnes = m.arrivalsTonnes,
                        dayChangePercent = m.dayChangePercent,
                        updatedAtEpochMs = m.updatedAtEpochMs,
                        sourceSite = m.sourceSite,
                        isLiveWeb = true
                    )
                }
                mandiDao.clearLivePricesForCrop(cropId)
                mandiDao.upsertMandiPrices(entities)

                val topModal = webResult.mandis.maxOf { it.modalPricePerQuintal }
                mandiDao.upsertHistoricalPrices(
                    listOf(
                        HistoricalPriceEntity(
                            id = "${cropId}_d0",
                            cropId = cropId,
                            dayOffset = 0,
                            modalPricePerQuintal = topModal
                        )
                    )
                )

                if (webResult.sources.isNotEmpty()) {
                    mandiDao.clearWebSourcesForCrop(cropId)
                    mandiDao.insertWebSources(
                        webResult.sources.mapIndexed { idx, src ->
                            WebSourceEntity(
                                id = "${cropId}_src_$idx",
                                cropId = cropId,
                                title = src.title,
                                domain = src.domain,
                                uri = src.uri
                            )
                        }
                    )
                }

                SyncResult(
                    sourceMode = DataSourceMode.LIVE_WEB_SEARCH,
                    message = "Updated from Google & ${webResult.sources.size.coerceAtLeast(1)} market websites"
                )
            } else {
                SyncResult(
                    sourceMode = DataSourceMode.SAVED_ON_PHONE,
                    message = "Showing saved rates on phone"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Live Google web sync failed, falling back to saved Room cache", e)
            SyncResult(
                sourceMode = DataSourceMode.SAVED_ON_PHONE,
                message = "Saved rates on phone (offline ready)"
            )
        }
    }
}
