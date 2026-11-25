package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.AppLanguage
import com.example.data.remote.FarmerProfile
import com.example.data.remote.OperationType
import com.example.data.remote.SavedHarvestLot
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FarmerCloudRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeFarmerProfile(): Flow<FarmerProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            FarmerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeSavedLots(): Flow<List<SavedHarvestLot>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/saved_lots"
        emitAll(
            db.collection("users").document(uid).collection("saved_lots")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SavedHarvestLot::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureFarmerProfile(
        displayName: String,
        preferredLanguage: AppLanguage,
        selectedCropId: String,
        preferredVehicleRate: Double
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val cleanName = displayName.trim().take(80).ifBlank { "Farmer" }
                val cleanCrop = selectedCropId.trim().take(40).ifBlank { "sugarcane" }
                val cleanRate = preferredVehicleRate.coerceIn(1.0, 500.0)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "preferredLanguage" to preferredLanguage.code,
                    "selectedCropId" to cleanCrop,
                    "preferredVehicleRate" to cleanRate,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateFarmerPreferences(
        preferredLanguage: AppLanguage,
        selectedCropId: String,
        preferredVehicleRate: Double
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val cleanCrop = selectedCropId.trim().take(40).ifBlank { "sugarcane" }
            val cleanRate = preferredVehicleRate.coerceIn(1.0, 500.0)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "preferredLanguage" to preferredLanguage.code,
                        "selectedCropId" to cleanCrop,
                        "preferredVehicleRate" to cleanRate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val displayName = auth.currentUser?.displayName?.trim()?.take(80)?.ifBlank { "Farmer" } ?: "Farmer"
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to displayName,
                        "preferredLanguage" to preferredLanguage.code,
                        "selectedCropId" to cleanCrop,
                        "preferredVehicleRate" to cleanRate,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveHarvestLot(
        cropId: String,
        cropName: String,
        mandiName: String,
        quantityQuintals: Double,
        modalPricePerQuintal: Double,
        netProfit: Double,
        transportCost: Double,
        verdict: String,
        notes: String? = null
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/saved_lots"
        return try {
            val uid = requireUserId()
            require(cropId.isNotBlank() && cropId.length <= 40) { "Invalid cropId" }
            require(cropName.isNotBlank() && cropName.length <= 80) { "Invalid cropName" }
            require(mandiName.isNotBlank() && mandiName.length <= 120) { "Invalid mandiName" }
            require(quantityQuintals > 0.0 && quantityQuintals <= 100000.0) { "Invalid quantity" }
            require(modalPricePerQuintal > 0.0 && modalPricePerQuintal <= 1000000.0) { "Invalid rate" }

            val normalizedVerdict = if (verdict == "HOLD_WAIT") "HOLD_WAIT" else "SELL_NOW"
            val docRef = db.collection("users").document(uid).collection("saved_lots").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "cropId" to cropId.trim().take(40),
                "cropName" to cropName.trim().take(80),
                "mandiName" to mandiName.trim().take(120),
                "quantityQuintals" to quantityQuintals,
                "modalPricePerQuintal" to modalPricePerQuintal,
                "netProfit" to netProfit.coerceIn(-10000000.0, 1000000000.0),
                "transportCost" to transportCost.coerceIn(0.0, 10000000.0),
                "verdict" to normalizedVerdict,
                "notes" to notes?.trim()?.take(240)?.takeIf { it.isNotEmpty() },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedLot(lotId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/saved_lots/$lotId"
        return try {
            val uid = requireUserId()
            require(lotId.isNotBlank()) { "Lot ID cannot be blank" }
            val docRef = db.collection("users").document(uid).collection("saved_lots").document(lotId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
