package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MandiPriceEntity::class,
        HistoricalPriceEntity::class,
        WebSourceEntity::class,
        FarmerAccountEntity::class,
        LocalSavedLotEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AgriMitraDatabase : RoomDatabase() {
    abstract fun mandiDao(): MandiDao
    abstract fun farmerAccountDao(): FarmerAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AgriMitraDatabase? = null

        fun getInstance(context: Context): AgriMitraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AgriMitraDatabase::class.java,
                    "agrimitra_cache.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

object SeedMandiData {
    fun buildInitialMandis(nowMs: Long): List<MandiPriceEntity> = listOf(
        // Sugarcane (per quintal mill / jaggery procurement rates)
        MandiPriceEntity("sugarcane_mandya", "sugarcane", "Mandya Sugar & Jaggery Yard", "Mandya", "Karnataka", 18.0, 325.0, 365.0, 348.0, 1850.0, 2.1, nowMs, "KrishiJagran", false),
        MandiPriceEntity("sugarcane_belagavi", "sugarcane", "Belagavi Gokak Mill Zone", "Belagavi", "Karnataka", 92.0, 338.0, 378.0, 360.0, 2400.0, 2.8, nowMs, "AgriWatch", false),
        MandiPriceEntity("sugarcane_kolhapur", "sugarcane", "Kolhapur Shirol Cane Yard", "Kolhapur", "Maharashtra", 125.0, 345.0, 385.0, 368.0, 2950.0, 3.1, nowMs, "CommodityOnline", false),
        MandiPriceEntity("sugarcane_muzaffarnagar", "sugarcane", "Muzaffarnagar Cane Mandi", "Muzaffarnagar", "Uttar Pradesh", 74.0, 350.0, 382.0, 370.0, 3100.0, 1.6, nowMs, "KisanDeals", false),

        // Arecanut
        MandiPriceEntity("arecanut_shimoga", "arecanut", "Shivamogga Market", "Shivamogga", "Karnataka", 42.0, 46200.0, 51400.0, 49200.0, 148.0, 3.1, nowMs, "CommodityOnline", false),
        MandiPriceEntity("arecanut_channagiri", "arecanut", "Channagiri Market", "Davanagere", "Karnataka", 16.0, 44800.0, 48900.0, 47100.0, 112.0, 1.8, nowMs, "KrishiJagran", false),
        MandiPriceEntity("arecanut_sirsi", "arecanut", "Sirsi TSS Yard", "Uttara Kannada", "Karnataka", 98.0, 47100.0, 52600.0, 50400.0, 94.0, 3.9, nowMs, "KisanDeals", false),
        MandiPriceEntity("arecanut_sagar", "arecanut", "Sagar Market", "Shivamogga", "Karnataka", 74.0, 45600.0, 49800.0, 48150.0, 76.0, 2.2, nowMs, "NaPanta", false),
        MandiPriceEntity("arecanut_tumkur", "arecanut", "Tumakuru Market", "Tumakuru", "Karnataka", 132.0, 43900.0, 47800.0, 46300.0, 62.0, 0.9, nowMs, "CommodityOnline", false),

        // Paddy
        MandiPriceEntity("paddy_gangavathi", "paddy", "Gangavathi Mandi", "Koppal", "Karnataka", 54.0, 2410.0, 2790.0, 2680.0, 420.0, 1.9, nowMs, "CommodityOnline", false),
        MandiPriceEntity("paddy_sindhanur", "paddy", "Sindhanur Mandi", "Raichur", "Karnataka", 86.0, 2460.0, 2840.0, 2730.0, 510.0, 2.4, nowMs, "AgriWatch", false),
        MandiPriceEntity("paddy_mandya", "paddy", "Mandya Market", "Mandya", "Karnataka", 18.0, 2290.0, 2590.0, 2490.0, 290.0, 0.6, nowMs, "NaPanta", false),
        MandiPriceEntity("paddy_davangere", "paddy", "Davanagere Market", "Davanagere", "Karnataka", 36.0, 2360.0, 2670.0, 2560.0, 340.0, 1.3, nowMs, "KrishiJagran", false),

        // Wheat
        MandiPriceEntity("wheat_indore", "wheat", "Indore Grain Mandi", "Indore", "Madhya Pradesh", 58.0, 2580.0, 2980.0, 2810.0, 720.0, 1.7, nowMs, "AgriWatch", false),
        MandiPriceEntity("wheat_sehore", "wheat", "Sehore Sharbati Yard", "Sehore", "Madhya Pradesh", 82.0, 2650.0, 3120.0, 2920.0, 540.0, 2.3, nowMs, "CommodityOnline", false),
        MandiPriceEntity("wheat_khanna", "wheat", "Khanna Grain Market", "Ludhiana", "Punjab", 110.0, 2510.0, 2780.0, 2680.0, 960.0, 1.1, nowMs, "KrishiJagran", false),
        MandiPriceEntity("wheat_dharwad", "wheat", "Dharwad APMC Yard", "Dharwad", "Karnataka", 26.0, 2520.0, 2820.0, 2690.0, 310.0, 1.4, nowMs, "NaPanta", false),

        // Maize
        MandiPriceEntity("maize_davangere", "maize", "Davanagere Maize Yard", "Davanagere", "Karnataka", 28.0, 2120.0, 2390.0, 2280.0, 610.0, 1.9, nowMs, "CommodityOnline", false),
        MandiPriceEntity("maize_haveri", "maize", "Haveri Grain Market", "Haveri", "Karnataka", 46.0, 2180.0, 2440.0, 2340.0, 480.0, 2.4, nowMs, "NaPanta", false),
        MandiPriceEntity("maize_chhindwara", "maize", "Chhindwara Mandi", "Chhindwara", "Madhya Pradesh", 95.0, 2210.0, 2490.0, 2390.0, 740.0, 2.8, nowMs, "AgriWatch", false),
        MandiPriceEntity("maize_nizamabad", "maize", "Nizamabad Grain Yard", "Nizamabad", "Telangana", 112.0, 2190.0, 2460.0, 2360.0, 520.0, 2.1, nowMs, "KisanDeals", false),

        // Soybean
        MandiPriceEntity("soybean_latur", "soybean", "Latur Krishi Mandi", "Latur", "Maharashtra", 54.0, 4460.0, 4880.0, 4720.0, 680.0, 2.2, nowMs, "AgriWatch", false),
        MandiPriceEntity("soybean_ujjain", "soybean", "Ujjain Oilseed Yard", "Ujjain", "Madhya Pradesh", 88.0, 4520.0, 4950.0, 4790.0, 810.0, 2.7, nowMs, "CommodityOnline", false),
        MandiPriceEntity("soybean_bidar", "soybean", "Bidar Market Yard", "Bidar", "Karnataka", 24.0, 4390.0, 4760.0, 4610.0, 390.0, 1.5, nowMs, "NaPanta", false),
        MandiPriceEntity("soybean_indore", "soybean", "Indore Laxmibai Mandi", "Indore", "Madhya Pradesh", 118.0, 4560.0, 4990.0, 4840.0, 920.0, 3.0, nowMs, "KrishiJagran", false),

        // Groundnut
        MandiPriceEntity("groundnut_gadag", "groundnut", "Gadag Pod Market", "Gadag", "Karnataka", 22.0, 6150.0, 6680.0, 6480.0, 340.0, 1.8, nowMs, "CommodityOnline", false),
        MandiPriceEntity("groundnut_challakere", "groundnut", "Challakere Oilseed Yard", "Chitradurga", "Karnataka", 58.0, 6320.0, 6890.0, 6670.0, 470.0, 2.5, nowMs, "KisanDeals", false),
        MandiPriceEntity("groundnut_rajkot", "groundnut", "Rajkot Gondal Yard", "Rajkot", "Gujarat", 116.0, 6450.0, 7050.0, 6820.0, 890.0, 3.1, nowMs, "AgriWatch", false),
        MandiPriceEntity("groundnut_kurnool", "groundnut", "Kurnool Groundnut Yard", "Kurnool", "Andhra Pradesh", 84.0, 6240.0, 6760.0, 6560.0, 410.0, 2.0, nowMs, "NaPanta", false),

        // Tomato
        MandiPriceEntity("tomato_kolar", "tomato", "Kolar Market", "Kolar", "Karnataka", 48.0, 1450.0, 2250.0, 1980.0, 680.0, -4.2, nowMs, "CommodityOnline", false),
        MandiPriceEntity("tomato_chintamani", "tomato", "Chintamani Mandi", "Chikkaballapur", "Karnataka", 14.0, 1380.0, 2050.0, 1840.0, 310.0, -3.5, nowMs, "NaPanta", false),
        MandiPriceEntity("tomato_madanapalle", "tomato", "Madanapalle Market", "Annamayya", "Andhra Pradesh", 88.0, 1650.0, 2420.0, 2190.0, 540.0, -1.8, nowMs, "KisanDeals", false),
        MandiPriceEntity("tomato_bengaluru", "tomato", "Yeshwanthpur Market", "Bengaluru", "Karnataka", 64.0, 1600.0, 2380.0, 2120.0, 495.0, -2.1, nowMs, "KrishiJagran", false),

        // Onion
        MandiPriceEntity("onion_lasalgaon", "onion", "Lasalgaon Market", "Nashik", "Maharashtra", 58.0, 2250.0, 3150.0, 2890.0, 820.0, 5.4, nowMs, "AgriWatch", false),
        MandiPriceEntity("onion_pimpalgaon", "onion", "Pimpalgaon Market", "Nashik", "Maharashtra", 22.0, 2100.0, 2940.0, 2690.0, 610.0, 4.1, nowMs, "CommodityOnline", false),
        MandiPriceEntity("onion_hubballi", "onion", "Hubballi Amargol", "Dharwad", "Karnataka", 92.0, 2320.0, 3210.0, 2980.0, 430.0, 6.2, nowMs, "KisanDeals", false),
        MandiPriceEntity("onion_gadag", "onion", "Gadag Market", "Gadag", "Karnataka", 45.0, 2150.0, 2860.0, 2610.0, 290.0, 3.0, nowMs, "NaPanta", false),

        // Black Pepper
        MandiPriceEntity("pepper_kochi", "black_pepper", "Kochi Spice Market", "Ernakulam", "Kerala", 110.0, 61500.0, 65800.0, 64200.0, 48.0, 2.7, nowMs, "AgriWatch", false),
        MandiPriceEntity("pepper_madikeri", "black_pepper", "Madikeri Market", "Kodagu", "Karnataka", 24.0, 59800.0, 63900.0, 62400.0, 34.0, 1.9, nowMs, "CommodityOnline", false),
        MandiPriceEntity("pepper_sakleshpur", "black_pepper", "Sakleshpur Market", "Hassan", "Karnataka", 56.0, 60400.0, 64300.0, 63100.0, 29.0, 2.3, nowMs, "KrishiJagran", false),
        MandiPriceEntity("pepper_chikkamagaluru", "black_pepper", "Chikkamagaluru Yard", "Chikkamagaluru", "Karnataka", 72.0, 60100.0, 64100.0, 62800.0, 25.0, 2.0, nowMs, "NaPanta", false),

        // Coconut / Copra
        MandiPriceEntity("coconut_tiptur", "coconut", "Tiptur Copra Yard", "Tumakuru", "Karnataka", 44.0, 11800.0, 13450.0, 12900.0, 210.0, 2.6, nowMs, "CommodityOnline", false),
        MandiPriceEntity("coconut_arsikere", "coconut", "Arsikere Market", "Hassan", "Karnataka", 19.0, 11400.0, 12800.0, 12350.0, 165.0, 1.8, nowMs, "KisanDeals", false),
        MandiPriceEntity("coconut_kangayam", "coconut", "Kangayam Market", "Tiruppur", "Tamil Nadu", 118.0, 12100.0, 13700.0, 13150.0, 240.0, 3.2, nowMs, "AgriWatch", false),
        MandiPriceEntity("coconut_hassan", "coconut", "Hassan Market", "Hassan", "Karnataka", 38.0, 11550.0, 12950.0, 12480.0, 130.0, 2.1, nowMs, "NaPanta", false),

        // Turmeric
        MandiPriceEntity("turmeric_sangli", "turmeric", "Sangli Market", "Sangli", "Maharashtra", 62.0, 14600.0, 16900.0, 16150.0, 175.0, 3.8, nowMs, "AgriWatch", false),
        MandiPriceEntity("turmeric_erode", "turmeric", "Erode Turmeric Yard", "Erode", "Tamil Nadu", 124.0, 14900.0, 17300.0, 16500.0, 220.0, 4.2, nowMs, "CommodityOnline", false),
        MandiPriceEntity("turmeric_chamarajanagar", "turmeric", "Chamarajanagar Yard", "Chamarajanagar", "Karnataka", 21.0, 13900.0, 15800.0, 15200.0, 96.0, 2.4, nowMs, "KrishiJagran", false),
        MandiPriceEntity("turmeric_nizamabad", "turmeric", "Nizamabad Market", "Nizamabad", "Telangana", 95.0, 14300.0, 16400.0, 15750.0, 185.0, 3.1, nowMs, "NaPanta", false),

        // Cotton
        MandiPriceEntity("cotton_raichur", "cotton", "Raichur Market", "Raichur", "Karnataka", 48.0, 6950.0, 7680.0, 7420.0, 380.0, -0.9, nowMs, "CommodityOnline", false),
        MandiPriceEntity("cotton_adoni", "cotton", "Adoni Cotton Yard", "Kurnool", "Andhra Pradesh", 82.0, 7050.0, 7790.0, 7560.0, 420.0, -0.5, nowMs, "AgriWatch", false),
        MandiPriceEntity("cotton_haveri", "cotton", "Haveri Market", "Haveri", "Karnataka", 18.0, 6820.0, 7490.0, 7260.0, 260.0, -1.2, nowMs, "NaPanta", false),
        MandiPriceEntity("cotton_akola", "cotton", "Akola Cotton Mandi", "Akola", "Maharashtra", 115.0, 7120.0, 7850.0, 7610.0, 460.0, -0.3, nowMs, "KisanDeals", false)
    )

    fun buildInitialHistory(): List<HistoricalPriceEntity> {
        val seriesByCrop = mapOf(
            "sugarcane" to listOf(342.0, 344.0, 345.0, 347.0, 349.0, 351.0, 353.0, 355.0, 357.0, 360.0, 362.0, 365.0, 367.0, 370.0),
            "arecanut" to listOf(45200.0, 45600.0, 45900.0, 46400.0, 46750.0, 47100.0, 47500.0, 47800.0, 48300.0, 48650.0, 49100.0, 49600.0, 50000.0, 50400.0),
            "paddy" to listOf(2540.0, 2555.0, 2570.0, 2565.0, 2585.0, 2600.0, 2620.0, 2630.0, 2645.0, 2660.0, 2680.0, 2695.0, 2710.0, 2730.0),
            "wheat" to listOf(2680.0, 2695.0, 2710.0, 2725.0, 2740.0, 2760.0, 2780.0, 2795.0, 2815.0, 2835.0, 2860.0, 2880.0, 2900.0, 2920.0),
            "maize" to listOf(2160.0, 2175.0, 2190.0, 2210.0, 2225.0, 2245.0, 2260.0, 2280.0, 2295.0, 2315.0, 2335.0, 2350.0, 2370.0, 2390.0),
            "soybean" to listOf(4520.0, 4540.0, 4565.0, 4590.0, 4615.0, 4640.0, 4665.0, 4690.0, 4715.0, 4740.0, 4765.0, 4790.0, 4815.0, 4840.0),
            "groundnut" to listOf(6340.0, 6370.0, 6405.0, 6440.0, 6475.0, 6510.0, 6545.0, 6585.0, 6620.0, 6660.0, 6700.0, 6740.0, 6780.0, 6820.0),
            "tomato" to listOf(2620.0, 2580.0, 2540.0, 2490.0, 2450.0, 2420.0, 2380.0, 2340.0, 2310.0, 2280.0, 2260.0, 2230.0, 2210.0, 2190.0),
            "onion" to listOf(2410.0, 2440.0, 2480.0, 2520.0, 2560.0, 2610.0, 2650.0, 2700.0, 2740.0, 2790.0, 2840.0, 2890.0, 2940.0, 2980.0),
            "black_pepper" to listOf(60200.0, 60500.0, 60800.0, 61100.0, 61400.0, 61700.0, 62000.0, 62350.0, 62600.0, 62950.0, 63300.0, 63600.0, 63900.0, 64200.0),
            "coconut" to listOf(12100.0, 12180.0, 12260.0, 12350.0, 12420.0, 12500.0, 12580.0, 12650.0, 12740.0, 12830.0, 12910.0, 12990.0, 13070.0, 13150.0),
            "turmeric" to listOf(14800.0, 14920.0, 15060.0, 15190.0, 15320.0, 15460.0, 15600.0, 15720.0, 15860.0, 15990.0, 16120.0, 16250.0, 16380.0, 16500.0),
            "cotton" to listOf(7840.0, 7820.0, 7800.0, 7790.0, 7770.0, 7750.0, 7730.0, 7715.0, 7695.0, 7680.0, 7660.0, 7645.0, 7625.0, 7610.0)
        )

        return seriesByCrop.flatMap { (cropId, values) ->
            values.mapIndexed { idx, price ->
                val offset = idx - (values.size - 1)
                HistoricalPriceEntity(
                    id = "${cropId}_d$offset",
                    cropId = cropId,
                    dayOffset = offset,
                    modalPricePerQuintal = price
                )
            }
        }
    }
}
