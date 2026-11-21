package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.EscrowStatus
import com.example.data.local.ListingCategory
import com.example.data.remote.FirestoreEscrowTransaction
import com.example.data.remote.FirestoreListing
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.data.remote.FirestoreUserProfile
import com.example.data.repository.MarketplaceRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PartnerCommissionSummary(
    val partnerOrderCount: Int = 0,
    val grossPartnerSalesXaf: Long = 0L,
    val totalCommissionsXaf: Long = 0L,
    val totalNetPayoutsXaf: Long = 0L
)

class MainViewModel(
    private val repository: FirestoreMarketplaceRepository,
    private val currentUserId: String,
    private val currentUserEmail: String,
    private val currentUserDisplayName: String,
    val isCeoAdmin: Boolean
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ListingCategory?>(null)
    val selectedCategory: StateFlow<ListingCategory?> = _selectedCategory.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val currencyCode: StateFlow<String> = repository.currencyCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "XAF")

    val userProfile: StateFlow<FirestoreUserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            handleStreamError("userProfile", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val approvedListingsStream = repository.observeApprovedListings()
        .catch { error ->
            handleStreamError("approvedListings", error)
            emit(emptyList())
        }

    val filteredApprovedListings: StateFlow<List<FirestoreListing>> = combine(
        approvedListingsStream,
        _searchQuery,
        _selectedCategory
    ) { listings, query, category ->
        val q = query.trim().lowercase()
        listings.filter { item ->
            val matchesCategory = category == null || item.category == category.name
            val matchesQuery = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.location.lowercase().contains(q) ||
                item.description.lowercase().contains(q) ||
                item.specs.lowercase().contains(q)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val pendingApprovalListings: StateFlow<List<FirestoreListing>> =
        (if (isCeoAdmin) repository.observePendingListingsForAdmin() else flowOf(emptyList()))
            .catch { error ->
                handleStreamError("pendingListings", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val mySubmittedListings: StateFlow<List<FirestoreListing>> = repository.observeMyListings(currentUserId)
        .catch { error ->
            handleStreamError("myListings", error)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val allAccessibleListings: StateFlow<List<FirestoreListing>> =
        if (isCeoAdmin) {
            repository.observeAllListingsForAdmin()
                .catch { error ->
                    handleStreamError("allListings", error)
                    emit(emptyList())
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
        } else {
            combine(approvedListingsStream, mySubmittedListings) { approved, mine ->
                (approved + mine).distinctBy { it.id }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())
        }

    val visibleTransactions: StateFlow<List<FirestoreEscrowTransaction>> =
        repository.observeEscrowTransactions(currentUserId, isCeoAdmin)
            .catch { error ->
                handleStreamError("escrowTransactions", error)
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val commissionSummary: StateFlow<PartnerCommissionSummary> = visibleTransactions
        .map { txs ->
            val partnerTxs = txs.filter { it.commissionRatePercent > 0L }
            PartnerCommissionSummary(
                partnerOrderCount = partnerTxs.size,
                grossPartnerSalesXaf = partnerTxs.sumOf { it.amountXaf },
                totalCommissionsXaf = partnerTxs.sumOf { it.commissionXaf },
                totalNetPayoutsXaf = partnerTxs.sumOf { it.netSellerPayoutXaf }
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            PartnerCommissionSummary()
        )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                fullNameFallback = currentUserDisplayName,
                email = currentUserEmail
            ).onFailure { err ->
                _events.emit(err.localizedMessage ?: "Failed to load account profile")
            }
            if (isCeoAdmin) {
                repository.seedOfficialCatalogIfEmpty()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: ListingCategory?) {
        _selectedCategory.value = category
    }

    fun toggleCurrency() {
        viewModelScope.launch {
            val next = if (currencyCode.value == "XAF") "USD" else "XAF"
            repository.setCurrencyCode(next)
        }
    }

    fun updateSellerProfile(fullName: String, phone: String) {
        viewModelScope.launch {
            repository.updateSellerProfile(fullName, phone)
                .onSuccess { _events.emit("Seller profile saved") }
                .onFailure { _events.emit(it.message ?: "Failed to update profile") }
        }
    }

    fun submitListing(
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
        payerPhone: String,
        onSubmitted: (FirestoreListing) -> Unit
    ) {
        val profile = userProfile.value ?: FirestoreUserProfile(
            id = currentUserId,
            userId = currentUserId,
            fullName = currentUserDisplayName.ifBlank { currentUserEmail.substringBefore("@") },
            email = currentUserEmail,
            phone = contactPhone
        )
        viewModelScope.launch {
            repository.submitListing(
                profile = profile,
                title = title,
                description = description,
                category = category,
                priceXaf = priceXaf,
                location = location,
                specs = specs,
                imageUri = imageUri,
                contactPhone = contactPhone,
                paymentMethod = paymentMethod,
                paymentReference = paymentReference,
                payerPhone = payerPhone
            ).onSuccess { saved ->
                val msg = if (saved.isOwnerListing) {
                    "Official advert published live by CEO ${MarketplaceRepository.CEO_NAME}"
                } else {
                    "Advert & payment submitted! Awaiting approval from CEO ${MarketplaceRepository.CEO_NAME} (${MarketplaceRepository.OWNER_EMAIL})."
                }
                _events.emit(msg)
                onSubmitted(saved)
            }.onFailure {
                _events.emit(it.message ?: "Could not submit advert")
            }
        }
    }

    fun approveListing(listing: FirestoreListing, note: String = "") {
        viewModelScope.launch {
            repository.approveListing(listing.id, note)
                .onSuccess {
                    _events.emit("Approved by CEO ${MarketplaceRepository.CEO_NAME}: ${listing.title}")
                }
                .onFailure {
                    _events.emit(it.message ?: "Approval failed")
                }
        }
    }

    fun rejectListing(listing: FirestoreListing, reason: String) {
        viewModelScope.launch {
            repository.rejectListing(listing.id, reason)
                .onSuccess { _events.emit("Rejected advert: ${listing.title}") }
                .onFailure { _events.emit(it.message ?: "Rejection failed") }
        }
    }

    fun deleteListing(listingId: String) {
        viewModelScope.launch {
            repository.deleteListing(listingId)
                .onSuccess { _events.emit("Listing removed") }
                .onFailure { _events.emit(it.message ?: "Removal failed") }
        }
    }

    fun createEscrowPurchase(
        listing: FirestoreListing,
        buyerName: String,
        buyerEmail: String,
        buyerPhone: String,
        paymentChannel: String,
        buyerPaymentRef: String,
        onCreated: (FirestoreEscrowTransaction) -> Unit
    ) {
        viewModelScope.launch {
            repository.createEscrowPurchase(
                listing = listing,
                buyerName = buyerName,
                buyerEmail = buyerEmail,
                buyerPhone = buyerPhone,
                paymentChannel = paymentChannel,
                buyerPaymentRef = buyerPaymentRef
            ).onSuccess { tx ->
                _events.emit("Escrow order ${tx.referenceCode} created! Protected by Gideon Global.")
                onCreated(tx)
            }.onFailure {
                _events.emit(it.message ?: "Could not place escrow order")
            }
        }
    }

    fun updateEscrowStatus(tx: FirestoreEscrowTransaction, newStatus: EscrowStatus, note: String) {
        viewModelScope.launch {
            repository.updateEscrowStatus(tx.id, newStatus, note)
                .onSuccess { _events.emit("Escrow ${tx.referenceCode} updated: ${newStatus.label}") }
                .onFailure { _events.emit(it.message ?: "Failed to update escrow") }
        }
    }

    private suspend fun handleStreamError(streamName: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $streamName", rootError)
        _events.emit(rootError.localizedMessage ?: "Failed to load $streamName")
    }

    class Factory(
        private val repository: FirestoreMarketplaceRepository,
        private val currentUserId: String,
        private val currentUserEmail: String,
        private val currentUserDisplayName: String,
        private val isCeoAdmin: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                repository = repository,
                currentUserId = currentUserId,
                currentUserEmail = currentUserEmail,
                currentUserDisplayName = currentUserDisplayName,
                isCeoAdmin = isCeoAdmin
            ) as T
        }
    }

    private companion object {
        const val TAG = "GideonMainVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
