package com.example.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.core.PasswordHasher
import com.example.data.local.FarmerAccountDao
import com.example.data.local.FarmerAccountEntity
import com.example.data.local.LocalSavedLotEntity
import com.example.data.remote.SavedHarvestLot
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

object AccountInputValidator {
    private val USERNAME_REGEX = Regex("^[a-z0-9_.-]{3,30}$")

    fun normalizeUsername(raw: String): String = raw.trim().lowercase()

    fun validateUsername(raw: String): String? {
        val normalized = normalizeUsername(raw)
        if (normalized.length < 3) {
            return "Username must be at least 3 characters."
        }
        if (normalized.length > 30) {
            return "Username cannot exceed 30 characters."
        }
        if (!USERNAME_REGEX.matches(normalized)) {
            return "Username may only use letters, numbers, dots (.), underscores (_), or hyphens (-)."
        }
        return null
    }

    fun validateFullName(raw: String): String? {
        val clean = raw.trim()
        if (clean.length < 2) return "Please enter your name (at least 2 characters)."
        if (clean.length > 60) return "Name cannot exceed 60 characters."
        return null
    }

    fun validatePassword(password: String): String? {
        if (password.length < 6) return "Password must be at least 6 characters."
        if (password.length > 128) return "Password is too long."
        return null
    }
}

class LocalAuthRepository(private val accountDao: FarmerAccountDao) {

    suspend fun registerAccount(
        fullName: String,
        username: String,
        password: String,
        confirmPassword: String
    ): Result<FarmerAccountEntity> = withContext(Dispatchers.IO) {
        val nameErr = AccountInputValidator.validateFullName(fullName)
        if (nameErr != null) return@withContext Result.failure(IllegalArgumentException(nameErr))

        val userErr = AccountInputValidator.validateUsername(username)
        if (userErr != null) return@withContext Result.failure(IllegalArgumentException(userErr))

        val passErr = AccountInputValidator.validatePassword(password)
        if (passErr != null) return@withContext Result.failure(IllegalArgumentException(passErr))

        if (password != confirmPassword) {
            return@withContext Result.failure(IllegalArgumentException("Passwords do not match."))
        }

        val normalizedUser = AccountInputValidator.normalizeUsername(username)
        val existing = accountDao.findByUsername(normalizedUser)
        if (existing != null) {
            return@withContext Result.failure(
                IllegalStateException("Username '$normalizedUser' already exists. Please Sign In instead.")
            )
        }

        val passChars = password.toCharArray()
        val hashed = try {
            PasswordHasher.hash(passChars)
        } finally {
            passChars.fill('\u0000')
        }

        val entity = FarmerAccountEntity(
            username = normalizedUser,
            displayName = fullName.trim().take(60),
            saltHex = hashed.saltHex,
            passwordHashHex = hashed.hashHex,
            createdAtMs = System.currentTimeMillis()
        )

        return@withContext try {
            accountDao.insertAccount(entity)
            Result.success(entity)
        } catch (_: SQLiteConstraintException) {
            Result.failure(
                IllegalStateException("Username '$normalizedUser' already exists. Please Sign In instead.")
            )
        }
    }

    suspend fun loginAccount(
        username: String,
        password: String
    ): Result<FarmerAccountEntity> = withContext(Dispatchers.IO) {
        val normalizedUser = AccountInputValidator.normalizeUsername(username)
        if (normalizedUser.isEmpty() || password.isEmpty()) {
            return@withContext Result.failure(
                IllegalArgumentException("Please enter both username and password.")
            )
        }

        val account = accountDao.findByUsername(normalizedUser)
            ?: return@withContext Result.failure(
                IllegalArgumentException("Incorrect username or password. New farmer? Switch to Create Account.")
            )

        val passChars = password.toCharArray()
        val valid = try {
            PasswordHasher.verify(passChars, account.saltHex, account.passwordHashHex)
        } finally {
            passChars.fill('\u0000')
        }

        if (!valid) {
            return@withContext Result.failure(
                IllegalArgumentException("Incorrect username or password.")
            )
        }

        Result.success(account)
    }

    suspend fun findAccount(username: String): FarmerAccountEntity? = withContext(Dispatchers.IO) {
        val normalized = AccountInputValidator.normalizeUsername(username)
        if (normalized.isEmpty()) null else accountDao.findByUsername(normalized)
    }

    fun observeSavedLots(ownerUsername: String): Flow<List<SavedHarvestLot>> {
        val normalized = AccountInputValidator.normalizeUsername(ownerUsername)
        return accountDao.observeLocalSavedLots(normalized).map { rows ->
            rows.map { row ->
                SavedHarvestLot(
                    id = row.id,
                    userId = row.ownerUsername,
                    cropId = row.cropId,
                    cropName = row.cropName,
                    mandiName = row.mandiName,
                    quantityQuintals = row.quantityQuintals,
                    modalPricePerQuintal = row.modalPricePerQuintal,
                    netProfit = row.netProfit,
                    transportCost = row.transportCost,
                    verdict = row.verdict,
                    notes = row.notes,
                    createdAt = Timestamp(row.updatedAtMs / 1000L, ((row.updatedAtMs % 1000L) * 1_000_000).toInt()),
                    updatedAt = Timestamp(row.updatedAtMs / 1000L, ((row.updatedAtMs % 1000L) * 1_000_000).toInt())
                )
            }
        }
    }

    suspend fun saveHarvestLot(
        ownerUsername: String,
        cropId: String,
        cropName: String,
        mandiName: String,
        quantityQuintals: Double,
        modalPricePerQuintal: Double,
        netProfit: Double,
        transportCost: Double,
        verdict: String,
        notes: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUser = AccountInputValidator.normalizeUsername(ownerUsername)
            require(normalizedUser.isNotBlank()) { "Account required" }
            require(cropId.isNotBlank() && cropName.isNotBlank() && mandiName.isNotBlank())
            val entity = LocalSavedLotEntity(
                id = UUID.randomUUID().toString(),
                ownerUsername = normalizedUser,
                cropId = cropId.trim().take(40),
                cropName = cropName.trim().take(80),
                mandiName = mandiName.trim().take(120),
                quantityQuintals = quantityQuintals.coerceIn(0.1, 100000.0),
                modalPricePerQuintal = modalPricePerQuintal.coerceIn(1.0, 1000000.0),
                netProfit = netProfit.coerceIn(-10000000.0, 1000000000.0),
                transportCost = transportCost.coerceIn(0.0, 10000000.0),
                verdict = if (verdict == "HOLD_WAIT") "HOLD_WAIT" else "SELL_NOW",
                notes = notes?.trim()?.take(240)?.takeIf { it.isNotEmpty() },
                updatedAtMs = System.currentTimeMillis()
            )
            accountDao.upsertLocalSavedLot(entity)
        }
    }

    suspend fun deleteSavedLot(ownerUsername: String, lotId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUser = AccountInputValidator.normalizeUsername(ownerUsername)
            accountDao.deleteLocalSavedLot(lotId, normalizedUser)
        }
    }
}
