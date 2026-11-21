package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {
    @Query("SELECT * FROM listings WHERE approvalStatus = 'APPROVED' ORDER BY isOwnerListing DESC, updatedAtMillis DESC")
    fun observeApprovedListings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings ORDER BY createdAtMillis DESC")
    fun observeAllListings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE approvalStatus = 'PENDING_APPROVAL' ORDER BY createdAtMillis ASC")
    fun observePendingApprovalListings(): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE LOWER(sellerEmail) = LOWER(:email) ORDER BY createdAtMillis DESC")
    fun observeListingsBySeller(email: String): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE approvalStatus = 'APPROVED' AND category = :category ORDER BY isOwnerListing DESC, updatedAtMillis DESC")
    fun observeApprovedListingsByCategory(category: String): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE approvalStatus = 'APPROVED' AND category != 'LAND' ORDER BY isOwnerListing DESC, updatedAtMillis DESC")
    fun observeApprovedProductListings(): Flow<List<ListingEntity>>

    @Query(
        "SELECT * FROM listings WHERE approvalStatus = 'APPROVED' AND (" +
            "LOWER(title) LIKE '%' || LOWER(:query) || '%' OR " +
            "LOWER(description) LIKE '%' || LOWER(:query) || '%' OR " +
            "LOWER(location) LIKE '%' || LOWER(:query) || '%'" +
            ") ORDER BY isOwnerListing DESC, updatedAtMillis DESC"
    )
    fun searchApprovedListings(query: String): Flow<List<ListingEntity>>

    @Query("SELECT * FROM listings WHERE id = :id LIMIT 1")
    fun observeListingById(id: Long): Flow<ListingEntity?>

    @Query("SELECT * FROM listings WHERE id = :id LIMIT 1")
    suspend fun getListingById(id: Long): ListingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: ListingEntity): Long

    @Update
    suspend fun updateListing(listing: ListingEntity)

    @Query(
        "UPDATE listings SET approvalStatus = :approvalStatus, paymentStatus = :paymentStatus, " +
            "adminNotes = :adminNotes, updatedAtMillis = :updatedAtMillis WHERE id = :id"
    )
    suspend fun updateApprovalStatus(
        id: Long,
        approvalStatus: String,
        paymentStatus: String,
        adminNotes: String,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM listings WHERE id = :id")
    suspend fun deleteListingById(id: Long)

    @Query("SELECT COUNT(*) FROM listings")
    suspend fun countListings(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM escrow_transactions ORDER BY createdAtMillis DESC")
    fun observeAllTransactions(): Flow<List<EscrowTransactionEntity>>

    @Query("SELECT * FROM escrow_transactions WHERE LOWER(buyerEmail) = LOWER(:email) ORDER BY createdAtMillis DESC")
    fun observeTransactionsForBuyer(email: String): Flow<List<EscrowTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: EscrowTransactionEntity): Long

    @Update
    suspend fun updateTransaction(tx: EscrowTransactionEntity)

    @Query("SELECT COUNT(*) FROM escrow_transactions")
    suspend fun countTransactions(): Int
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM user_accounts WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    fun observeAccountByEmail(email: String): Flow<UserAccountEntity?>

    @Query("SELECT * FROM user_accounts WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getAccountByEmail(email: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccountEntity)

    @Update
    suspend fun updateAccount(account: UserAccountEntity)
}
