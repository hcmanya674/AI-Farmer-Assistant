package com.example.data.local

import com.example.domain.model.Crop
import com.example.domain.model.CropCategory
import com.example.domain.model.Perishability

object CropCatalog {
    val allCrops: List<Crop> = listOf(
        Crop(
            id = "sugarcane",
            nameEn = "Sugarcane",
            nameKn = "ಕಬ್ಬು",
            nameHi = "गन्ना",
            nameMr = "ऊस",
            variety = "Co-86032 / Mill & Jaggery",
            category = CropCategory.CASH_CROP,
            perishability = Perishability.HIGH,
            seasonalFactorNote = "Sugar mills & jaggery units competing for high-recovery Co-86032 cane; crush within 48 hrs of harvest.",
            defaultQuantityQuintals = 80.0,
            accentColorHex = 0xFF16A34A
        ),
        Crop(
            id = "arecanut",
            nameEn = "Arecanut",
            nameKn = "ಅಡಿಕೆ",
            nameHi = "सुपारी",
            nameMr = "सुपारी",
            variety = "Red Chali / Rashi",
            category = CropCategory.SPICE_PLANTATION,
            perishability = Perishability.LOW,
            seasonalFactorNote = "Pre-festival & wedding season procurement lifting Shivamogga/Sirsi rates.",
            defaultQuantityQuintals = 12.0,
            accentColorHex = 0xFF8D4E2A
        ),
        Crop(
            id = "paddy",
            nameEn = "Paddy (Rice)",
            nameKn = "ಭತ್ತ",
            nameHi = "धान",
            nameMr = "भात",
            variety = "Sona Masoori / Grade-A",
            category = CropCategory.GRAIN,
            perishability = Perishability.LOW,
            seasonalFactorNote = "Millers actively stocking fine-grain Sona Masoori; strong buyer demand.",
            defaultQuantityQuintals = 30.0,
            accentColorHex = 0xFFD97706
        ),
        Crop(
            id = "wheat",
            nameEn = "Wheat",
            nameKn = "ಗೋಧಿ",
            nameHi = "गेहूं",
            nameMr = "गहू",
            variety = "Sharbati / Lokwan",
            category = CropCategory.GRAIN,
            perishability = Perishability.LOW,
            seasonalFactorNote = "Roller flour mills & traders paying premium for clean, dry Sharbati lots.",
            defaultQuantityQuintals = 30.0,
            accentColorHex = 0xFFB45309
        ),
        Crop(
            id = "maize",
            nameEn = "Maize (Corn)",
            nameKn = "ಮೆಕ್ಕೆಜೋಳ",
            nameHi = "मक्का",
            nameMr = "मका",
            variety = "Yellow Hybrid Feed",
            category = CropCategory.GRAIN,
            perishability = Perishability.LOW,
            seasonalFactorNote = "Ethanol plants & poultry feed buyers sustaining firm bids in Karnataka & MP.",
            defaultQuantityQuintals = 35.0,
            accentColorHex = 0xFFCA8A04
        ),
        Crop(
            id = "soybean",
            nameEn = "Soybean",
            nameKn = "ಸೋಯಾಬೀನ್",
            nameHi = "सोयाबीन",
            nameMr = "सोयाबीन",
            variety = "JS-335 Yellow Bold",
            category = CropCategory.CASH_CROP,
            perishability = Perishability.LOW,
            seasonalFactorNote = "Oil crushers & soymeal exporters bidding actively for low-moisture seed.",
            defaultQuantityQuintals = 20.0,
            accentColorHex = 0xFF65A30D
        ),
        Crop(
            id = "groundnut",
            nameEn = "Groundnut",
            nameKn = "ಶೇಂಗಾ",
            nameHi = "मूंगफली",
            nameMr = "भुईमूग",
            variety = "TMV-2 / Bold Pod",
            category = CropCategory.CASH_CROP,
            perishability = Perishability.LOW,
            seasonalFactorNote = "Oil mill & roasting kernel demand keeping Gadag and Saurashtra yards firm.",
            defaultQuantityQuintals = 18.0,
            accentColorHex = 0xFF9A3412
        ),
        Crop(
            id = "tomato",
            nameEn = "Tomato",
            nameKn = "ಟೊಮೆಟೊ",
            nameHi = "टमाटर",
            nameMr = "टोमॅटो",
            variety = "Hybrid Desi",
            category = CropCategory.VEGETABLE,
            perishability = Perishability.HIGH,
            seasonalFactorNote = "Heavy Kolar & Nashik arrivals creating short-term supply pressure.",
            defaultQuantityQuintals = 15.0,
            accentColorHex = 0xFFDC2626
        ),
        Crop(
            id = "onion",
            nameEn = "Onion",
            nameKn = "ಈರುಳ್ಳಿ",
            nameHi = "प्याज",
            nameMr = "कांदा",
            variety = "Nashik Red / Garwa",
            category = CropCategory.VEGETABLE,
            perishability = Perishability.MEDIUM,
            seasonalFactorNote = "Export inquiries and tighter stored-stock arrivals pushing wholesale bids higher.",
            defaultQuantityQuintals = 25.0,
            accentColorHex = 0xFF9333EA
        ),
        Crop(
            id = "black_pepper",
            nameEn = "Black Pepper",
            nameKn = "ಕಾಳುಮೆಣಸು",
            nameHi = "काली मिर्च",
            nameMr = "काळी मिरी",
            variety = "Malabar Garbled (MG-1)",
            category = CropCategory.SPICE_PLANTATION,
            perishability = Perishability.LOW,
            seasonalFactorNote = "Strong spice export demand and low domestic carryover inventory.",
            defaultQuantityQuintals = 5.0,
            accentColorHex = 0xFF374151
        ),
        Crop(
            id = "coconut",
            nameEn = "Coconut / Copra",
            nameKn = "ತೆಂಗಿನಕಾಯಿ",
            nameHi = "नारियल",
            nameMr = "नारळ",
            variety = "Tiptur Ball Copra",
            category = CropCategory.SPICE_PLANTATION,
            perishability = Perishability.LOW,
            seasonalFactorNote = "Steady oil-mill crushing demand and festive buying in Tiptur/Arsikere.",
            defaultQuantityQuintals = 18.0,
            accentColorHex = 0xFF15803D
        ),
        Crop(
            id = "turmeric",
            nameEn = "Turmeric",
            nameKn = "ಅರಿಶಿನ",
            nameHi = "हल्दी",
            nameMr = "हळद",
            variety = "Salem / Sangli Finger",
            category = CropCategory.SPICE_PLANTATION,
            perishability = Perishability.LOW,
            seasonalFactorNote = "Healthy export orders lifting quality finger lots in Sangli & Erode.",
            defaultQuantityQuintals = 10.0,
            accentColorHex = 0xFFEAB308
        ),
        Crop(
            id = "cotton",
            nameEn = "Cotton",
            nameKn = "ಹತ್ತಿ",
            nameHi = "कपास",
            nameMr = "कापूस",
            variety = "Shankar-6 Long Staple",
            category = CropCategory.CASH_CROP,
            perishability = Perishability.LOW,
            seasonalFactorNote = "Ginning mills buying selectively; moisture levels determine top slab bids.",
            defaultQuantityQuintals = 20.0,
            accentColorHex = 0xFF0284C7
        )
    )

    fun byId(id: String): Crop = allCrops.firstOrNull { it.id == id } ?: allCrops.first()
}
