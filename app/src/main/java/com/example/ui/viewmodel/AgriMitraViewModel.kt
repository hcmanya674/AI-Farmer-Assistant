package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.core.AppLanguage
import com.example.core.AuthSession
import com.example.core.VoiceSpeakerHelper
import com.example.data.local.AgriMitraDatabase
import com.example.data.local.CropCatalog
import com.example.data.local.FarmerPreferences
import com.example.data.local.FarmerPreferencesRepository
import com.example.data.remote.GeminiAdvisorService
import com.example.data.remote.SavedHarvestLot
import com.example.data.remote.WebMarketSearchService
import com.example.data.repository.FarmerCloudRepository
import com.example.data.repository.LocalAuthRepository
import com.example.data.repository.MandiRepository
import com.example.domain.engine.MarketProfitCalculator
import com.example.domain.engine.PriceForecastEngine
import com.example.domain.model.AiAdviceCard
import com.example.domain.model.Crop
import com.example.domain.model.CropCategory
import com.example.domain.model.DataSourceMode
import com.example.domain.model.ForecastHorizon
import com.example.domain.model.MandiPrice
import com.example.domain.model.MarketComparisonResult
import com.example.domain.model.PricePrediction
import com.example.domain.model.QuantityUnit
import com.example.domain.model.WebSourceLink
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AgriMitraViewModel"

sealed interface AiAdvisorState {
    data object Idle : AiAdvisorState
    data object Loading : AiAdvisorState
    data class Success(val advice: AiAdviceCard) : AiAdvisorState
    data class Error(val message: String) : AiAdvisorState
}

sealed interface FollowUpState {
    data object Idle : FollowUpState
    data object Loading : FollowUpState
    data class Streaming(val text: String) : FollowUpState
    data class Success(val text: String) : FollowUpState
    data class Error(val message: String) : FollowUpState
}

sealed interface SavedLotsState {
    data object Loading : SavedLotsState
    data class Success(val lots: List<SavedHarvestLot>) : SavedLotsState
    data class Error(val message: String) : SavedLotsState
}

data class AgriMitraUiState(
    val farmerName: String = "Farmer",
    val allCrops: List<Crop> = CropCatalog.allCrops,
    val filteredCrops: List<Crop> = CropCatalog.allCrops,
    val selectedCategory: CropCategory? = null,
    val topPriceByCrop: Map<String, MandiPrice> = emptyMap(),
    val searchQuery: String = "",
    val selectedCrop: Crop = CropCatalog.allCrops.first(),
    val quantityQuintals: Double = 80.0,
    val quantityUnit: QuantityUnit = QuantityUnit.QUINTALS,
    val transportCostPerKm: Double = 22.0,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val horizon: ForecastHorizon = ForecastHorizon.DAYS_7,
    val rankedMarkets: List<MarketComparisonResult> = emptyList(),
    val webSources: List<WebSourceLink> = emptyList(),
    val prediction: PricePrediction? = null,
    val dataSourceMode: DataSourceMode = DataSourceMode.SAVED_ON_PHONE,
    val syncStatusMessage: String = "Saved on Phone",
    val isSyncing: Boolean = false,
    val isSavingPlan: Boolean = false,
    val planStatusMessage: String? = null
)

private data class SyncState(
    val mode: DataSourceMode = DataSourceMode.SAVED_ON_PHONE,
    val message: String = "Saved on Phone",
    val isSyncing: Boolean = false
)

private data class ControlBundle(
    val prefs: FarmerPreferences,
    val query: String,
    val category: CropCategory?,
    val horizon: ForecastHorizon,
    val sync: SyncState
)

