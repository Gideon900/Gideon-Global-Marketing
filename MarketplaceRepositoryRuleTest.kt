package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.AccountRole
import com.example.data.local.ApprovalStatus
import com.example.data.local.EscrowStatus
import com.example.data.local.ListingCategory
import com.example.data.local.PaymentStatus
import com.example.data.repository.MarketplaceRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun repo(): FirestoreMarketplaceRepository =
        FirestoreMarketplaceRepository(db = db, auth = auth, sessionPreferences = null)

    @Test
    fun unauthenticatedQueryIsRejectedByFirestoreRules() = runBlocking(Dispatchers.IO) {
        auth.signOut()
        var denied = false
        try {
            repo().observeApprovedListings().first()
        } catch (e: Throwable) {
            val firestoreEx = generateSequence(e) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED || e is IllegalStateException) {
                denied = true
            }
        }
        assertTrue("Unauthenticated read should be blocked", denied)
    }

    @Test
    fun sellerRegistrationAndPaidAdvertRequireCeoAgchu30Approval() = runBlocking(Dispatchers.IO) {
        val repository = repo()

        // 1. Register Seller and update seller contact phone
        val sellerUser = signInTestUser("pauline.seller@gideontest.cm")
        val sellerProfile = repository.ensureUserProfile(
            fullNameFallback = "Pauline Etoga Shop",
            email = "pauline.seller@gideontest.cm"
        ).getOrThrow()
        assertEquals(AccountRole.USER.name, sellerProfile.role)
        repository.updateSellerProfile("Pauline Etoga Shop", "+237677112233").getOrThrow()

        // 2. Seller submits paid Land advert via MTN Mobile Money (+237676831188)
        val pendingLand = repository.submitListing(
            profile = sellerProfile.copy(phone = "+237677112233"),
            title = "600 m² Titled Land in Kribi",
            description = "Coastal residential plot with Titre Foncier ready.",
            category = ListingCategory.LAND,
            priceXaf = 22_000_000L,
            location = "Kribi",
            specs = "600 m² • Titre Foncier",
            imageUri = "",
            contactPhone = "+237677112233",
            paymentMethod = "MTN Mobile Money (${MarketplaceRepository.OWNER_PHONE_PRIMARY})",
            paymentReference = "MP261119.1045.K99123",
            payerPhone = "+237677112233"
        ).getOrThrow()

        assertEquals(ApprovalStatus.PENDING_APPROVAL.name, pendingLand.approvalStatus)
        assertEquals(PaymentStatus.PAYMENT_SUBMITTED.name, pendingLand.paymentStatus)
        assertEquals(5L, pendingLand.commissionRatePercent)
        assertFalse(pendingLand.isOwnerListing)

        // Seller cannot self-approve their advert
        val selfApproveResult = repository.approveListing(pendingLand.id, "Self approve")
        assertTrue("Seller must not be able to self-approve", selfApproveResult.isFailure)

        // 3. CEO agchu30@gmail.com signs in and verifies payment -> approves listing
        auth.signOut()
        val ceoUser = signInTestUser(MarketplaceRepository.OWNER_EMAIL, verifyEmail = true)
        assertTrue(repository.isCeoAdmin())
        val ceoProfile = repository.ensureUserProfile(
            fullNameFallback = MarketplaceRepository.CEO_NAME,
            email = MarketplaceRepository.OWNER_EMAIL
        ).getOrThrow()
        assertEquals(AccountRole.OWNER_ADMIN.name, ceoProfile.role)
        assertEquals(MarketplaceRepository.CEO_NAME, ceoProfile.fullName)

        repository.approveListing(pendingLand.id, "Verified MTN MoMo payment by CEO LAGCHU LAGCHU GIDEON")
            .getOrThrow()

        // 4. Buyer signs in, views approved listing, and places Escrow order with 5% Partner Shop Commission
        auth.signOut()
        signInTestUser("marc.buyer@gideontest.cm")
        val approved = repository.observeApprovedListings().first()
        assertTrue(approved.any { it.id == pendingLand.id && it.sellerUid == sellerUser.uid })

        val escrowOrder = repository.createEscrowPurchase(
            listing = pendingLand.copy(approvalStatus = ApprovalStatus.APPROVED.name),
            buyerName = "Marc Talla",
            buyerEmail = "marc.buyer@gideontest.cm",
            buyerPhone = "+237690112233",
            paymentChannel = "MTN Mobile Money (+237 676 831 188)",
            buyerPaymentRef = "MP261119.1200.B88110"
        ).getOrThrow()

        assertEquals(22_000_000L, escrowOrder.amountXaf)
        assertEquals(5L, escrowOrder.commissionRatePercent)
        assertEquals(1_100_000L, escrowOrder.commissionXaf)
        assertEquals(20_900_000L, escrowOrder.netSellerPayoutXaf)
        assertEquals(EscrowStatus.ESCROW_PENDING_VERIFY.name, escrowOrder.escrowStatus)

        // 5. CEO confirms payment and completes escrow release
        auth.signOut()
        signInTestUser(MarketplaceRepository.OWNER_EMAIL, verifyEmail = true)
        assertEquals(ceoUser.email, auth.currentUser?.email)
        repository.updateEscrowStatus(
            txId = escrowOrder.id,
            newStatus = EscrowStatus.ESCROW_SECURED,
            note = "MTN MoMo deposit confirmed by CEO"
        ).getOrThrow()
        repository.updateEscrowStatus(
            txId = escrowOrder.id,
            newStatus = EscrowStatus.COMPLETED_RELEASED,
            note = "Land documents handed over; 5% partner commission settled"
        ).getOrThrow()

        val adminTxs = repository.observeEscrowTransactions(ceoUser.uid, isAdmin = true).first()
        assertTrue(
            adminTxs.any {
                it.id == escrowOrder.id && it.escrowStatus == EscrowStatus.COMPLETED_RELEASED.name
            }
        )
    }

    @Test
    fun ceoDirectProductListingPublishesImmediatelyWithZeroCommission() = runBlocking(Dispatchers.IO) {
        val repository = repo()
        val ceoUser = signInTestUser(MarketplaceRepository.OWNER_EMAIL, verifyEmail = true)
        val ceoProfile = repository.ensureUserProfile(
            fullNameFallback = MarketplaceRepository.CEO_NAME,
            email = MarketplaceRepository.OWNER_EMAIL
        ).getOrThrow()

        val product = repository.submitListing(
            profile = ceoProfile,
            title = "5kVA Hybrid Solar Power Kit",
            description = "Brand new 5kVA solar inverter with lithium battery pack, 2-year warranty.",
            category = ListingCategory.ELECTRONICS,
            priceXaf = 1_200_000L,
            location = "Douala, Akwa",
            specs = "5kVA • Lithium • 2-Yr Warranty",
            imageUri = "",
            contactPhone = MarketplaceRepository.OWNER_PHONE_PRIMARY,
            paymentMethod = "",
            paymentReference = "",
            payerPhone = ""
        ).getOrThrow()

        assertTrue(product.isOwnerListing)
        assertEquals(ApprovalStatus.APPROVED.name, product.approvalStatus)
        assertEquals(0L, product.commissionRatePercent)
        assertEquals(ceoUser.uid, product.sellerUid)
    }
}
