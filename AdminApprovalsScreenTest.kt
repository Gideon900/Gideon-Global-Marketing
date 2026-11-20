package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.local.ApprovalStatus
import com.example.data.remote.FirestoreListing
import com.example.ui.screens.AdminApprovalsScreen
import com.example.ui.theme.GideonGlobalTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class AdminApprovalsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val samplePending = FirestoreListing(
        id = "pending_1",
        sellerUid = "seller_1",
        sellerName = "Clarisse Fon",
        sellerEmail = "clarisse@vendor.cm",
        sellerPhone = "+237681994455",
        title = "5kVA Hybrid Solar Power Kit",
        description = "Complete solar power system with lithium battery.",
        category = "ELECTRONICS",
        priceXaf = 1_450_000L,
        location = "Douala, Akwa",
        approvalStatus = ApprovalStatus.PENDING_APPROVAL.name,
        paymentMethod = "MTN Mobile Money (+237 676 831 188)",
        paymentReference = "MP261119.0942.A10023",
        payerPhone = "+237681994455",
        listingFeeXaf = 5_000L,
        commissionRatePercent = 5L
    )

    @Test
    fun nonAdminUserSeesAuthorizationRequiredGate() {
        composeRule.setContent {
            GideonGlobalTheme {
                AdminApprovalsScreen(
                    isCeoAdmin = false,
                    pendingListings = listOf(samplePending),
                    allListings = listOf(samplePending),
                    currencyCode = "XAF",
                    onApproveListing = {},
                    onRejectListing = { _, _ -> },
                    onListingClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("admin_access_denied_card").assertIsDisplayed()
        composeRule.onNodeWithTag("one_tap_approve_btn_pending_1").assertDoesNotExist()
    }

    @Test
    fun ceoAdminSupportsOneTapApproveAndOneTapReject() {
        var approvedId: String? = null
        var rejectedId: String? = null

        composeRule.setContent {
            GideonGlobalTheme {
                AdminApprovalsScreen(
                    isCeoAdmin = true,
                    pendingListings = listOf(samplePending),
                    allListings = listOf(samplePending),
                    currencyCode = "XAF",
                    onApproveListing = { approvedId = it.id },
                    onRejectListing = { listing, _ -> rejectedId = listing.id },
                    onListingClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("admin_approval_card_pending_1").assertIsDisplayed()
        composeRule.onNodeWithTag("one_tap_approve_btn_pending_1")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals("pending_1", approvedId)

        composeRule.onNodeWithTag("one_tap_reject_btn_pending_1")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals("pending_1", rejectedId)
    }
}
