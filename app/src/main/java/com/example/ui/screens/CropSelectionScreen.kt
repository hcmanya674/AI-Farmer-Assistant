package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.AppLanguage
import com.example.core.CurrencyFormatter
import com.example.data.remote.SavedHarvestLot
import com.example.domain.model.Crop
import com.example.domain.model.CropCategory
import com.example.domain.model.MandiPrice
import com.example.domain.model.QuantityUnit
import com.example.ui.components.CropArtworkBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.AgriMitraUiState
import com.example.ui.viewmodel.SavedLotsState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CropSelectionScreen(
    uiState: AgriMitraUiState,
    savedLotsState: SavedLotsState,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (CropCategory?) -> Unit,
    onSelectCrop: (Crop) -> Unit,
    onQuantityQuintalsChange: (Double) -> Unit,
    onQuantityUnitChange: (QuantityUnit) -> Unit,
    onTransportRateChange: (Double) -> Unit,
    onCompareClick: () -> Unit,
    onLoadSavedLot: (SavedHarvestLot) -> Unit,
    onDeleteSavedLot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBags = uiState.quantityUnit == QuantityUnit.BAGS_50KG
    val displayQty = if (isBags) (uiState.quantityQuintals * 2.0) else uiState.quantityQuintals

    var quantityText by remember(displayQty, uiState.quantityUnit) {
        mutableStateOf(
            if (displayQty % 1.0 == 0.0) displayQty.roundToInt().toString()
            else String.format(java.util.Locale.US, "%.1f", displayQty)
        )
    }

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
            // Warm Welcome Banner
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF14532D),
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.86f)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Agriculture,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.home_headline),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.home_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.90f)
                                )
                            }
                        }
                    }
                }
            }

            // STEP 1: Pick Your Crop + Category Filter Chips
            item {
                Text(
                    text = stringResource(R.string.step_1_crop),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text(stringResource(R.string.search_crops_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("crop_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Easy Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf(
                        null to stringResource(R.string.cat_all),
                        CropCategory.CASH_CROP to stringResource(R.string.cat_cash),
                        CropCategory.GRAIN to stringResource(R.string.cat_grains),
                        CropCategory.SPICE_PLANTATION to stringResource(R.string.cat_spices),
                        CropCategory.VEGETABLE to stringResource(R.string.cat_vegetables)
                    )
                    categories.forEach { (category, label) ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { onSelectCategory(category) },
                            label = { Text(label) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("category_chip_${category?.name ?: "ALL"}")
                        )
                    }
                }
            }

            if (uiState.filteredCrops.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.no_crops_found),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.no_crops_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val rows = uiState.filteredCrops.chunked(2)
                items(rows.size) { rowIdx ->
                    val pair = rows[rowIdx]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { crop ->
                            CropCard(
                                crop = crop,
                                topPrice = uiState.topPriceByCrop[crop.id],
                                language = uiState.language,
                                isSelected = crop.id == uiState.selectedCrop.id,
                                onClick = { onSelectCrop(crop) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // STEP 2 & STEP 3: Load Size (Quintals or 50kg Bags) & Vehicle Cost
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.step_2_load),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Unit Toggle: Quintals (100 kg) vs Bags (50 kg)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = !isBags,
                                onClick = { onQuantityUnitChange(QuantityUnit.QUINTALS) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.testTag("unit_quintals_btn")
                            ) {
                                Text(stringResource(R.string.quintals_full), maxLines = 1)
                            }
                            SegmentedButton(
                                selected = isBags,
                                onClick = { onQuantityUnitChange(QuantityUnit.BAGS_50KG) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.testTag("unit_bags_btn")
                            ) {
                                Text(stringResource(R.string.bags_full), maxLines = 1)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val stepQtl = if (isBags) 0.5 else 1.0
                                    onQuantityQuintalsChange((uiState.quantityQuintals - stepQtl).coerceAtLeast(0.5))
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("qty_minus_button")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                            }

                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { input ->
                                    quantityText = input
                                    input.toDoubleOrNull()?.let { parsed ->
                                        if (parsed > 0.0) {
                                            val inQtl = if (isBags) parsed / 2.0 else parsed
                                            onQuantityQuintalsChange(inQtl)
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                suffix = {
                                    Text(
                                        text = if (isBags) stringResource(R.string.bags_unit) else stringResource(R.string.quintal_unit),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quantity_input")
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    val stepQtl = if (isBags) 0.5 else 1.0
                                    onQuantityQuintalsChange(uiState.quantityQuintals + stepQtl)
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("qty_plus_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                            }
                        }

                        val bagsCount = (uiState.quantityQuintals * 2.0).roundToInt()
                        val qtlRounded = if (uiState.quantityQuintals % 1.0 == 0.0) {
                            uiState.quantityQuintals.roundToInt().toString()
                        } else {
                            String.format(java.util.Locale.US, "%.1f", uiState.quantityQuintals)
                        }
                        Text(
                            text = if (isBags) {
                                "= $qtlRounded ${stringResource(R.string.quintals_full)}"
                            } else {
                                "= $bagsCount ${stringResource(R.string.bags_full)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = if (isBags) listOf(10.0, 20.0, 50.0, 100.0) else listOf(5.0, 10.0, 25.0, 50.0)
                            presets.forEach { presetDisplay ->
                                val presetQtl = if (isBags) presetDisplay / 2.0 else presetDisplay
                                FilterChip(
                                    selected = kotlin.math.abs(displayQty - presetDisplay) < 0.01,
                                    onClick = { onQuantityQuintalsChange(presetQtl) },
                                    label = {
                                        Text(
                                            text = "${presetDisplay.toInt()} ${if (isBags) stringResource(R.string.bags_unit) else stringResource(R.string.quintal_unit)}",
                                            fontFamily = JetBrainsMonoFontFamily
                                        )
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.step_3_vehicle),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple(15.0, "Tractor", "₹15/km"),
                                Triple(22.0, "Pickup", "₹22/km"),
                                Triple(35.0, "Truck", "₹35/km")
                            ).forEach { (rate, vehicleName, rateLabel) ->
                                val selected = uiState.transportCostPerKm == rate
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onTransportRateChange(rate) },
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = vehicleName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = rateLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Big Farmer CTA Button
            item {
                Button(
                    onClick = onCompareClick,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("compare_mandis_button")
                ) {
                    Text(
                        text = "${stringResource(R.string.compare_mandis_cta)} • ${uiState.selectedCrop.localizedName(uiState.language)}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            }

            // Saved Cloud Harvest Plans Section
            if (savedLotsState is SavedLotsState.Success && savedLotsState.lots.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.saved_plans_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        savedLotsState.lots.take(5).forEach { lot ->
                            SavedHarvestLotCard(
                                lot = lot,
                                onOpenClick = {
                                    onLoadSavedLot(lot)
                                    onCompareClick()
                                },
                                onDeleteClick = { onDeleteSavedLot(lot.id) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SavedHarvestLotCard(
    lot: SavedHarvestLot,
    onOpenClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "${lot.cropName} • ${lot.quantityQuintals.roundToInt()} ${stringResource(R.string.quintal_unit)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lot.mandiName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stringResource(R.string.net_profit)}: ${CurrencyFormatter.formatInr(lot.netProfit)}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            FilledTonalButton(
                onClick = onOpenClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(stringResource(R.string.load_plan))
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.delete_plan),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CropCard(
    crop: Crop,
    topPrice: MandiPrice?,
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "crop_card_bg"
    )
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("crop_card_${crop.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                CropArtworkBadge(
                    cropId = crop.id,
                    accentColorHex = crop.accentColorHex,
                    size = 44.dp
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected crop",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = crop.localizedName(language),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = crop.variety,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (topPrice != null) {
                val isUp = topPrice.dayChangePercent >= 0.0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${CurrencyFormatter.formatInr(topPrice.modalPricePerQuintal)}/q",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (isUp) ProfitGreen else LossRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
