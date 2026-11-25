package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.CurrencyFormatter
import com.example.domain.model.AiAdviceCard
import com.example.domain.model.ForecastHorizon
import com.example.domain.model.PricePrediction
import com.example.domain.model.SellVerdict
import com.example.ui.components.CropArtworkBadge
import com.example.ui.components.PriceTrendChart
import com.example.ui.theme.HoldAmber
import com.example.ui.theme.HoldAmberBg
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.ui.viewmodel.AgriMitraUiState
import com.example.ui.viewmodel.AiAdvisorState
import com.example.ui.viewmodel.FollowUpState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForecastAdvisorScreen(
    uiState: AgriMitraUiState,
    aiAdviceState: AiAdvisorState,
    followUpState: FollowUpState,
    isSpeaking: Boolean,
    onSelectHorizon: (ForecastHorizon) -> Unit,
    onRefreshAiAdvice: () -> Unit,
    onToggleSpeakAdvice: (AiAdviceCard) -> Unit,
    onAskFollowUp: (String) -> Unit,
    onEnsureAiLoaded: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(uiState.selectedCrop.id, uiState.horizon, uiState.language) {
        onEnsureAiLoaded()
    }

    var questionInput by remember { mutableStateOf("") }
    val prediction = uiState.prediction

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CropArtworkBadge(
                        cropId = uiState.selectedCrop.id,
                        accentColorHex = uiState.selectedCrop.accentColorHex,
                        size = 44.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.forecast_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${uiState.selectedCrop.localizedName(uiState.language)} • ${uiState.selectedCrop.perishability.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (prediction != null) {
                // SELL NOW vs HOLD & WAIT Banner
                item {
                    VerdictBannerCard(prediction = prediction)
                }

                // 7d / 15d / 30d Horizon Switcher
                item {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val options = listOf(
                            ForecastHorizon.DAYS_7 to stringResource(R.string.horizon_7d),
                            ForecastHorizon.DAYS_15 to stringResource(R.string.horizon_15d),
                            ForecastHorizon.DAYS_30 to stringResource(R.string.horizon_30d)
                        )
                        options.forEachIndexed { index, (horizon, label) ->
                            SegmentedButton(
                                selected = uiState.horizon == horizon,
                                onClick = { onSelectHorizon(horizon) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = options.size
                                ),
                                modifier = Modifier.testTag("horizon_${horizon.days}d_btn")
                            ) {
                                Text(text = label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Price Trend Chart Card
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PriceTrendChart(
                                points = prediction.seriesPoints,
                                verdict = prediction.verdict,
                                pastLegend = stringResource(R.string.past_7d_legend),
                                forecastLegend = stringResource(R.string.forecast_legend)
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ForecastMetricTile(
                                    label = stringResource(R.string.projected_price),
                                    value = CurrencyFormatter.formatInr(prediction.predictedPrice),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                ForecastMetricTile(
                                    label = stringResource(R.string.projected_change),
                                    value = CurrencyFormatter.formatPercent(prediction.expectedChangePercent),
                                    color = if (prediction.expectedChangePercent >= 0) ProfitGreen else MaterialTheme.colorScheme.error
                                )
                                ForecastMetricTile(
                                    label = stringResource(
                                        R.string.projected_impact,
                                        uiState.quantityQuintals.roundToInt().toString()
                                    ),
                                    value = CurrencyFormatter.formatSignedInr(prediction.projectedNetGainForQuantity),
                                    color = if (prediction.projectedNetGainForQuantity >= 0) ProfitGreen else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Voice & AI Smart Advisor Card
            item {
                GeminiSmartAdvisorCard(
                    aiAdviceState = aiAdviceState,
                    isSpeaking = isSpeaking,
                    onRefreshAiAdvice = onRefreshAiAdvice,
                    onToggleSpeak = onToggleSpeakAdvice
                )
            }

            // Ask AgriMitra Follow-Up Streaming Q&A (grounded in Google Search)
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_ask_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "How long can I store safely?",
                                "Are festival prices rising soon?",
                                "Rain impact on rates this week?"
                            ).forEach { promptChip ->
                                AssistChip(
                                    onClick = {
                                        questionInput = promptChip
                                        onAskFollowUp(promptChip)
                                    },
                                    label = { Text(promptChip, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = questionInput,
                                onValueChange = { questionInput = it },
                                placeholder = { Text(stringResource(R.string.ai_ask_hint)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_followup_input")
                            )
                            FilledIconButton(
                                onClick = {
                                    if (questionInput.isNotBlank()) {
                                        onAskFollowUp(questionInput)
                                    }
                                },
                                enabled = questionInput.isNotBlank() && followUpState !is FollowUpState.Loading,
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("ai_followup_send")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = stringResource(R.string.ai_send)
                                )
                            }
                        }

                        when (followUpState) {
                            is FollowUpState.Idle -> Unit
                            is FollowUpState.Loading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = stringResource(R.string.ai_loading),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is FollowUpState.Streaming -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = followUpState.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            is FollowUpState.Success -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = followUpState.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            is FollowUpState.Error -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = followUpState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun VerdictBannerCard(prediction: PricePrediction) {
    val isHold = prediction.verdict == SellVerdict.HOLD_WAIT
    val bgColor = if (isHold) ProfitGreenBg else HoldAmberBg
    val fgColor = if (isHold) ProfitGreen else HoldAmber

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.5.dp, fgColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sell_verdict_banner")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isHold) Icons.Filled.HourglassTop else Icons.Filled.Sell,
                        contentDescription = null,
                        tint = fgColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (isHold) {
                            stringResource(R.string.verdict_hold_wait)
                        } else {
                            stringResource(R.string.verdict_sell_now)
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = fgColor
                    )
                }

                Surface(
                    color = fgColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (prediction.expectedChangePercent >= 0) {
                                Icons.AutoMirrored.Filled.TrendingUp
                            } else {
                                Icons.AutoMirrored.Filled.TrendingDown
                            },
                            contentDescription = null,
                            tint = fgColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = CurrencyFormatter.formatPercent(prediction.expectedChangePercent),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = fgColor
                        )
                    }
                }
            }

            Text(
                text = prediction.verdictSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1F2937)
            )
        }
    }
}

@Composable
private fun ForecastMetricTile(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}

@Composable
private fun GeminiSmartAdvisorCard(
    aiAdviceState: AiAdvisorState,
    isSpeaking: Boolean,
    onRefreshAiAdvice: () -> Unit,
    onToggleSpeak: (AiAdviceCard) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gemini_advisor_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.ai_advisor_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.ai_advisor_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (aiAdviceState is AiAdvisorState.Success) {
                        FilledTonalButton(
                            onClick = { onToggleSpeak(aiAdviceState.advice) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("listen_advice_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSpeaking) stringResource(R.string.stop_listening) else stringResource(R.string.listen_advice),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefreshAiAdvice,
                        enabled = aiAdviceState !is AiAdvisorState.Loading,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_ai_advice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.ai_refresh_btn),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            when (aiAdviceState) {
                is AiAdvisorState.Idle -> {
                    Button(
                        onClick = onRefreshAiAdvice,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.ai_generate_btn))
                    }
                }

                is AiAdvisorState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Text(
                            text = stringResource(R.string.ai_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is AiAdvisorState.Success -> {
                    val advice = aiAdviceState.advice

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = advice.headlineVerdict,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        advice.bullets.forEach { bullet ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(16.dp)
                                )
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (advice.marketDrivers.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = advice.marketDrivers,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (advice.riskNote.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = advice.riskNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                is AiAdvisorState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = aiAdviceState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            OutlinedButton(
                                onClick = onRefreshAiAdvice,
                                modifier = Modifier.testTag("retry_ai_advice_button")
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
            }
        }
    }
}
