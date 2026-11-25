package com.example.data.firebase

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val opticMode: String = "EO_TACTICAL",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedBookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetType: String = "",
    val targetKey: String = "",
    val title: String = "",
    val subtitle: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DonationRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val paybill: String = MPESA_PAYBILL,
    val accountNumber: String = MPESA_ACCOUNT_NUMBER,
    val amountKes: Long = 0L,
    val donorPhone: String = "",
    val referenceCode: String = "",
    val status: String = "RECORDED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val MPESA_PAYBILL = "522522"
        const val MPESA_ACCOUNT_NUMBER = "1355643775"
    }
}

@Keep
@IgnoreExtraProperties
data class IntelChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val content: String = "",
    val modelId: String = "gemini-3.8-flash",
    val targetLat: Double = 0.0,
    val targetLon: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

class CallidusFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String?,
        defaultOptic: String
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            path = docRef.path
            val existing = docRef.get().await()
            val safeName = displayName.trim().ifEmpty { "Callidus Operator" }.take(80)
            val safeEmail = (email ?: "").trim().take(160)
            val safeOptic = defaultOptic.take(40)
            if (!existing.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "opticMode" to safeOptic,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateOpticPreference(opticMode: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            path = docRef.path
            docRef.update(
                mapOf(
                    "opticMode" to opticMode.take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeBookmarks(userId: String): Flow<List<SavedBookmark>> =
        db.collection("users").document(userId).collection("bookmarks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(SavedBookmark::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    suspend fun saveBookmark(
        targetType: String,
        targetKey: String,
        title: String,
        subtitle: String,
        lat: Double,
        lon: Double
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val safeDocId = targetKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
                .ifEmpty { "bm_${System.currentTimeMillis()}" }
            val docRef = db.collection("users").document(uid).collection("bookmarks").document(safeDocId)
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "targetType" to targetType.take(32).ifEmpty { "TARGET" },
                "targetKey" to targetKey.take(128).ifEmpty { safeDocId },
                "title" to title.trim().take(160).ifEmpty { "Pinned Target" },
                "subtitle" to subtitle.trim().take(240),
                "lat" to lat.coerceIn(-90.0, 90.0),
                "lon" to lon.coerceIn(-180.0, 180.0),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeBookmark(bookmarkId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            require(bookmarkId.isNotBlank()) { "Bookmark ID cannot be blank" }
            val docRef = db.collection("users").document(uid).collection("bookmarks").document(bookmarkId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeDonations(userId: String): Flow<List<DonationRecord>> =
        db.collection("users").document(userId).collection("donations")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(DonationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    suspend fun recordDonationPledge(
        amountKes: Long,
        donorPhone: String,
        referenceCode: String
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            require(amountKes in 10L..500_000L) { "Amount must be between KES 10 and KES 500,000" }
            val docRef = db.collection("users").document(uid).collection("donations").document()
            path = docRef.path
            val safeRef = referenceCode.trim().take(64)
            val status = if (safeRef.length >= 6) "CONFIRMED" else "RECORDED"
            val payload = mapOf(
                "userId" to uid,
                "paybill" to DonationRecord.MPESA_PAYBILL,
                "accountNumber" to DonationRecord.MPESA_ACCOUNT_NUMBER,
                "amountKes" to amountKes,
                "donorPhone" to donorPhone.trim().take(24),
                "referenceCode" to safeRef,
                "status" to status,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeIntelMessages(userId: String): Flow<List<IntelChatMessage>> =
        db.collection("users").document(userId).collection("intel_messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snap ->
                snap.toObjects(IntelChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    suspend fun saveIntelMessage(
        role: String,
        content: String,
        modelId: String,
        targetLat: Double,
        targetLon: Double
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val safeContent = content.trim().take(16000)
            if (safeContent.isEmpty()) return Result.success(Unit)
            val safeRole = if (role == "model") "model" else "user"
            val docRef = db.collection("users").document(uid).collection("intel_messages").document()
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "role" to safeRole,
                "content" to safeContent,
                "modelId" to modelId.take(64).ifEmpty { "gemini-3.8-flash" },
                "targetLat" to targetLat.coerceIn(-90.0, 90.0),
                "targetLon" to targetLon.coerceIn(-180.0, 180.0),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun clearIntelHistory(userId: String): Result<Unit> {
        var path = "users/$userId/intel_messages"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "User mismatch" }
            val docs = db.collection("users").document(uid).collection("intel_messages").get().await()
            for (doc in docs.documents) {
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
