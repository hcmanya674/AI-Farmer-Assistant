package com.example

import com.example.core.PasswordHasher
import com.example.data.local.FarmerAccountDao
import com.example.data.local.FarmerAccountEntity
import com.example.data.local.LocalSavedLotEntity
import com.example.data.repository.AccountInputValidator
import com.example.data.repository.LocalAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeFarmerAccountDao : FarmerAccountDao {
    private val accounts = mutableMapOf<String, FarmerAccountEntity>()
    private val lotsFlow = MutableStateFlow<List<LocalSavedLotEntity>>(emptyList())

    override suspend fun findByUsername(username: String): FarmerAccountEntity? = accounts[username]

    override suspend fun insertAccount(account: FarmerAccountEntity) {
        require(!accounts.containsKey(account.username)) { "Duplicate user" }
        accounts[account.username] = account
    }

    override fun observeLocalSavedLots(ownerUsername: String): Flow<List<LocalSavedLotEntity>> =
        lotsFlow.map { rows ->
            rows.filter { it.ownerUsername == ownerUsername }.sortedByDescending { it.updatedAtMs }
        }

    override suspend fun upsertLocalSavedLot(lot: LocalSavedLotEntity) {
        lotsFlow.value = lotsFlow.value.filterNot { it.id == lot.id } + lot
    }

    override suspend fun deleteLocalSavedLot(lotId: String, ownerUsername: String) {
        lotsFlow.value = lotsFlow.value.filterNot { it.id == lotId && it.ownerUsername == ownerUsername }
    }
}

class LocalAuthTest {

    @Test
    fun passwordHasherProducesUniqueSaltAndVerifiesCorrectPassword() {
        val h1 = PasswordHasher.hash("kisan2026".toCharArray())
        val h2 = PasswordHasher.hash("kisan2026".toCharArray())

        assertNotEquals(h1.saltHex, h2.saltHex)
        assertNotEquals(h1.hashHex, h2.hashHex)
        assertTrue(PasswordHasher.verify("kisan2026".toCharArray(), h1.saltHex, h1.hashHex))
        assertFalse(PasswordHasher.verify("wrongpass".toCharArray(), h1.saltHex, h1.hashHex))
    }

    @Test
    fun validatorRejectsWeakOrInvalidInputs() {
        assertTrue(AccountInputValidator.validateFullName("A") != null)
        assertTrue(AccountInputValidator.validateUsername("ab") != null)
        assertTrue(AccountInputValidator.validateUsername("bad user!") != null)
        assertTrue(AccountInputValidator.validatePassword("12345") != null)
        assertEquals(null, AccountInputValidator.validateUsername("Ramesh_Patil01"))
    }

    @Test
    fun registerLoginAndIsolatedSavedPlansWorkEndToEnd() = runTest {
        val repo = LocalAuthRepository(FakeFarmerAccountDao())

        val signup = repo.registerAccount(
            fullName = "Ramesh Patil",
            username = "Ramesh_01",
            password = "harvest99",
            confirmPassword = "harvest99"
        )
        assertTrue(signup.isSuccess)
        assertEquals("ramesh_01", signup.getOrThrow().username)

        val duplicate = repo.registerAccount(
            fullName = "Another User",
            username = "ramesh_01",
            password = "harvest99",
            confirmPassword = "harvest99"
        )
        assertTrue(duplicate.isFailure)

        val wrongPass = repo.loginAccount("ramesh_01", "wrongpass")
        assertTrue(wrongPass.isFailure)

        val validLogin = repo.loginAccount("  RAMESH_01 ", "harvest99")
        assertTrue(validLogin.isSuccess)
        assertEquals("Ramesh Patil", validLogin.getOrThrow().displayName)

        repo.saveHarvestLot(
            ownerUsername = "ramesh_01",
            cropId = "sugarcane",
            cropName = "Sugarcane",
            mandiName = "Mandya Yard",
            quantityQuintals = 80.0,
            modalPricePerQuintal = 348.0,
            netProfit = 27000.0,
            transportCost = 840.0,
            verdict = "SELL_NOW",
            notes = "Mandya, Karnataka"
        )
        val rameshLots = repo.observeSavedLots("ramesh_01").first()
        val otherLots = repo.observeSavedLots("suresh_02").first()

        assertEquals(1, rameshLots.size)
        assertEquals(0, otherLots.size)
    }
}
