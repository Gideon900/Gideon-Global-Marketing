package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "USER",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreListing(
    @DocumentId val id: String = "",
    val sellerUid: String = "",
    val sellerName: String = "",
    val sellerEmail: String = "",
    val sellerPhone: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "LAND",
    val priceXaf: Long = 0L,
    val location: String = "",
    val specs: String = "",
    val imageUri: String = "",
    @get:PropertyName("isOwnerListing")
    @PropertyName("isOwnerListing")
    val isOwnerListing: Boolean = false,
    val approvalStatus: String = "PENDING_APPROVAL",
    val paymentStatus: String = "PAYMENT_SUBMITTED",
    val paymentMethod: String = "",
    val paymentReference: String = "",
    val payerPhone: String = "",
    val listingFeeXaf: Long = 0L,
    val commissionRatePercent: Long = 5L,
    val adminNotes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreEscrowTransaction(
    @DocumentId val id: String = "",
    val referenceCode: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val listingCategory: String = "",
    val amountXaf: Long = 0L,
    val commissionRatePercent: Long = 5L,
    val commissionXaf: Long = 0L,
    val netSellerPayoutXaf: Long = 0L,
    val buyerUid: String = "",
    val buyerName: String = "",
    val buyerEmail: String = "",
    val buyerPhone: String = "",
    val sellerUid: String = "",
    val sellerName: String = "",
    val sellerPhone: String = "",
    val paymentChannel: String = "",
    val buyerPaymentRef: String = "",
    val escrowStatus: String = "ESCROW_PENDING_VERIFY",
    val deliveryNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
