package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.data.repository.MarketplaceRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun appNameAndOwnerContactResourcesMatchGideonGlobal() {
        assertEquals("Gideon Global", context.getString(R.string.app_name))
        assertEquals("LAGCHU LAGCHU GIDEON", context.getString(R.string.ceo_name))
        assertEquals("agchu30@gmail.com", context.getString(R.string.owner_email))
        assertEquals("+237676831188", context.getString(R.string.owner_phone_primary))
        assertEquals("+237683480584", context.getString(R.string.owner_phone_secondary))
        assertEquals(
            "ai-studio-android-gideongl-d408afe8-9d94-4fa7-8cea-713bd69e0adf",
            context.getString(R.string.firestore_database_id)
        )
    }

    @Test
    fun partnerShopCommissionIsFivePercentAndOwnerDirectIsZeroPercent() {
        val partner = FirestoreMarketplaceRepository.calculateCommission(
            grossAmountXaf = 1_000_000L,
            isOwnerListing = false
        )
        assertEquals(5L, partner.commissionRatePercent)
        assertEquals(50_000L, partner.commissionXaf)
        assertEquals(950_000L, partner.netSellerPayoutXaf)

        val ownerDirect = FirestoreMarketplaceRepository.calculateCommission(
            grossAmountXaf = 1_000_000L,
            isOwnerListing = true
        )
        assertEquals(0L, ownerDirect.commissionRatePercent)
        assertEquals(0L, ownerDirect.commissionXaf)
        assertEquals(1_000_000L, ownerDirect.netSellerPayoutXaf)
        assertEquals("+237676831188", MarketplaceRepository.OWNER_PHONE_PRIMARY)
    }
}
