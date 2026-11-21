package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ListingCategory(val label: String, val feeXaf: Long) {
    LAND("Land & Property", 15_000L),
    ELECTRONICS("Electronics", 5_000L),
    VEHICLES("Vehicles", 10_000L),
    HOME("Home & Living", 5_000L),
    FASHION("Fashion & Luxury", 3_500L),
    AGRICULTURE("Agriculture & Goods", 3_500L),
    SERVICES("Business Services", 5_000L);

    companion object {
        fun fromName(name: String): ListingCategory =
            entries.firstOrNull { it.name == name } ?: LAND
    }
}

enum class ApprovalStatus(val label: String) {
    PENDING_APPROVAL("Pending Approval"),
    APPROVED("Approved & Live"),
    REJECTED("Rejected");

    companion object {
        fun fromName(name: String): ApprovalStatus =
            entries.firstOrNull { it.name == name } ?: PENDING_APPROVAL
    }
}

enum class PaymentStatus(val label: String) {
    NOT_REQUIRED("Owner Direct"),
    PAYMENT_SUBMITTED("Payment Submitted"),
    PAYMENT_VERIFIED("Payment Verified"),
    REFUNDED("Refunded");

    companion object {
        fun fromName(name: String): PaymentStatus =
            entries.firstOrNull { it.name == name } ?: PAYMENT_SUBMITTED
    }
}

enum class EscrowStatus(val label: String) {
    ESCROW_PENDING_VERIFY("Verifying Payment"),
    ESCROW_SECURED("Funds Secured in Escrow"),
    ITEM_DELIVERED("Delivered / Handover"),
    COMPLETED_RELEASED("Completed & Released"),
    DISPUTED("Under Review");

    companion object {
        fun fromName(name: String): EscrowStatus =
            entries.firstOrNull { it.name == name } ?: ESCROW_PENDING_VERIFY
    }
}

enum class AccountRole {
    OWNER_ADMIN,
    USER
}

@Entity(tableName = "listings")
data class ListingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val category: String,
    val priceXaf: Long,
    val location: String,
    val specs: String,
    val imageUri: String,
    val sellerName: String,
    val sellerEmail: String,
    val sellerPhone: String,
    val isOwnerListing: Boolean,
    val approvalStatus: String,
    val paymentStatus: String,
    val paymentMethod: String,
    val paymentReference: String,
    val payerPhone: String,
    val listingFeeXaf: Long,
    val adminNotes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "escrow_transactions")
data class EscrowTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val referenceCode: String,
    val listingId: Long,
    val listingTitle: String,
    val listingCategory: String,
    val amountXaf: Long,
    val buyerName: String,
    val buyerEmail: String,
    val buyerPhone: String,
    val sellerName: String,
    val sellerPhone: String,
    val paymentChannel: String,
    val buyerPaymentRef: String,
    val escrowStatus: String,
    val deliveryNote: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val email: String,
    val fullName: String,
    val phone: String,
    val role: String,
    val pinCode: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
