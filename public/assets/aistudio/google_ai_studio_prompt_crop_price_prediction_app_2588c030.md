# Google AI Studio System & Implementation Prompt: AI-Based Crop Price Prediction and Market Comparison Android App

Copy and paste the prompt below into **Google AI Studio** (using Gemini 1.5 Pro or Gemini 2.5 Pro) to generate the architecture, backend API integration, machine learning logic, and Android frontend code (Kotlin/Jetpack Compose) for your application.

---

## 📋 PROMPT TO COPY:

```text
Act as an expert Android Developer, AI/ML Architect, and Agri-Tech Product Designer. I am building an Android application to solve a real-world problem for my father and fellow farmers: **"AI-Based Crop Price Prediction and Market Comparison."**

### 1. Problem Context & Goal
Farmers often lose profit because they do not know which nearby market (Mandi/APMC) offers the best price for their harvested crop, nor do they know whether they should sell immediately or store the crop for a few days/weeks based on price trends. 
The goal of this app is to provide a simple, accessible, and accurate mobile tool that:
1. Compares current real-time prices of a selected crop across nearby markets ( factoring in distance/transport cost).
2. Uses AI/ML time-series forecasting and historical data analysis to predict crop prices for the next 7, 15, and 30 days.
3. Gives a clear **"Sell Now" vs. "Hold/Wait"** recommendation in simple language.

### 2. Target Audience & UX Requirements
- **Primary User:** Indian farmers (specifically tailored for ease of use, high readability, large buttons, minimal typing, and visual indicators like green/red arrows).
- **Language Support:** Design the UI architecture to easily support English and regional languages (e.g., Kannada, Hindi, Marathi) using Android string resources and Gemini translation capabilities.
- **Offline Capability:** Cache the last fetched market prices locally using Room Database so the farmer can view them even with poor rural internet connectivity.

### 3. Technical Stack Preferences
- **Android Frontend:** Kotlin with Jetpack Compose (Material Design 3), MVVM Architecture.
- **Networking & Local DB:** Retrofit (for APIs), Room (for offline caching), Coil (for crop images/icons).
- **AI & Data Layer:** 
  - Integration with **Gemini API (Google AI SDK for Android)** to act as an intelligent agricultural advisor that explains *why* prices are rising/falling (weather, seasonal demand, festival spikes) and summarizes market trends in simple, conversational terms.
  - Integration structure for Government Open Data APIs (e.g., India's Agmarknet / data.gov.in API for Mandi prices) and a historical price dataset for ML forecasting.

### 4. Step-by-Step Output Required
Please help me build this project systematically. Provide the following in well-structured, production-ready code and explanations:

#### Phase 1: System Architecture & Data Flow
- Provide a clean architecture diagram (in text/Mermaid format) showing how the Android App, Agmarknet API, ML Prediction Model/Endpoint, and Gemini API interact.
- Define the data models (`Crop`, `MandiPrice`, `PricePrediction`, `MarketComparisonResult`).

#### Phase 2: AI Price Prediction & Net Profit Logic
- Write the logic/algorithm (in Kotlin or a lightweight Python FastAPI microservice that the Android app calls) to:
  1. Predict future prices (7-day and 15-day forecast) using historical trends and seasonal factors.
  2. Calculate **Net Realizable Profit** per market using the formula: 
     $$\text{Net Profit} = (\text{Market Price per Quintal} \times \text{Quantity}) - (\text{Distance in km} \times \text{Transport Cost per km})$$
  3. Rank nearby markets based on Net Profit rather than just raw selling price.

#### Phase 3: Google Gemini AI Integration (Smart Advisor)
- Write the Kotlin code using the `com.google.ai.client.generativeai` SDK to pass the fetched market data and predicted trend to Gemini, prompting it to generate a 3-bullet-point actionable advice card for the farmer (e.g., *"Prices in [Mandi A] are 8% higher than [Mandi B]. Even after transport costs, you earn ₹1,200 more. Demand is expected to rise next week due to upcoming festivals."*).

#### Phase 4: Android UI Implementation (Jetpack Compose)
Generate complete, clean Jetpack Compose screens for:
1. **Home / Crop Selection Screen:** Grid of common crops (e.g., Arecanut, Paddy, Tomato, Onion, Pepper, Coconut) with search and quantity input (in Quintals).
2. **Market Comparison Screen:** A ranked list of nearby Mandis showing Distance (km), Modal Price, Estimated Transport Cost, and **Net Profit** highlighted clearly.
3. **Price Forecast & AI Advice Screen:** A visual price trend chart (past 7 days + predicted next 7 days), a clear "SELL NOW" or "WAIT" badge, and the Gemini AI summary card.

Please start by providing **Phase 1 and Phase 2** in detail, followed by the complete Kotlin code for **Phase 3 and Phase 4**. Ensure all code follows modern Android best practices, handles loading/error states gracefully, and includes comments explaining where to insert API keys.
```

---

## 💡 Tips for Using This in Google AI Studio:
1. **Select the Right Model:** In Google AI Studio, select **Gemini 1.5 Pro** or **Gemini 2.5 Pro** from the right-hand sidebar, as they handle large multi-file code generation best.
2. **Set Temperature:** Set the Temperature to **0.2 or 0.3** so the generated Kotlin code is accurate, deterministic, and bug-free.
3. **Iterate Step-by-Step:** Once AI Studio generates the initial response, you can follow up with prompts like:
   - *"Now generate the `build.gradle.kts` dependencies file needed for this exact code."*
   - *"Add Kannada localization strings (`strings.xml`) for all the UI labels you just created."*
   - *"Help me get a free API key from data.gov.in for Agmarknet Mandi prices and write the Retrofit interface for it."*