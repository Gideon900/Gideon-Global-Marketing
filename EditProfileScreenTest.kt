package com.example.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.data.local.ApprovalStatus
import com.example.data.local.PaymentStatus
import com.example.data.remote.FirestoreListing
import com.example.data.remote.FirestoreUserProfile
import com.example.ui.screens.EditProfileScreen
import com.example.ui.theme.GideonGlobalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class EditProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleProfile = FirestoreUserProfile(
        id = "seller_42",
        userId = "seller_42",
        fullName = "Douala Ventures",
        email = "seller@doualaventures.cm",
        phone = "+237670000111",
        role = "USER"
    )

    private val submittedAdverts = listOf(
        FirestoreListing(
            id = "adv_pending",
            sellerUid = "seller_42",
            sellerName = "Douala Ventures",
            sellerEmail = "seller@doualaventures.cm",
            sellerPhone = "+237670000111",
            title = "Commercial Plot in Bonapriso",
            category = "LAND",
            priceXaf = 45_000_000L,
            location = "Douala, Bonapriso",
            approvalStatus = ApprovalStatus.PENDING_APPROVAL.name,
            paymentStatus = PaymentStatus.PAYMENT_SUBMITTED.name,
            paymentMethod = "MTN Mobile Money (+237 676 831 188)",
            paymentReference = "MP261119.1015.C88210",
            listingFeeXaf = 15_000L,
            commissionRatePercent = 5L
        ),
        FirestoreListing(
            id = "adv_approved",
            sellerUid = "seller_42",
            sellerName = "Douala Ventures",
            sellerEmail = "seller@doualaventures.cm",
            sellerPhone = "+237670000111",
            title = "MacBook Pro M3 16-inch",
            category = "ELECTRONICS",
            priceXaf = 1_650_000L,
            location = "Douala, Akwa",
            approvalStatus = ApprovalStatus.APPROVED.name,
            paymentStatus = PaymentStatus.PAYMENT_VERIFIED.name,
            paymentMethod = "Orange Money (+237 683 480 584)",
            paymentReference = "OM261118.0811.D22190",
            listingFeeXaf = 5_000L,
            commissionRatePercent = 5L,
            adminNotes = "Verified and live on marketplace."
        ),
        FirestoreListing(
            id = "adv_rejected",
            sellerUid = "seller_42",
            sellerName = "Douala Ventures",
            sellerEmail = "seller@doualaventures.cm",
            sellerPhone = "+237670000111",
            title = "Damaged Generator Lot",
            category = "HOME",
            priceXaf = 250_000L,
            location = "Yaounde, Bastos",
            approvalStatus = ApprovalStatus.REJECTED.name,
            paymentStatus = PaymentStatus.REFUNDED.name,
            paymentMethod = "MTN Mobile Money (+237 676 831 188)",
            paymentReference = "MP261117.1430.B11002",
            listingFeeXaf = 5_000L,
            commissionRatePercent = 5L,
            adminNotes = "Please upload clear serial number photos."
        )
    )

    @Test
    fun validatesShortNameAndSavesUpdatedContactInformation() {
        var savedName: String? = null
        var savedPhone: String? = null

        composeRule.setContent {
            GideonGlobalTheme {
                EditProfileScreen(
                    userProfile = sampleProfile,
                    userEmail = sampleProfile.email,
                    userDisplayName = sampleProfile.fullName,
                    isCeoAdmin = false,
                    myListings = emptyList(),
                    currencyCode = "XAF",
                    onSaveProfile = { name, phone ->
                        savedName = name
                        savedPhone = phone
                    },
                    onListingClick = {},
                    onCreateAdvert = {},
                    onBack = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edit_profile_name_input")
            .performScrollTo()
            .performTextReplacement("A")
        composeRule.onNodeWithTag("edit_profile_save_button")
            .performScrollTo()
            .performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edit_profile_validation_error").assertIsDisplayed()
        assertNull(savedName)

        composeRule.onNodeWithTag("edit_profile_name_input")
            .performScrollTo()
            .performTextReplacement("Bamenda Solar Hub")
        composeRule.onNodeWithTag("edit_profile_phone_input")
            .performScrollTo()
            .performTextReplacement("+237677112233")
        composeRule.onNodeWithTag("edit_profile_save_button")
            .performScrollTo()
            .performClick()

        composeRule.waitForIdle()
        assertEquals("Bamenda Solar Hub", savedName)
        assertEquals("+237677112233", savedPhone)
    }

    @Test
    fun displaysSubmittedAdvertStatusesFiltersAndHandlesClick() {
        var clickedListingId: String? = null

        composeRule.setContent {
            GideonGlobalTheme {
                EditProfileScreen(
                    userProfile = sampleProfile,
                    userEmail = sampleProfile.email,
                    userDisplayName = sampleProfile.fullName,
                    isCeoAdmin = false,
                    myListings = submittedAdverts,
                    currencyCode = "XAF",
                    onSaveProfile = { _, _ -> },
                    onListingClick = { clickedListingId = it.id },
                    onCreateAdvert = {},
                    onBack = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edit_profile_list")
            .performScrollToNode(hasTestTag("profile_advert_summary_row"))
        composeRule.onNodeWithTag("profile_advert_summary_row").assertIsDisplayed()
        composeRule.onNodeWithText("Pending (1)").assertIsDisplayed()
        composeRule.onNodeWithText("Approved (1)").assertIsDisplayed()
        composeRule.onNodeWithText("Rejected (1)").assertIsDisplayed()

        composeRule.onNodeWithTag("edit_profile_list")
            .performScrollToNode(hasTestTag("profile_advert_card_adv_pending"))
        composeRule.onNodeWithTag("profile_advert_card_adv_pending")
            .assertIsDisplayed()
            .performClick()
        assertEquals("adv_pending", clickedListingId)

        composeRule.onNodeWithTag("edit_profile_list")
            .performScrollToNode(hasTestTag("profile_advert_summary_row"))
        composeRule.onNodeWithTag("profile_filter_rejected")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edit_profile_list")
            .performScrollToNode(hasTestTag("profile_advert_card_adv_rejected"))
        composeRule.onNodeWithTag("profile_advert_card_adv_rejected")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edit_profile_list")
            .performScrollToNode(hasText("CEO Admin Note: Please upload clear serial number photos."))
        composeRule.onNodeWithText("CEO Admin Note: Please upload clear serial number photos.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("profile_advert_card_adv_pending").assertDoesNotExist()
    }
}