@OptIn(ExperimentalCoroutinesApi::class)
class AgriMitraViewModel(
    application: Application,
    private val session: AuthSession,
    private val cloudRepository: FarmerCloudRepository?,
    private val localAuthRepository: LocalAuthRepository
) : AndroidViewModel(application) {

    private val database = AgriMitraDatabase.getInstance(application)
    private val preferencesRepo = FarmerPreferencesRepository(application)
    private val mandiRepository = MandiRepository(
        mandiDao = database.mandiDao(),
        webSearchService = WebMarketSearchService()
    )
    private val geminiAdvisorService = GeminiAdvisorService()
    private val voiceSpeaker = VoiceSpeakerHelper(application)

    val isSpeaking: StateFlow<Boolean> = voiceSpeaker.isSpeaking

    private val searchQueryFlow = MutableStateFlow("")
    private val categoryFilterFlow = MutableStateFlow<CropCategory?>(null)
    private val horizonFlow = MutableStateFlow(ForecastHorizon.DAYS_7)
    private val syncStateFlow = MutableStateFlow(SyncState())
    private val planFeedbackFlow = MutableStateFlow<Pair<Boolean, String?>>(false to null)

    private val _aiAdviceState = MutableStateFlow<AiAdvisorState>(AiAdvisorState.Idle)
    val aiAdviceState: StateFlow<AiAdvisorState> = _aiAdviceState.asStateFlow()

    private val _followUpState = MutableStateFlow<FollowUpState>(FollowUpState.Idle)
    val followUpState: StateFlow<FollowUpState> = _followUpState.asStateFlow()

    private val rawSavedLotsFlow = when (session) {
        is AuthSession.Google -> cloudRepository!!.observeSavedLots()
        is AuthSession.Local -> localAuthRepository.observeSavedLots(session.username)
    }

    val savedLotsState: StateFlow<SavedLotsState> = rawSavedLotsFlow
        .map<List<SavedHarvestLot>, SavedLotsState> { SavedLotsState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(SavedLotsState.Error(rootError.localizedMessage ?: "Failed to load saved plans"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SavedLotsState.Loading
        )

    private var lastAdviceCacheKey: String? = null
    private val sessionAdviceCache = mutableMapOf<String, AiAdviceCard>()

    private val selectedCropIdFlow = preferencesRepo.preferencesFlow
        .map { it.selectedCropId }
        .distinctUntilChanged()

    private val mandiPricesFlow = selectedCropIdFlow.flatMapLatest { cropId ->
        mandiRepository.observeMandisForCrop(cropId)
    }

    private val historyFlow = selectedCropIdFlow.flatMapLatest { cropId ->
        mandiRepository.observeHistoricalSeries(cropId)
    }

    private val webSourcesFlow = selectedCropIdFlow.flatMapLatest { cropId ->
        mandiRepository.observeWebSources(cropId)
    }

    private val topPricesFlow = mandiRepository.observeTopModalPriceByCrop()

    private val baseControlsFlow = combine(
        preferencesRepo.preferencesFlow,
        searchQueryFlow,
        categoryFilterFlow,
        horizonFlow,
        syncStateFlow
    ) { prefs: FarmerPreferences, query: String, category: CropCategory?, horizon: ForecastHorizon, sync: SyncState ->
        ControlBundle(prefs, query, category, horizon, sync)
    }

    private val marketBundleFlow = combine(
        mandiPricesFlow,
        historyFlow,
        webSourcesFlow,
        topPricesFlow
    ) { mandis, history, sources, topMap ->
        Quadruple(mandis, history, sources, topMap)
    }

    val uiState: StateFlow<AgriMitraUiState> = combine(
        baseControlsFlow,
        marketBundleFlow,
        planFeedbackFlow
    ) { controls, (mandis, history, sources, topMap), (savingPlan, planMessage) ->
        val prefs = controls.prefs
        val query = controls.query
        val category = controls.category
        val horizon = controls.horizon
        val sync = controls.sync

        val selectedCrop = CropCatalog.byId(prefs.selectedCropId)
        val trimmedQuery = query.trim().lowercase()
        val filtered = CropCatalog.allCrops.filter { crop ->
            val matchesCategory = category == null || crop.category == category
            val matchesQuery = trimmedQuery.isEmpty() ||
                crop.nameEn.lowercase().contains(trimmedQuery) ||
                crop.nameKn.lowercase().contains(trimmedQuery) ||
                crop.nameHi.lowercase().contains(trimmedQuery) ||
                crop.nameMr.lowercase().contains(trimmedQuery) ||
                crop.variety.lowercase().contains(trimmedQuery)
            matchesCategory && matchesQuery
        }

        val ranked = MarketProfitCalculator.calculateAndRank(
            mandiPrices = mandis,
            quantityQuintals = prefs.quantityQuintals,
            transportCostPerKm = prefs.transportCostPerKm
        )

        val currentBestModal = ranked.firstOrNull()?.mandiPrice?.modalPricePerQuintal
            ?: mandis.maxOfOrNull { it.modalPricePerQuintal }
            ?: 0.0

        val prediction = if (currentBestModal > 0.0) {
            PriceForecastEngine.generateForecast(
                crop = selectedCrop,
                historicalPrices = history,
                currentBestModalPrice = currentBestModal,
                quantityQuintals = prefs.quantityQuintals,
                horizon = horizon
            )
        } else null

        val effectiveMode = if (mandis.any { it.isLiveWeb }) {
            DataSourceMode.LIVE_WEB_SEARCH
        } else {
            sync.mode
        }

        val displayName = session.displayName
            .trim()
            .substringBefore(" ")
            .takeIf { it.isNotBlank() }
            ?: "Farmer"

        AgriMitraUiState(
            farmerName = displayName,
            allCrops = CropCatalog.allCrops,
            filteredCrops = filtered,
            selectedCategory = category,
            topPriceByCrop = topMap,
            searchQuery = query,
            selectedCrop = selectedCrop,
            quantityQuintals = prefs.quantityQuintals,
            quantityUnit = prefs.quantityUnit,
            transportCostPerKm = prefs.transportCostPerKm,
            language = prefs.language,
            horizon = horizon,
            rankedMarkets = ranked,
            webSources = sources,
            prediction = prediction,
            dataSourceMode = effectiveMode,
            syncStatusMessage = sync.message,
            isSyncing = sync.isSyncing,
            isSavingPlan = savingPlan,
            planStatusMessage = planMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AgriMitraUiState()
    )

    init {
        viewModelScope.launch {
            mandiRepository.ensureSeeded()
            val prefs = preferencesRepo.preferencesFlow.first()
            if (session is AuthSession.Google) {
                val fullName = session.displayName.takeIf { it.isNotBlank() } ?: "Farmer"
                cloudRepository?.ensureFarmerProfile(
                    displayName = fullName,
                    preferredLanguage = prefs.language,
                    selectedCropId = prefs.selectedCropId,
                    preferredVehicleRate = prefs.transportCostPerKm
                )
            }
            refreshCurrentCropPrices()
        }
    }

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun selectCategory(category: CropCategory?) {
        categoryFilterFlow.value = category
    }

    fun selectCrop(crop: Crop) {
        voiceSpeaker.stop()
        planFeedbackFlow.value = false to null
        viewModelScope.launch {
            preferencesRepo.updateSelectedCrop(crop.id, crop.defaultQuantityQuintals)
            val prefs = preferencesRepo.preferencesFlow.first()
            cloudRepository?.updateFarmerPreferences(
                preferredLanguage = prefs.language,
                selectedCropId = crop.id,
                preferredVehicleRate = prefs.transportCostPerKm
            )
            _followUpState.value = FollowUpState.Idle
            refreshCurrentCropPrices()
            refreshAiAdviceForCurrentSelection(forceRefresh = false)
        }
    }

    fun updateQuantityQuintals(quantityQuintals: Double) {
        planFeedbackFlow.value = false to null
        viewModelScope.launch {
            preferencesRepo.updateQuantity(quantityQuintals)
        }
    }

    fun updateQuantityUnit(unit: QuantityUnit) {
        viewModelScope.launch {
            preferencesRepo.updateQuantityUnit(unit)
        }
    }

    fun updateTransportRate(ratePerKm: Double) {
        planFeedbackFlow.value = false to null
        viewModelScope.launch {
            preferencesRepo.updateTransportCostPerKm(ratePerKm)
            val prefs = preferencesRepo.preferencesFlow.first()
            cloudRepository?.updateFarmerPreferences(
                preferredLanguage = prefs.language,
                selectedCropId = prefs.selectedCropId,
                preferredVehicleRate = ratePerKm
            )
        }
    }

    fun selectLanguage(language: AppLanguage) {
        voiceSpeaker.stop()
        viewModelScope.launch {
            preferencesRepo.updateLanguage(language)
            val prefs = preferencesRepo.preferencesFlow.first()
            cloudRepository?.updateFarmerPreferences(
                preferredLanguage = language,
                selectedCropId = prefs.selectedCropId,
                preferredVehicleRate = prefs.transportCostPerKm
            )
            _followUpState.value = FollowUpState.Idle
            refreshAiAdviceForCurrentSelection(forceRefresh = false)
        }
    }

    fun selectHorizon(horizon: ForecastHorizon) {
        voiceSpeaker.stop()
        horizonFlow.value = horizon
        refreshAiAdviceForCurrentSelection(forceRefresh = false)
    }

    fun refreshCurrentCropPrices() {
        val cropId = uiState.value.selectedCrop.id
        viewModelScope.launch {
            syncStateFlow.value = syncStateFlow.value.copy(isSyncing = true)
            val result = mandiRepository.refreshCropPricesFromWeb(cropId)
            syncStateFlow.value = SyncState(
                mode = result.sourceMode,
                message = result.message,
                isSyncing = false
            )
        }
    }

    fun saveCurrentBestPlan(savedMessage: String) {
        val state = uiState.value
        val best = state.rankedMarkets.firstOrNull() ?: return
        val verdictName = state.prediction?.verdict?.name ?: "SELL_NOW"
        val cropLocalized = state.selectedCrop.localizedName(state.language)
        val notes = "${best.mandiPrice.district}, ${best.mandiPrice.state}"
        viewModelScope.launch {
            planFeedbackFlow.value = true to null
            val res = when (session) {
                is AuthSession.Google -> cloudRepository!!.saveHarvestLot(
                    cropId = state.selectedCrop.id,
                    cropName = cropLocalized,
                    mandiName = best.mandiPrice.mandiName,
                    quantityQuintals = state.quantityQuintals,
                    modalPricePerQuintal = best.mandiPrice.modalPricePerQuintal,
                    netProfit = best.netProfit,
                    transportCost = best.transportCost,
                    verdict = verdictName,
                    notes = notes
                )
                is AuthSession.Local -> localAuthRepository.saveHarvestLot(
                    ownerUsername = session.username,
                    cropId = state.selectedCrop.id,
                    cropName = cropLocalized,
                    mandiName = best.mandiPrice.mandiName,
                    quantityQuintals = state.quantityQuintals,
                    modalPricePerQuintal = best.mandiPrice.modalPricePerQuintal,
                    netProfit = best.netProfit,
                    transportCost = best.transportCost,
                    verdict = verdictName,
                    notes = notes
                )
            }
            planFeedbackFlow.value = if (res.isSuccess) {
                false to savedMessage
            } else {
                false to (res.exceptionOrNull()?.localizedMessage ?: "Could not save plan right now.")
            }
        }
    }

    fun loadSavedLot(lot: SavedHarvestLot) {
        voiceSpeaker.stop()
        planFeedbackFlow.value = false to null
        val crop = CropCatalog.byId(lot.cropId)
        val qty = lot.quantityQuintals.takeIf { it > 0.0 } ?: crop.defaultQuantityQuintals
        viewModelScope.launch {
            preferencesRepo.updateSelectedCrop(crop.id, qty)
            _followUpState.value = FollowUpState.Idle
            refreshCurrentCropPrices()
        }
    }

    fun deleteSavedLot(lotId: String) {
        viewModelScope.launch {
            when (session) {
                is AuthSession.Google -> cloudRepository!!.deleteSavedLot(lotId)
                is AuthSession.Local -> localAuthRepository.deleteSavedLot(session.username, lotId)
            }
        }
    }

    fun clearPlanFeedback() {
        planFeedbackFlow.value = false to null
    }

    fun ensureAiAdviceLoaded() {
        val state = uiState.value
        val key = "${state.selectedCrop.id}_${state.horizon.days}_${state.language.code}"
        if (lastAdviceCacheKey != key || _aiAdviceState.value is AiAdvisorState.Idle) {
            refreshAiAdviceForCurrentSelection(forceRefresh = false)
        }
    }

    fun refreshAiAdviceForCurrentSelection(forceRefresh: Boolean = true) {
        val state = uiState.value
        val prediction = state.prediction ?: return
        val key = "${state.selectedCrop.id}_${state.horizon.days}_${state.language.code}"

        if (!forceRefresh) {
            sessionAdviceCache[key]?.let { cached ->
                lastAdviceCacheKey = key
                _aiAdviceState.value = AiAdvisorState.Success(cached)
                return
            }
        }

        lastAdviceCacheKey = key
        viewModelScope.launch {
            _aiAdviceState.value = AiAdvisorState.Loading
            try {
                val advice = geminiAdvisorService.generateStructuredAdvice(
                    crop = state.selectedCrop,
                    rankedMarkets = state.rankedMarkets,
                    prediction = prediction,
                    webSources = state.webSources,
                    language = state.language
                )
                sessionAdviceCache[key] = advice
                _aiAdviceState.value = AiAdvisorState.Success(advice)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI advice generation timed out", e)
                _aiAdviceState.value = AiAdvisorState.Error(
                    "Advice check took too long. Check your internet and tap Try Again."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI advice generation failed", e)
                _aiAdviceState.value = AiAdvisorState.Error(
                    e.localizedMessage ?: "Unable to connect to AgriMitra AI right now."
                )
            }
        }
    }

    fun askFollowUpQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return
        val state = uiState.value
        val prediction = state.prediction ?: return

        viewModelScope.launch {
            _followUpState.value = FollowUpState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    geminiAdvisorService.askFollowUpStream(
                        question = trimmed,
                        crop = state.selectedCrop,
                        bestMandi = state.rankedMarkets.firstOrNull(),
                        prediction = prediction,
                        language = state.language
                    ).collect { chunk ->
                        accumulated.append(chunk)
                        if (accumulated.isNotEmpty()) {
                            _followUpState.value = FollowUpState.Streaming(accumulated.toString())
                        }
                    }
                }
                if (accumulated.isNotBlank()) {
                    _followUpState.value = FollowUpState.Success(accumulated.toString())
                } else {
                    _followUpState.value = FollowUpState.Error("AgriMitra AI returned an empty answer.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Follow-up streaming timed out", e)
                _followUpState.value = FollowUpState.Error("Request timed out. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Follow-up streaming failed", e)
                _followUpState.value = FollowUpState.Error(
                    e.localizedMessage ?: "Failed to get AI response."
                )
            }
        }
    }

    fun toggleSpeakAdvice(advice: AiAdviceCard) {
        if (voiceSpeaker.isSpeaking.value) {
            voiceSpeaker.stop()
        } else {
            val script = buildString {
                append(advice.headlineVerdict)
                append(". ")
                advice.bullets.forEach { bullet ->
                    append(bullet)
                    append(". ")
                }
                if (advice.riskNote.isNotBlank()) {
                    append(advice.riskNote)
                }
            }
            voiceSpeaker.speak(script, advice.language)
        }
    }

    override fun onCleared() {
        voiceSpeaker.release()
        super.onCleared()
    }

    companion object {
        fun provideFactory(application: Application, session: AuthSession): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val cloudRepo = if (session is AuthSession.Google) {
                        val db = FirebaseFirestore.getInstance(
                            application.getString(R.string.firestore_database_id)
                        )
                        FarmerCloudRepository(db)
                    } else null
                    val localRepo = LocalAuthRepository(
                        AgriMitraDatabase.getInstance(application).farmerAccountDao()
                    )
                    return AgriMitraViewModel(
                        application = application,
                        session = session,
                        cloudRepository = cloudRepo,
                        localAuthRepository = localRepo
                    ) as T
                }
            }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
