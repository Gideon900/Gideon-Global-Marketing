package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.AccountRole
import com.example.data.local.ApprovalStatus
import com.example.data.local.EscrowStatus
import com.example.data.local.EscrowTransactionEntity
import com.example.data.local.ListingCategory
import com.example.data.local.ListingDao
import com.example.data.local.ListingEntity
import com.example.data.local.PaymentStatus
import com.example.data.local.SessionPreferences
import com.example.data.local.TransactionDao
import com.example.data.local.UserAccountEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceRepository(
    private val listingDao: ListingDao,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val sessionPreferences: SessionPreferences
) {

    val approvedListings: Flow<List<ListingEntity>> = listingDao.observeApprovedListings()
    val allListings: Flow<List<ListingEntity>> = listingDao.observeAllListings()
    val pendingApprovalListings: Flow<List<ListingEntity>> = listingDao.observePendingApprovalListings()
    val allTransactions: Flow<List<EscrowTransactionEntity>> = transactionDao.observeAllTransactions()
    val currencyCode: Flow<String> = sessionPreferences.currencyCode

    val currentAccount: Flow<UserAccountEntity?> = sessionPreferences.loggedInEmail.flatMapLatest { email ->
        if (email.isNullOrBlank()) {
            flowOf(null)
        } else {
            accountDao.observeAccountByEmail(email)
        }
    }

    fun observeListingById(id: Long): Flow<ListingEntity?> = listingDao.observeListingById(id)

    fun observeApprovedLandListings(): Flow<List<ListingEntity>> =
        listingDao.observeApprovedListingsByCategory(ListingCategory.LAND.name)

    fun observeApprovedProductListings(): Flow<List<ListingEntity>> =
        listingDao.observeApprovedProductListings()

    fun searchApprovedListings(query: String): Flow<List<ListingEntity>> =
        listingDao.searchApprovedListings(query.trim())

    fun observeSellerListings(email: String): Flow<List<ListingEntity>> =
        listingDao.observeListingsBySeller(email)

    fun observeBuyerTransactions(email: String): Flow<List<EscrowTransactionEntity>> =
        transactionDao.observeTransactionsForBuyer(email)

    suspend fun updateListingDetails(
        listingId: Long,
        priceXaf: Long,
        description: String
    ): Result<ListingEntity> {
        if (priceXaf <= 0L) {
            return Result.failure(IllegalArgumentException("Price must be greater than zero"))
        }
        if (description.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Description must be at least 5 characters"))
        }
        val existing = listingDao.getListingById(listingId)
            ?: return Result.failure(IllegalArgumentException("Listing #$listingId not found"))
        val updated = existing.copy(
            priceXaf = priceXaf,
            description = description.trim(),
            updatedAtMillis = System.currentTimeMillis()
        )
        listingDao.updateListing(updated)
        return Result.success(updated)
    }

    suspend fun ensureSeeded() {
        val existingOwner = accountDao.getAccountByEmail(OWNER_EMAIL)
        if (existingOwner == null) {
            accountDao.insertAccount(
                UserAccountEntity(
                    email = OWNER_EMAIL,
                    fullName = CEO_NAME,
                    phone = "$OWNER_PHONE_PRIMARY / $OWNER_PHONE_ORANGE_LOCAL",
                    role = AccountRole.OWNER_ADMIN.name,
                    pinCode = DEFAULT_OWNER_PIN
                )
            )
        } else if (existingOwner.fullName != CEO_NAME) {
            accountDao.updateAccount(existingOwner.copy(fullName = CEO_NAME))
        }

        if (listingDao.countListings() == 0) {
            val now = System.currentTimeMillis()
            val seedListings = listOf(
                ListingEntity(
                    title = "500 m² Titled Residential Land — Bonapriso Annex",
                    description = "Prime flat dry land with full Land Title (Titre Foncier), paved road access, water & electricity poles on site. Ideal for residential duplex or commercial apartments. Directly verified by Gideon Global.",
                    category = ListingCategory.LAND.name,
                    priceXaf = 28_500_000L,
                    location = "Douala, Bonapriso",
                    specs = "500 m² • Titre Foncier Ready • Paved Access",
                    imageUri = "drawable://img_land_estate",
                    sellerName = OWNER_DISPLAY_NAME,
                    sellerEmail = OWNER_EMAIL,
                    sellerPhone = OWNER_PHONE_PRIMARY,
                    isOwnerListing = true,
                    approvalStatus = ApprovalStatus.APPROVED.name,
                    paymentStatus = PaymentStatus.NOT_REQUIRED.name,
                    paymentMethod = "Owner Direct",
                    paymentReference = "OWNER-OFFICIAL",
                    payerPhone = OWNER_PHONE_PRIMARY,
                    listingFeeXaf = 0L,
                    adminNotes = "Official Gideon Global Property",
                    createdAtMillis = now - 86_400_000L * 3,
                    updatedAtMillis = now - 86_400_000L * 3
                ),
                ListingEntity(
                    title = "1,200 m² Commercial Highway Plot — Bastos Corridor",
                    description = "Strategic corner plot near diplomatic and commercial district. Clean boundary beacons, surveyed plan available, zero disputes. Safe escrow purchase supported.",
                    category = ListingCategory.LAND.name,
                    priceXaf = 65_000_000L,
                    location = "Yaoundé, Bastos",
                    specs = "1,200 m² • Corner Plot • Surveyed Plan",
                    imageUri = "drawable://img_hero_gideon",
                    sellerName = OWNER_DISPLAY_NAME,
                    sellerEmail = OWNER_EMAIL,
                    sellerPhone = OWNER_PHONE_SECONDARY,
                    isOwnerListing = true,
                    approvalStatus = ApprovalStatus.APPROVED.name,
                    paymentStatus = PaymentStatus.NOT_REQUIRED.name,
                    paymentMethod = "Owner Direct",
                    paymentReference = "OWNER-OFFICIAL",
                    payerPhone = OWNER_PHONE_SECONDARY,
                    listingFeeXaf = 0L,
                    adminNotes = "Official Gideon Global Property",
                    createdAtMillis = now - 86_400_000L * 2,
                    updatedAtMillis = now - 86_400_000L * 2
                ),
                ListingEntity(
                    title = "Flagship Executive Tech Bundle — Phone & Watch",
                    description = "Factory-unlocked 256GB flagship smartphone paired with stainless steel chronograph smartwatch. Inspected and sealed by Gideon Global Marketing with 1-year warranty.",
                    category = ListingCategory.ELECTRONICS.name,
                    priceXaf = 685_000L,
                    location = "Douala, Akwa",
                    specs = "256 GB • Factory Sealed • 1-Yr Warranty",
                    imageUri = "drawable://img_products_showcase",
                    sellerName = OWNER_DISPLAY_NAME,
                    sellerEmail = OWNER_EMAIL,
                    sellerPhone = OWNER_PHONE_PRIMARY,
                    isOwnerListing = true,
                    approvalStatus = ApprovalStatus.APPROVED.name,
                    paymentStatus = PaymentStatus.NOT_REQUIRED.name,
                    paymentMethod = "Owner Direct",
                    paymentReference = "OWNER-OFFICIAL",
                    payerPhone = OWNER_PHONE_PRIMARY,
                    listingFeeXaf = 0L,
                    adminNotes = "Official Gideon Global Product",
                    createdAtMillis = now - 86_400_000L,
                    updatedAtMillis = now - 86_400_000L
                ),
                ListingEntity(
                    title = "400 m² Fertile Agricultural & Eco-Plot — Molyko",
                    description = "Elevated scenic parcel in Buea with panoramic mountain views and fast road link. Seller advert payment verified and approved by Gideon Global Admin.",
                    category = ListingCategory.LAND.name,
                    priceXaf = 12_500_000L,
                    location = "Buea, Molyko",
                    specs = "400 m² • Verified Deed • Mountain View",
                    imageUri = "drawable://img_land_estate",
                    sellerName = "Samuel Ndikum",
                    sellerEmail = "samuel.ndikum@seller.cm",
                    sellerPhone = "+237677112233",
                    isOwnerListing = false,
                    approvalStatus = ApprovalStatus.APPROVED.name,
                    paymentStatus = PaymentStatus.PAYMENT_VERIFIED.name,
                    paymentMethod = "MTN Mobile Money (+237 676 831 188)",
                    paymentReference = "MP261114.0942.B71204",
                    payerPhone = "+237677112233",
                    listingFeeXaf = 15_000L,
                    adminNotes = "MTN MoMo listing fee verified by agchu30@gmail.com",
                    createdAtMillis = now - 43_200_000L,
                    updatedAtMillis = now - 40_000_000L
                ),
                ListingEntity(
                    title = "Solar Power Home Inverter Kit 5kVA (New)",
                    description = "Complete hybrid solar inverter + 4 monocrystalline panels + lithium battery bank. Awaiting Gideon Global Admin payment verification before going live.",
                    category = ListingCategory.HOME.name,
                    priceXaf = 1_450_000L,
                    location = "Bamenda, Commercial Ave",
                    specs = "5kVA Hybrid • Lithium Bank • 4 Panels",
                    imageUri = "drawable://img_products_showcase",
                    sellerName = "Clarisse Fon",
                    sellerEmail = "clarisse.fon@vendor.cm",
                    sellerPhone = "+237681994455",
                    isOwnerListing = false,
                    approvalStatus = ApprovalStatus.PENDING_APPROVAL.name,
                    paymentStatus = PaymentStatus.PAYMENT_SUBMITTED.name,
                    paymentMethod = "Orange Money (+237 683 480 584)",
                    paymentReference = "OM261116.1018.C90341",
                    payerPhone = "+237681994455",
                    listingFeeXaf = 5_000L,
                    adminNotes = "",
                    createdAtMillis = now - 3_600_000L,
                    updatedAtMillis = now - 3_600_000L
                )
            )
            seedListings.forEach { listingDao.insertListing(it) }
        }

        if (transactionDao.countTransactions() == 0) {
            transactionDao.insertTransaction(
                EscrowTransactionEntity(
                    referenceCode = "GGM-740219",
                    listingId = 1L,
                    listingTitle = "Flagship Executive Tech Bundle — Phone & Watch",
                    listingCategory = ListingCategory.ELECTRONICS.name,
                    amountXaf = 685_000L,
                    buyerName = "Jean-Paul Mbarga",
                    buyerEmail = "jpmbarga@client.cm",
                    buyerPhone = "+237699334411",
                    sellerName = OWNER_DISPLAY_NAME,
                    sellerPhone = OWNER_PHONE_PRIMARY,
                    paymentChannel = "MTN Mobile Money (+237 676 831 188)",
                    buyerPaymentRef = "MP261115.1640.K44102",
                    escrowStatus = EscrowStatus.ESCROW_SECURED.name,
                    deliveryNote = "Payment confirmed by Gideon Global. Ready for insured dispatch to Yaoundé.",
                    createdAtMillis = System.currentTimeMillis() - 18_000_000L
                )
            )
        }
    }

    suspend fun loginOwner(emailInput: String, pinInput: String): Result<UserAccountEntity> {
        val cleanEmail = emailInput.trim().lowercase()
        if (cleanEmail != OWNER_EMAIL) {
            return Result.failure(IllegalArgumentException("Owner admin access is linked to $OWNER_EMAIL"))
        }
        val owner = accountDao.getAccountByEmail(OWNER_EMAIL)
            ?: return Result.failure(IllegalStateException("Owner account not initialized"))
        if (pinInput.trim() != owner.pinCode) {
            return Result.failure(IllegalArgumentException("Incorrect Owner PIN"))
        }
        sessionPreferences.setLoggedInEmail(owner.email)
        return Result.success(owner)
    }

    suspend fun signInOrRegisterUser(
        fullName: String,
        email: String,
        phone: String,
        pinCode: String
    ): Result<UserAccountEntity> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address"))
        }
        if (cleanEmail == OWNER_EMAIL) {
            return loginOwner(cleanEmail, pinCode)
        }
        val existing = accountDao.getAccountByEmail(cleanEmail)
        if (existing != null) {
            if (pinCode.isNotBlank() && existing.pinCode != pinCode.trim()) {
                return Result.failure(IllegalArgumentException("Incorrect account PIN for $cleanEmail"))
            }
            sessionPreferences.setLoggedInEmail(existing.email)
            return Result.success(existing)
        }
        if (fullName.trim().length < 2) {
            return Result.failure(IllegalArgumentException("Enter your full name to register"))
        }
        if (phone.trim().length < 8) {
            return Result.failure(IllegalArgumentException("Enter a valid phone number"))
        }
        val effectivePin = pinCode.trim().ifBlank { "1234" }
        val account = UserAccountEntity(
            email = cleanEmail,
            fullName = fullName.trim(),
            phone = phone.trim(),
            role = AccountRole.USER.name,
            pinCode = effectivePin
        )
        accountDao.insertAccount(account)
        sessionPreferences.setLoggedInEmail(account.email)
        return Result.success(account)
    }

    suspend fun signOut() {
        sessionPreferences.setLoggedInEmail(null)
    }

    suspend fun updateOwnerPin(newPin: String): Result<Unit> {
        if (newPin.trim().length < 4) {
            return Result.failure(IllegalArgumentException("PIN must be at least 4 characters"))
        }
        val owner = accountDao.getAccountByEmail(OWNER_EMAIL)
            ?: return Result.failure(IllegalStateException("Owner account not found"))
        accountDao.updateAccount(owner.copy(pinCode = newPin.trim()))
        return Result.success(Unit)
    }

    suspend fun setCurrencyCode(code: String) {
        sessionPreferences.setCurrencyCode(code)
    }

    fun isOwnerAdmin(account: UserAccountEntity?): Boolean {
        return account != null &&
            account.role == AccountRole.OWNER_ADMIN.name &&
            account.email.equals(OWNER_EMAIL, ignoreCase = true)
    }

    suspend fun submitListing(
        account: UserAccountEntity,
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
    ): Result<ListingEntity> {
        val isOwner = isOwnerAdmin(account)

        if (!isOwner && paymentReference.trim().length < 4) {
            return Result.failure(
                IllegalArgumentException("Please enter your Mobile Money / Bank payment transaction reference so CEO $CEO_NAME can approve your advert.")
            )
        }

        val resolvedImage = imageUri.ifBlank {
            if (category == ListingCategory.LAND) "drawable://img_land_estate" else "drawable://img_products_showcase"
        }

        val now = System.currentTimeMillis()
        val entity = ListingEntity(
            title = title.trim(),
            description = description.trim(),
            category = category.name,
            priceXaf = priceXaf,
            location = location.trim(),
            specs = specs.trim(),
            imageUri = resolvedImage,
            sellerName = if (isOwner) "$CEO_NAME ($OWNER_DISPLAY_NAME)" else account.fullName,
            sellerEmail = account.email,
            sellerPhone = contactPhone.trim().ifBlank { account.phone },
            isOwnerListing = isOwner,
            approvalStatus = if (isOwner) ApprovalStatus.APPROVED.name else ApprovalStatus.PENDING_APPROVAL.name,
            paymentStatus = if (isOwner) PaymentStatus.NOT_REQUIRED.name else PaymentStatus.PAYMENT_SUBMITTED.name,
            paymentMethod = if (isOwner) "Owner Direct" else paymentMethod,
            paymentReference = if (isOwner) "OWNER-OFFICIAL" else paymentReference.trim(),
            payerPhone = if (isOwner) OWNER_PHONE_PRIMARY else payerPhone.trim(),
            listingFeeXaf = if (isOwner) 0L else category.feeXaf,
            adminNotes = if (isOwner) "Published & approved by CEO $CEO_NAME ($OWNER_EMAIL)" else "",
            createdAtMillis = now,
            updatedAtMillis = now
        )
        val newId = listingDao.insertListing(entity)
        return Result.success(entity.copy(id = newId))
    }

    suspend fun approveListing(actingAccount: UserAccountEntity?, listing: ListingEntity, note: String = "") {
        require(isOwnerAdmin(actingAccount)) {
            "Only CEO $CEO_NAME ($OWNER_EMAIL) is authorized to approve adverts."
        }
        listingDao.updateListing(
            listing.copy(
                approvalStatus = ApprovalStatus.APPROVED.name,
                paymentStatus = if (listing.isOwnerListing) PaymentStatus.NOT_REQUIRED.name else PaymentStatus.PAYMENT_VERIFIED.name,
                adminNotes = note.ifBlank { "Payment verified & approved by CEO $CEO_NAME ($OWNER_EMAIL)" },
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun rejectListing(actingAccount: UserAccountEntity?, listing: ListingEntity, reason: String) {
        require(isOwnerAdmin(actingAccount)) {
            "Only CEO $CEO_NAME ($OWNER_EMAIL) is authorized to reject adverts."
        }
        listingDao.updateListing(
            listing.copy(
                approvalStatus = ApprovalStatus.REJECTED.name,
                paymentStatus = if (listing.paymentStatus == PaymentStatus.PAYMENT_SUBMITTED.name) {
                    PaymentStatus.REFUNDED.name
                } else {
                    listing.paymentStatus
                },
                adminNotes = reason.ifBlank { "Rejected by CEO $CEO_NAME. Contact $OWNER_EMAIL." },
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteListing(actingAccount: UserAccountEntity?, listingId: Long) {
        require(isOwnerAdmin(actingAccount)) {
            "Only CEO $CEO_NAME ($OWNER_EMAIL) is authorized to remove marketplace adverts."
        }
        listingDao.deleteListingById(listingId)
    }

    suspend fun createEscrowPurchase(
        listing: ListingEntity,
        buyerName: String,
        buyerEmail: String,
        buyerPhone: String,
        paymentChannel: String,
        buyerPaymentRef: String
    ): Result<EscrowTransactionEntity> {
        if (buyerName.trim().length < 2) {
            return Result.failure(IllegalArgumentException("Enter your full name"))
        }
        if (buyerPhone.trim().length < 8) {
            return Result.failure(IllegalArgumentException("Enter a valid phone number"))
        }
        if (buyerPaymentRef.trim().length < 4) {
            return Result.failure(IllegalArgumentException("Enter your escrow payment transaction reference"))
        }
        val refCode = "GGM-${Random.nextInt(100000, 999999)}"
        val tx = EscrowTransactionEntity(
            referenceCode = refCode,
            listingId = listing.id,
            listingTitle = listing.title,
            listingCategory = listing.category,
            amountXaf = listing.priceXaf,
            buyerName = buyerName.trim(),
            buyerEmail = buyerEmail.trim().ifBlank { "guest@gideonglobal.cm" },
            buyerPhone = buyerPhone.trim(),
            sellerName = listing.sellerName,
            sellerPhone = listing.sellerPhone,
            paymentChannel = paymentChannel,
            buyerPaymentRef = buyerPaymentRef.trim(),
            escrowStatus = EscrowStatus.ESCROW_PENDING_VERIFY.name,
            deliveryNote = "Awaiting Gideon Global escrow confirmation ($OWNER_PHONE_PRIMARY / $OWNER_PHONE_SECONDARY)."
        )
        val id = transactionDao.insertTransaction(tx)
        return Result.success(tx.copy(id = id))
    }

    suspend fun updateEscrowStatus(
        tx: EscrowTransactionEntity,
        newStatus: EscrowStatus,
        note: String
    ) {
        transactionDao.updateTransaction(
            tx.copy(
                escrowStatus = newStatus.name,
                deliveryNote = note
            )
        )
    }

    companion object {
        const val CEO_NAME = "LAGCHU LAGCHU GIDEON"
        const val OWNER_EMAIL = "agchu30@gmail.com"
        const val OWNER_DISPLAY_NAME = "Gideon Global Marketing"
        const val OWNER_PHONE_PRIMARY = "+237676831188"
        const val OWNER_PHONE_SECONDARY = "+237683480584"
        const val OWNER_PHONE_ORANGE_LOCAL = "683480584"
        const val DEFAULT_OWNER_PIN = "2370"
    }
}
