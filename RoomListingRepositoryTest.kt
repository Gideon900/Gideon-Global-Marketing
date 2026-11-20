package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.MarketplaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomListingRepositoryTest {

    private lateinit var database: GideonDatabase
    private lateinit var repository: MarketplaceRepository

    @Before
    fun setUp() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, GideonDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MarketplaceRepository(
            listingDao = database.listingDao(),
            transactionDao = database.transactionDao(),
            accountDao = database.accountDao(),
            sessionPreferences = SessionPreferences(context)
        )
        repository.ensureSeeded()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun roomSchemaSupportsLandAndProductListingsWithPriceDescriptionAndAdminApproval() = runBlocking {
        val seller = repository.signInOrRegisterUser(
            fullName = "Pauline Etoga",
            email = "pauline.room@seller.cm",
            phone = "+237677001122",
            pinCode = "1234"
        ).getOrThrow()

        val landListing = repository.submitListing(
            account = seller,
            title = "750 m² Residential Plot — Kribi",
            description = "Surveyed titled plot with paved road access and water connection.",
            category = ListingCategory.LAND,
            priceXaf = 24_000_000L,
            location = "Kribi",
            specs = "750 m² • Titre Foncier",
            imageUri = "",
            contactPhone = seller.phone,
            paymentMethod = "MTN Mobile Money (+237 676 831 188)",
            paymentReference = "MP261119.0815.L10293",
            payerPhone = seller.phone
        ).getOrThrow()

        val productListing = repository.submitListing(
            account = seller,
            title = "5kVA Hybrid Solar Inverter",
            description = "Heavy duty solar power kit with lithium battery backup.",
            category = ListingCategory.ELECTRONICS,
            priceXaf = 1_150_000L,
            location = "Douala",
            specs = "5kVA • Lithium • 2-Yr Warranty",
            imageUri = "",
            contactPhone = seller.phone,
            paymentMethod = "Orange Money (+237 683 480 584)",
            paymentReference = "OM261119.0820.P44551",
            payerPhone = seller.phone
        ).getOrThrow()

        // Newly submitted third-party adverts start in PENDING_APPROVAL
        val pending = repository.pendingApprovalListings.first()
        assertTrue(pending.any { it.id == landListing.id && it.approvalStatus == ApprovalStatus.PENDING_APPROVAL.name })
        assertTrue(pending.any { it.id == productListing.id && it.approvalStatus == ApprovalStatus.PENDING_APPROVAL.name })

        // Non-admin seller cannot approve listings
        var selfApproveBlocked = false
        try {
            repository.approveListing(seller, landListing)
        } catch (_: IllegalArgumentException) {
            selfApproveBlocked = true
        }
        assertTrue(selfApproveBlocked)

        // Update price and description before approval
        val updatedLand = repository.updateListingDetails(
            listingId = landListing.id,
            priceXaf = 25_500_000L,
            description = "Updated: 750 m² surveyed residential plot near beach highway."
        ).getOrThrow()
        assertEquals(25_500_000L, updatedLand.priceXaf)

        // Owner admin approves land listing and rejects product listing
        val admin = repository.loginOwner(
            MarketplaceRepository.OWNER_EMAIL,
            MarketplaceRepository.DEFAULT_OWNER_PIN
        ).getOrThrow()

        repository.approveListing(admin, updatedLand, "Payment verified by CEO LAGCHU LAGCHU GIDEON")
        repository.rejectListing(admin, productListing, "Invalid Orange Money reference")

        val approvedLands = repository.observeApprovedLandListings().first()
        assertTrue(approvedLands.any { it.id == landListing.id && it.priceXaf == 25_500_000L })

        val searchResults = repository.searchApprovedListings("beach highway").first()
        assertEquals(1, searchResults.size)
        assertEquals(landListing.id, searchResults.first().id)

        val approvedProducts = repository.observeApprovedProductListings().first()
        assertTrue(approvedProducts.none { it.id == productListing.id })

        val rejectedRow = database.listingDao().getListingById(productListing.id)!!
        assertEquals(ApprovalStatus.REJECTED.name, rejectedRow.approvalStatus)
    }
}
