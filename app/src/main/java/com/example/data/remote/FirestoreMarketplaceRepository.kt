package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.AccountRole
import com.example.data.local.ApprovalStatus
import com.example.data.local.EscrowStatus
import com.example.data.local.ListingCategory
import com.example.data.local.PaymentStatus
import com.example.data.local.SessionPreferences
import com.example.data.repository.MarketplaceRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

data class CommissionBreakdown(
    val grossAmountXaf: Long,
    val commissionRatePercent: Long,
    val commissionXaf: Long,
    val netSellerPayoutXaf: Long
)

class FirestoreMarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val sessionPreferences: SessionPreferences? = null
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
        sessionPreferences = SessionPreferences(context.applicationContext)
    )

    val currencyCode: Flow<String>
        get() = sessionPreferences?.currencyCode ?: flow { emit("XAF") }

    suspend fun setCurrencyCode(code: String) {
        sessionPreferences?.setCurrencyCode(code)
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isCeoAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(MarketplaceRepository.OWNER_EMAIL, ignoreCase = true) &&
            user.isEmailVerified
    }

    private fun requireCeoAdmin() {
        require(isCeoAdmin()) {
            "Only CEO ${MarketplaceRepository.CEO_NAME} (${MarketplaceRepository.OWNER_EMAIL}) is authorized for this action."
        }
    }

    fun observeUserProfile(uid: String): Flow<FirestoreUserProfile?> {
        val path = "users/$uid"
        return db.collection("users").document(uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    FirestoreUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path, uid)
                throw error
            }
    }

    suspend fun ensureUserProfile(
        fullNameFallback: String,
        email: String,
        phone: String = ""
    ): Result<FirestoreUserProfile> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            val isCeo = isCeoAdmin()
            if (existingSnap.exists()) {
                val existing = existingSnap.toObject(
                    FirestoreUserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) {
                    if (isCeo && existing.role != AccountRole.OWNER_ADMIN.name) {
                        docRef.update(
                            mapOf(
                                "role" to AccountRole.OWNER_ADMIN.name,
                                "fullName" to MarketplaceRepository.CEO_NAME,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                        return Result.success(
                            existing.copy(
                                role = AccountRole.OWNER_ADMIN.name,
                                fullName = MarketplaceRepository.CEO_NAME
                            )
                        )
                    }
                    return Result.success(existing)
                }
            }

            val rawName = if (isCeo) {
                MarketplaceRepository.CEO_NAME
            } else {
                fullNameFallback.trim().ifBlank { email.substringBefore("@") }
            }
            val resolvedName = rawName.let { if (it.length < 2) "Partner Seller" else it.take(80) }
            val resolvedEmail = email.trim().ifBlank { "member@gideonglobal.cm" }.take(120)
            val resolvedRole = if (isCeo) AccountRole.OWNER_ADMIN.name else AccountRole.USER.name
            val resolvedPhone = if (isCeo) {
                "${MarketplaceRepository.OWNER_PHONE_PRIMARY} / ${MarketplaceRepository.OWNER_PHONE_ORANGE_LOCAL}"
            } else {
                phone.trim().take(40)
            }

            val payload = mapOf(
                "userId" to uid,
                "fullName" to resolvedName,
                "email" to resolvedEmail,
                "phone" to resolvedPhone,
                "role" to resolvedRole,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val created = docRef.get().await().toObject(
                FirestoreUserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(
                created ?: FirestoreUserProfile(
                    id = uid,
                    userId = uid,
                    fullName = resolvedName,
                    email = resolvedEmail,
                    phone = resolvedPhone,
                    role = resolvedRole
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateSellerProfile(fullName: String, phone: String): Result<Unit> {
        val uid = requireUserId()
        val trimmedName = fullName.trim()
        val trimmedPhone = phone.trim()
        if (trimmedName.length < 2 || trimmedName.length > 80) {
            return Result.failure(IllegalArgumentException("Full name must be between 2 and 80 characters"))
        }
        if (trimmedPhone.length > 40) {
            return Result.failure(IllegalArgumentException("Phone number must be 40 characters or fewer"))
        }
        val docRef = db.collection("users").document(uid)
        return try {
            docRef.update(
                mapOf(
                    "fullName" to trimmedName,
                    "phone" to trimmedPhone,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun seedOfficialCatalogIfEmpty(): Result<Unit> {
        if (!isCeoAdmin()) return Result.success(Unit)
        val uid = requireUserId()
        return try {
            val existing = db.collection("listings")
                .whereEqualTo("approvalStatus", ApprovalStatus.APPROVED.name)
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) return Result.success(Unit)

            val defaults = listOf(
                mapOf(
                    "sellerUid" to uid,
                    "sellerName" to "${MarketplaceRepository.CEO_NAME} (${MarketplaceRepository.OWNER_DISPLAY_NAME})",
                    "sellerEmail" to MarketplaceRepository.OWNER_EMAIL,
                    "sellerPhone" to MarketplaceRepository.OWNER_PHONE_PRIMARY,
                    "title" to "500 m² Titled Residential Land — Bonapriso Annex",
                    "description" to "Prime flat dry land with full Land Title (Titre Foncier), paved road access, water & electricity poles on site. Directly verified by CEO LAGCHU LAGCHU GIDEON.",
                    "category" to ListingCategory.LAND.name,
                    "priceXaf" to 28_500_000L,
                    "location" to "Douala, Bonapriso",
                    "specs" to "500 m² • Titre Foncier Ready • Paved Access",
                    "imageUri" to "drawable://img_land_estate",
                    "isOwnerListing" to true,
                    "approvalStatus" to ApprovalStatus.APPROVED.name,
                    "paymentStatus" to PaymentStatus.NOT_REQUIRED.name,
                    "paymentMethod" to "Owner Direct",
                    "paymentReference" to "OWNER-OFFICIAL",
                    "payerPhone" to MarketplaceRepository.OWNER_PHONE_PRIMARY,
                    "listingFeeXaf" to 0L,
                    "commissionRatePercent" to 0L,
                    "adminNotes" to "Published & approved by CEO ${MarketplaceRepository.CEO_NAME}",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "sellerUid" to uid,
                    "sellerName" to "${MarketplaceRepository.CEO_NAME} (${MarketplaceRepository.OWNER_DISPLAY_NAME})",
                    "sellerEmail" to MarketplaceRepository.OWNER_EMAIL,
                    "sellerPhone" to MarketplaceRepository.OWNER_PHONE_SECONDARY,
                    "title" to "1,200 m² Commercial Highway Plot — Bastos Corridor",
                    "description" to "Strategic corner plot near diplomatic and commercial district. Clean boundary beacons and surveyed plan ready.",
                    "category" to ListingCategory.LAND.name,
                    "priceXaf" to 65_000_000L,
                    "location" to "Yaoundé, Bastos",
                    "specs" to "1,200 m² • Corner Plot • Surveyed Plan",
                    "imageUri" to "drawable://img_hero_gideon",
                    "isOwnerListing" to true,
                    "approvalStatus" to ApprovalStatus.APPROVED.name,
                    "paymentStatus" to PaymentStatus.NOT_REQUIRED.name,
                    "paymentMethod" to "Owner Direct",
                    "paymentReference" to "OWNER-OFFICIAL",
                    "payerPhone" to MarketplaceRepository.OWNER_PHONE_SECONDARY,
                    "listingFeeXaf" to 0L,
                    "commissionRatePercent" to 0L,
                    "adminNotes" to "Published & approved by CEO ${MarketplaceRepository.CEO_NAME}",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "sellerUid" to uid,
                    "sellerName" to "${MarketplaceRepository.CEO_NAME} (${MarketplaceRepository.OWNER_DISPLAY_NAME})",
                    "sellerEmail" to MarketplaceRepository.OWNER_EMAIL,
                    "sellerPhone" to MarketplaceRepository.OWNER_PHONE_PRIMARY,
                    "title" to "Flagship Executive Tech Bundle — Phone & Watch",
                    "description" to "Factory-unlocked 256GB flagship smartphone paired with stainless steel chronograph smartwatch. Inspected by Gideon Global Marketing.",
                    "category" to ListingCategory.ELECTRONICS.name,
                    "priceXaf" to 685_000L,
                    "location" to "Douala, Akwa",
                    "specs" to "256 GB • Factory Sealed • 1-Yr Warranty",
                    "imageUri" to "drawable://img_products_showcase",
                    "isOwnerListing" to true,
                    "approvalStatus" to ApprovalStatus.APPROVED.name,
                    "paymentStatus" to PaymentStatus.NOT_REQUIRED.name,
                    "paymentMethod" to "Owner Direct",
                    "paymentReference" to "OWNER-OFFICIAL",
                    "payerPhone" to MarketplaceRepository.OWNER_PHONE_PRIMARY,
                    "listingFeeXaf" to 0L,
                    "commissionRatePercent" to 0L,
                    "adminNotes" to "Published & approved by CEO ${MarketplaceRepository.CEO_NAME}",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            defaults.forEach { payload ->
                db.collection("listings").document().set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "listings", auth)
            Result.failure(e)
        }
    }

    fun observeApprovedListings(): Flow<List<FirestoreListing>> = flow {
        val uid = requireUserId()
        val path = "listings"
        emitAll(
            db.collection(path)
                .whereEqualTo("approvalStatus", ApprovalStatus.APPROVED.name)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        FirestoreListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<FirestoreListing> { it.isOwnerListing }
                            .thenByDescending { it.updatedAt?.seconds ?: 0L }
                    )
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMyListings(uid: String): Flow<List<FirestoreListing>> {
        val path = "listings"
        return db.collection(path)
            .whereEqualTo("sellerUid", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    FirestoreListing::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observePendingListingsForAdmin(): Flow<List<FirestoreListing>> = flow {
        val uid = requireUserId()
        val path = "listings"
        emitAll(
            db.collection(path)
                .whereEqualTo("approvalStatus", ApprovalStatus.PENDING_APPROVAL.name)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        FirestoreListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllListingsForAdmin(): Flow<List<FirestoreListing>> = flow {
        val uid = requireUserId()
        val path = "listings"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        FirestoreListing::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeEscrowTransactions(uid: String, isAdmin: Boolean): Flow<List<FirestoreEscrowTransaction>> {
        val path = "escrow_transactions"
        if (isAdmin) {
            return db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        FirestoreEscrowTransaction::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        }
        val buyerOrders = db.collection(path)
            .whereEqualTo("buyerUid", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    FirestoreEscrowTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
        val sellerOrders = db.collection(path)
            .whereEqualTo("sellerUid", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    FirestoreEscrowTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
        return combine(buyerOrders, sellerOrders) { bought, sold ->
            (bought + sold)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }.catch { error ->
            logFlowError(error, OperationType.LIST, path, uid)
            throw error
        }
    }

    suspend fun submitListing(
        profile: FirestoreUserProfile,
        title: String,
        description: String,
        category: ListingCategory,
        priceXaf: Long,
        location: String,
        specs: String,
        imageUri: String,
        contactPhone: String,
        paymentMethod: String,
        paymentReference: String,
        payerPhone: String
    ): Result<FirestoreListing> {
        val uid = requireUserId()
        val isOwner = isCeoAdmin()
        val trimmedTitle = title.trim()
        val trimmedDesc = description.trim()
        val trimmedLocation = location.trim()
        val trimmedPayRef = paymentReference.trim()

        if (trimmedTitle.length < 3 || trimmedTitle.length > 140) {
            return Result.failure(IllegalArgumentException("Title must be 3–140 characters"))
        }
        if (trimmedDesc.length < 5 || trimmedDesc.length > 2000) {
            return Result.failure(IllegalArgumentException("Description must be 5–2000 characters"))
        }
        if (trimmedLocation.length < 2 || trimmedLocation.length > 100) {
            return Result.failure(IllegalArgumentException("Location must be 2–100 characters"))
        }
        if (priceXaf <= 0L || priceXaf > 10_000_000_000L) {
            return Result.failure(IllegalArgumentException("Price must be a positive amount in FCFA"))
        }
        if (!isOwner && trimmedPayRef.length < 4) {
            return Result.failure(
                IllegalArgumentException("Enter your MTN MoMo / Orange Money payment transaction reference so CEO ${MarketplaceRepository.CEO_NAME} can approve your advert.")
            )
        }

        val resolvedImage = imageUri.ifBlank {
            if (category == ListingCategory.LAND) "drawable://img_land_estate" else "drawable://img_products_showcase"
        }.take(500)
        val commissionRate = if (isOwner) 0L else PARTNER_SHOP_COMMISSION_PERCENT
        val docRef = db.collection("listings").document()
        val sellerDisplay = if (isOwner) {
            "${MarketplaceRepository.CEO_NAME} (${MarketplaceRepository.OWNER_DISPLAY_NAME})"
        } else {
            profile.fullName.ifBlank { auth.currentUser?.displayName ?: "Partner Seller" }
        }.let { if (it.length < 2) "Partner Seller" else it.take(100) }
        val sellerEmail = (auth.currentUser?.email ?: profile.email).ifBlank { "seller@gideonglobal.cm" }.take(120)
        val payload = mapOf(
            "sellerUid" to uid,
            "sellerName" to sellerDisplay,
            "sellerEmail" to sellerEmail,
            "sellerPhone" to contactPhone.trim().ifBlank { profile.phone }.take(40),
            "title" to trimmedTitle,
            "description" to trimmedDesc,
            "category" to category.name,
            "priceXaf" to priceXaf,
            "location" to trimmedLocation,
            "specs" to specs.trim().take(200),
            "imageUri" to resolvedImage,
            "isOwnerListing" to isOwner,
            "approvalStatus" to if (isOwner) ApprovalStatus.APPROVED.name else ApprovalStatus.PENDING_APPROVAL.name,
            "paymentStatus" to if (isOwner) PaymentStatus.NOT_REQUIRED.name else PaymentStatus.PAYMENT_SUBMITTED.name,
            "paymentMethod" to (if (isOwner) "Owner Direct" else paymentMethod).take(80),
            "paymentReference" to (if (isOwner) "OWNER-OFFICIAL" else trimmedPayRef).take(80),
            "payerPhone" to (if (isOwner) MarketplaceRepository.OWNER_PHONE_PRIMARY else payerPhone.trim()).take(40),
            "listingFeeXaf" to if (isOwner) 0L else category.feeXaf,
            "commissionRatePercent" to commissionRate,
            "adminNotes" to if (isOwner) "Published & approved by CEO ${MarketplaceRepository.CEO_NAME}" else "",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            val saved = docRef.get().await().toObject(
                FirestoreListing::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(
                saved ?: FirestoreListing(
                    id = docRef.id,
                    sellerUid = uid,
                    sellerName = sellerDisplay,
                    sellerEmail = sellerEmail,
                    title = trimmedTitle,
                    category = category.name,
                    priceXaf = priceXaf,
                    location = trimmedLocation,
                    isOwnerListing = isOwner,
                    approvalStatus = if (isOwner) ApprovalStatus.APPROVED.name else ApprovalStatus.PENDING_APPROVAL.name,
                    commissionRatePercent = commissionRate
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun approveListing(listingId: String, note: String = ""): Result<Unit> {
        val docRef = db.collection("listings").document(listingId)
        return try {
            requireCeoAdmin()
            require(listingId.isNotBlank()) { "Listing ID cannot be blank" }
            docRef.update(
                mapOf(
                    "approvalStatus" to ApprovalStatus.APPROVED.name,
                    "paymentStatus" to PaymentStatus.PAYMENT_VERIFIED.name,
                    "adminNotes" to note.ifBlank {
                        "Payment verified & approved by CEO ${MarketplaceRepository.CEO_NAME} (${MarketplaceRepository.OWNER_EMAIL})"
                    }.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun rejectListing(listingId: String, reason: String): Result<Unit> {
        val docRef = db.collection("listings").document(listingId)
        return try {
            requireCeoAdmin()
            require(listingId.isNotBlank()) { "Listing ID cannot be blank" }
            docRef.update(
                mapOf(
                    "approvalStatus" to ApprovalStatus.REJECTED.name,
                    "paymentStatus" to PaymentStatus.REFUNDED.name,
                    "adminNotes" to reason.ifBlank {
                        "Rejected by CEO ${MarketplaceRepository.CEO_NAME}. Contact ${MarketplaceRepository.OWNER_EMAIL}."
                    }.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        val docRef = db.collection("listings").document(listingId)
        return try {
            requireCeoAdmin()
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun createEscrowPurchase(
        listing: FirestoreListing,
        buyerName: String,
        buyerEmail: String,
        buyerPhone: String,
        paymentChannel: String,
        buyerPaymentRef: String
    ): Result<FirestoreEscrowTransaction> {
        val buyerUid = requireUserId()
        if (listing.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Listing ID is required for escrow"))
        }
        if (buyerName.trim().length < 2) {
            return Result.failure(IllegalArgumentException("Enter your full name"))
        }
        if (buyerPhone.trim().length < 6) {
            return Result.failure(IllegalArgumentException("Enter a valid phone number"))
        }
        if (buyerPaymentRef.trim().length < 4) {
            return Result.failure(IllegalArgumentException("Enter your MoMo / Orange payment transaction reference"))
        }

        val breakdown = calculateCommission(
            grossAmountXaf = listing.priceXaf,
            isOwnerListing = listing.isOwnerListing,
            ratePercent = listing.commissionRatePercent
        )
        val refCode = "GGM-${Random.nextInt(100000, 999999)}"
        val docRef = db.collection("escrow_transactions").document()
        val resolvedEmail = buyerEmail.trim().ifBlank { auth.currentUser?.email ?: "buyer@gideonglobal.cm" }.take(120)
        val payload = mapOf(
            "referenceCode" to refCode,
            "listingId" to listing.id,
            "listingTitle" to listing.title.take(140),
            "listingCategory" to listing.category.take(40),
            "amountXaf" to breakdown.grossAmountXaf,
            "commissionRatePercent" to breakdown.commissionRatePercent,
            "commissionXaf" to breakdown.commissionXaf,
            "netSellerPayoutXaf" to breakdown.netSellerPayoutXaf,
            "buyerUid" to buyerUid,
            "buyerName" to buyerName.trim().take(100),
            "buyerEmail" to resolvedEmail,
            "buyerPhone" to buyerPhone.trim().take(40),
            "sellerUid" to listing.sellerUid.ifBlank { buyerUid },
            "sellerName" to listing.sellerName.ifBlank { "Partner Seller" }.take(100),
            "sellerPhone" to listing.sellerPhone.take(40),
            "paymentChannel" to paymentChannel.take(80),
            "buyerPaymentRef" to buyerPaymentRef.trim().take(80),
            "escrowStatus" to EscrowStatus.ESCROW_PENDING_VERIFY.name,
            "deliveryNote" to "Awaiting Gideon Global escrow verification (${MarketplaceRepository.OWNER_PHONE_PRIMARY}).",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            val saved = docRef.get().await().toObject(
                FirestoreEscrowTransaction::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(
                saved ?: FirestoreEscrowTransaction(
                    id = docRef.id,
                    referenceCode = refCode,
                    listingId = listing.id,
                    listingTitle = listing.title,
                    listingCategory = listing.category,
                    amountXaf = breakdown.grossAmountXaf,
                    commissionRatePercent = breakdown.commissionRatePercent,
                    commissionXaf = breakdown.commissionXaf,
                    netSellerPayoutXaf = breakdown.netSellerPayoutXaf,
                    buyerUid = buyerUid,
                    buyerName = buyerName.trim(),
                    buyerEmail = resolvedEmail,
                    buyerPhone = buyerPhone.trim(),
                    sellerUid = listing.sellerUid,
                    sellerName = listing.sellerName,
                    sellerPhone = listing.sellerPhone,
                    paymentChannel = paymentChannel,
                    buyerPaymentRef = buyerPaymentRef.trim(),
                    escrowStatus = EscrowStatus.ESCROW_PENDING_VERIFY.name
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateEscrowStatus(
        txId: String,
        newStatus: EscrowStatus,
        note: String
    ): Result<Unit> {
        val docRef = db.collection("escrow_transactions").document(txId)
        return try {
            requireUserId()
            docRef.update(
                mapOf(
                    "escrowStatus" to newStatus.name,
                    "deliveryNote" to note.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    private fun logFlowError(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val ex = firestoreError ?: (error as? Exception) ?: return
        if (auth.currentUser?.uid == uid) {
            handleFirestoreError(ex, op, path, auth)
        }
    }

    companion object {
        const val PARTNER_SHOP_COMMISSION_PERCENT = 5L

        fun calculateCommission(
            grossAmountXaf: Long,
            isOwnerListing: Boolean,
            ratePercent: Long = if (isOwnerListing) 0L else PARTNER_SHOP_COMMISSION_PERCENT
        ): CommissionBreakdown {
            val effectiveRate = if (isOwnerListing) 0L else ratePercent.coerceIn(0L, 50L)
            val commission = (grossAmountXaf * effectiveRate) / 100L
            val netPayout = grossAmountXaf - commission
            return CommissionBreakdown(
                grossAmountXaf = grossAmountXaf,
                commissionRatePercent = effectiveRate,
                commissionXaf = commission,
                netSellerPayoutXaf = netPayout
            )
        }
    }
}
