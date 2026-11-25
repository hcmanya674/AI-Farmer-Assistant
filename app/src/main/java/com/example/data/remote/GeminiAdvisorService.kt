package com.example.data.remote

import android.util.Log
import com.example.core.AppLanguage
import com.example.core.CurrencyFormatter
import com.example.domain.model.AiAdviceCard
import com.example.domain.model.Crop
import com.example.domain.model.MarketComparisonResult
import com.example.domain.model.PricePrediction
import com.example.domain.model.SellVerdict
import com.example.domain.model.WebSourceLink
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiAdvisorService"
private const val MODEL_NAME = "gemini-3.8-flash"

class GeminiAdvisorService {

    private val adviceSchema = Schema.obj(
        properties = mapOf(
            "headlineVerdict" to Schema.string(
                description = "A punchy 1-sentence recommendation starting with SELL NOW or HOLD & WAIT in the requested language."
            ),
            "bullets" to Schema.array(
                items = Schema.string(),
                description = "Exactly 3 concise, farmer-friendly bullet points explaining price differences across markets, money in hand after transport, and weather/festival demand drivers."
            ),
            "marketDrivers" to Schema.string(
                description = "2-sentence summary of weather, harvest arrivals, and festive demand driving this crop's trend."
            ),
            "riskNote" to Schema.string(
                description = "1-sentence practical storage, moisture, or price-drop caution for the farmer."
            )
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.25f
                responseMimeType = "application/json"
                responseSchema = adviceSchema
            }
        )
    }

    private val conversationalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            tools = listOf(Tool.googleSearch()),
            generationConfig = generationConfig {
                temperature = 0.35f
            }
        )
    }

    suspend fun generateStructuredAdvice(
        crop: Crop,
        rankedMarkets: List<MarketComparisonResult>,
        prediction: PricePrediction,
        webSources: List<WebSourceLink>,
        language: AppLanguage
    ): AiAdviceCard {
        val topMandi = rankedMarkets.firstOrNull()
        val closestMandi = rankedMarkets.firstOrNull { it.isClosest }

        val marketSnapshot = rankedMarkets.take(4).joinToString("\n") { item ->
            val m = item.mandiPrice
            "- ${m.mandiName} (${m.district}, ${m.state}, source: ${m.sourceSite}): Rate ${CurrencyFormatter.formatInr(m.modalPricePerQuintal)}/Qtl, Distance ${m.distanceKm.toInt()} km, Transport ${CurrencyFormatter.formatInr(item.transportCost)}, Net Money in Hand ${CurrencyFormatter.formatInr(item.netProfit)}"
        }

        val verdictWord = if (prediction.verdict == SellVerdict.SELL_NOW) "SELL NOW" else "HOLD & WAIT"

        val prompt = """
You are AgriMitra, a warm, clear, practical agricultural market advisor helping an Indian farmer make the best selling decision.
Respond strictly in ${language.geminiLanguageName}. Use simple, everyday farmer-friendly words (no technical jargon).

Crop: ${crop.nameEn} (${crop.variety})
Storage Profile: ${crop.perishability.label}
Load Size: ${topMandi?.quantityQuintals ?: crop.defaultQuantityQuintals} Quintals
Vehicle Cost: ₹${topMandi?.transportCostPerKm?.toInt() ?: 22}/km

Nearby Market Comparison (Ranked by Money in Hand after Transport):
$marketSnapshot

Best Profit Market: ${topMandi?.mandiPrice?.mandiName ?: "Local Market"} (${CurrencyFormatter.formatInr(topMandi?.netProfit ?: 0.0)})
Closest Market: ${closestMandi?.mandiPrice?.mandiName ?: "Local Market"} (${CurrencyFormatter.formatInr(closestMandi?.netProfit ?: 0.0)})

Forecast Horizon: ${prediction.horizon.days} days
Current Best Rate: ${CurrencyFormatter.formatInr(prediction.currentModalPrice)}/Qtl
Predicted ${prediction.horizon.days}-Day Rate: ${CurrencyFormatter.formatInr(prediction.predictedPrice)}/Qtl (${CurrencyFormatter.formatPercent(prediction.expectedChangePercent)})
Estimated Extra/Loss on Load: ${CurrencyFormatter.formatSignedInr(prediction.projectedNetGainForQuantity)}
Signal: $verdictWord
Seasonal Note: ${crop.seasonalFactorNote}

Write a clear 1-line headline verdict, exactly 3 simple actionable bullet points mentioning real market names and rupee figures, weather/festival market drivers, and a practical storage tip.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("Gemini returned an empty response.")

            val json = JSONObject(jsonText)
            val headline = json.optString("headlineVerdict").ifBlank { verdictWord }
            val bulletsArray = json.optJSONArray("bullets")
            val bullets = buildList {
                if (bulletsArray != null) {
                    for (i in 0 until bulletsArray.length()) {
                        val item = bulletsArray.optString(i).trim()
                        if (item.isNotEmpty()) add(item)
                    }
                }
            }
            val drivers = json.optString("marketDrivers")
            val risk = json.optString("riskNote")

            AiAdviceCard(
                headlineVerdict = headline,
                bullets = bullets,
                marketDrivers = drivers,
                riskNote = risk,
                sources = webSources,
                generatedAtEpochMs = System.currentTimeMillis(),
                language = language
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini structured advice generation failed", e)
            throw e
        }
    }

    fun askFollowUpStream(
        question: String,
        crop: Crop,
        bestMandi: MarketComparisonResult?,
        prediction: PricePrediction,
        language: AppLanguage
    ): Flow<String> {
        val prompt = """
You are AgriMitra, a friendly agricultural market advisor for Indian farmers.
Reply simply and clearly in ${language.geminiLanguageName} (maximum 4 short sentences, no jargon). Use Google Search if current weather, festival dates, or market news helps answer accurately.
Context:
- Crop: ${crop.nameEn} (${crop.variety}), ${crop.perishability.label}
- Best Market today: ${bestMandi?.mandiPrice?.mandiName ?: "Local Market"} at ${CurrencyFormatter.formatInr(prediction.currentModalPrice)}/Qtl (Money in hand ${CurrencyFormatter.formatInr(bestMandi?.netProfit ?: 0.0)})
- ${prediction.horizon.days}-day predicted rate: ${CurrencyFormatter.formatInr(prediction.predictedPrice)}/Qtl (${CurrencyFormatter.formatPercent(prediction.expectedChangePercent)})
- Advice: ${if (prediction.verdict == SellVerdict.SELL_NOW) "SELL NOW" else "HOLD & WAIT"}

Farmer's Question: "$question"
        """.trimIndent()

        return conversationalModel.generateContentStream(prompt).map { chunk ->
            chunk.text.orEmpty()
        }
    }
}
