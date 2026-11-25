package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.AppLanguage
import com.example.core.AuthSession
import com.example.core.configureAppCheck
import com.example.core.localizedContext
import com.example.core.signOutFarmer
import com.example.data.local.AgriMitraDatabase
import com.example.data.local.FarmerAccountEntity
import com.example.data.local.FarmerPreferencesRepository
import com.example.data.repository.LocalAuthRepository
import com.example.ui.components.AgriMitraTopBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CropSelectionScreen
import com.example.ui.screens.ForecastAdvisorScreen
import com.example.ui.screens.MarketComparisonScreen
import com.example.ui.theme.AgriMitraTheme
import com.example.ui.viewmodel.AgriMitraViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

const val ROUTE_CROPS = "crops"
const val ROUTE_COMPARE = "compare"
const val ROUTE_FORECAST = "forecast"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            AgriMitraTheme {
                AgriMitraRoot()
            }
        }
    }
}

@Composable
fun AgriMitraRoot() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val prefsRepo = remember(application) { FarmerPreferencesRepository(application) }
    val localAuthRepo = remember(application) {
        LocalAuthRepository(AgriMitraDatabase.getInstance(application).farmerAccountDao())
    }
    val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(initialValue = null)
    val selectedLanguage = prefs?.language ?: AppLanguage.ENGLISH
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    var localAccount by remember { mutableStateOf<FarmerAccountEntity?>(null) }
    var isResolvingLocalSession by remember { mutableStateOf(true) }
    val prefsLoaded = prefs != null
    val savedUsername = prefs?.activeLocalUsername

    LaunchedEffect(prefsLoaded, savedUsername) {
        if (!prefsLoaded) return@LaunchedEffect
        isResolvingLocalSession = true
        try {
            if (savedUsername.isNullOrBlank()) {
                localAccount = null
            } else {
                val found = localAuthRepo.findAccount(savedUsername)
                if (found == null) {
                    prefsRepo.setActiveLocalUsername(null)
                }
                localAccount = found
            }
        } finally {
            isResolvingLocalSession = false
        }
    }

    val localizedCtx = remember(context, selectedLanguage) {
        context.localizedContext(selectedLanguage)
    }

    val activeSession: AuthSession? = when {
        currentUser != null -> AuthSession.Google(
            uid = currentUser!!.uid,
            displayName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Farmer"
        )
        localAccount != null -> AuthSession.Local(
            username = localAccount!!.username,
            displayName = localAccount!!.displayName
        )
        else -> null
    }

    when {
        activeSession == null && (prefs == null || isResolvingLocalSession) -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        activeSession == null -> {
            CompositionLocalProvider(LocalContext provides localizedCtx) {
                AuthScreen(
                    selectedLanguage = selectedLanguage,
                    onSelectLanguage = { lang ->
                        scope.launch { prefsRepo.updateLanguage(lang) }
                    },
                    onGoogleAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                    },
                    onLocalAuthSuccess = { account ->
                        localAccount = account
                        scope.launch {
                            prefsRepo.setActiveLocalUsername(account.username)
                        }
                    }
                )
            }
        }
        else -> {
            key(activeSession.sessionKey) {
                val vm: AgriMitraViewModel = viewModel(
                    key = activeSession.sessionKey,
                    factory = AgriMitraViewModel.provideFactory(application, activeSession)
                )
                val credentialManager = remember(context) { CredentialManager.create(context) }
                AgriMitraApp(
                    viewModel = vm,
                    onSignOut = {
                        val wasGoogle = activeSession is AuthSession.Google
                        localAccount = null
                        scope.launch { prefsRepo.setActiveLocalUsername(null) }
                        if (wasGoogle) {
                            signOutFarmer(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = { currentUser = null }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AgriMitraApp(
    viewModel: AgriMitraViewModel,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedLotsState by viewModel.savedLotsState.collectAsStateWithLifecycle()
    val aiAdviceState by viewModel.aiAdviceState.collectAsStateWithLifecycle()
    val followUpState by viewModel.followUpState.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()

    val baseContext = LocalContext.current
    val localizedCtx = remember(baseContext, uiState.language) {
        baseContext.localizedContext(uiState.language)
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_CROPS

    val tabs = remember {
        listOf(
            NavTab(
                route = ROUTE_CROPS,
                labelRes = R.string.nav_crops,
                selectedIcon = Icons.Filled.Agriculture,
                unselectedIcon = Icons.Outlined.Agriculture,
                testTag = "nav_tab_crops"
            ),
            NavTab(
                route = ROUTE_COMPARE,
                labelRes = R.string.nav_compare,
                selectedIcon = Icons.Filled.Storefront,
                unselectedIcon = Icons.Outlined.Storefront,
                testTag = "nav_tab_compare"
            ),
            NavTab(
                route = ROUTE_FORECAST,
                labelRes = R.string.nav_forecast,
                selectedIcon = Icons.Filled.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome,
                testTag = "nav_tab_forecast"
            )
        )
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalContext provides localizedCtx) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 640.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar {
                            tabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                        ) {
                            tabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                val label = stringResource(tab.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        AgriMitraTopBar(
                            farmerName = uiState.farmerName,
                            language = uiState.language,
                            dataSourceMode = uiState.dataSourceMode,
                            isSyncing = uiState.isSyncing,
                            offlineLabel = stringResource(R.string.offline_cached),
                            liveLabel = stringResource(R.string.live_synced),
                            refreshLabel = stringResource(R.string.refresh_prices),
                            onSelectLanguage = viewModel::selectLanguage,
                            onRefreshClick = viewModel::refreshCurrentCropPrices,
                            onSignOutClick = onSignOut
                        )

                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_CROPS,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(ROUTE_CROPS) {
                                CropSelectionScreen(
                                    uiState = uiState,
                                    savedLotsState = savedLotsState,
                                    onSearchChange = viewModel::updateSearchQuery,
                                    onSelectCategory = viewModel::selectCategory,
                                    onSelectCrop = viewModel::selectCrop,
                                    onQuantityQuintalsChange = viewModel::updateQuantityQuintals,
                                    onQuantityUnitChange = viewModel::updateQuantityUnit,
                                    onTransportRateChange = viewModel::updateTransportRate,
                                    onCompareClick = { navigateToTab(ROUTE_COMPARE) },
                                    onLoadSavedLot = viewModel::loadSavedLot,
                                    onDeleteSavedLot = viewModel::deleteSavedLot
                                )
                            }

                            composable(ROUTE_COMPARE) {
                                MarketComparisonScreen(
                                    uiState = uiState,
                                    onOpenForecastClick = { navigateToTab(ROUTE_FORECAST) },
                                    onFetchWebPricesClick = viewModel::refreshCurrentCropPrices,
                                    onSaveBestPlanClick = viewModel::saveCurrentBestPlan
                                )
                            }

                            composable(ROUTE_FORECAST) {
                                ForecastAdvisorScreen(
                                    uiState = uiState,
                                    aiAdviceState = aiAdviceState,
                                    followUpState = followUpState,
                                    isSpeaking = isSpeaking,
                                    onSelectHorizon = viewModel::selectHorizon,
                                    onRefreshAiAdvice = { viewModel.refreshAiAdviceForCurrentSelection(forceRefresh = true) },
                                    onToggleSpeakAdvice = viewModel::toggleSpeakAdvice,
                                    onAskFollowUp = viewModel::askFollowUpQuestion,
                                    onEnsureAiLoaded = viewModel::ensureAiAdviceLoaded
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
