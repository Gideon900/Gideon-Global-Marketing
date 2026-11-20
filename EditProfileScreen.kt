package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ApprovalStatus
import com.example.data.local.ListingCategory
import com.example.data.local.PaymentStatus
import com.example.data.remote.FirestoreListing
import com.example.data.remote.FirestoreUserProfile
import com.example.data.repository.MarketplaceRepository
import com.example.ui.components.ApprovalStatusBadge
import com.example.ui.components.CeoSignatureFooter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ListingPhoto
import com.example.ui.components.StatusPill
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.InkDark
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusRejected
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    userProfile: FirestoreUserProfile?,
    userEmail: String,
    userDisplayName: String,
    isCeoAdmin: Boolean,
    myListings: List<FirestoreListing>,
    currencyCode: String,
    onSaveProfile: (fullName: String, phone: String) -> Unit,
    onListingClick: (FirestoreListing) -> Unit,
    onCreateAdvert: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val resolvedName = remember(userProfile?.fullName, userDisplayName, userEmail) {
        userProfile?.fullName?.ifBlank { null }
            ?: userDisplayName.ifBlank { userEmail.substringBefore("@").ifBlank { "Partner Seller" } }
    }
    val resolvedEmail = userProfile?.email?.ifBlank { null } ?: userEmail
    val resolvedPhone = userProfile?.phone ?: ""

    var fullName by rememberSaveable(userProfile?.fullName) { mutableStateOf(resolvedName) }
    var phone by rememberSaveable(userProfile?.phone) { mutableStateOf(resolvedPhone) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var statusFilter by rememberSaveable { mutableStateOf<ApprovalStatus?>(null) }

    val pendingCount = remember(myListings) {
        myListings.count { it.approvalStatus == ApprovalStatus.PENDING_APPROVAL.name }
    }
    val approvedCount = remember(myListings) {
        myListings.count { it.approvalStatus == ApprovalStatus.APPROVED.name }
    }
    val rejectedCount = remember(myListings) {
        myListings.count { it.approvalStatus == ApprovalStatus.REJECTED.name }
    }
    val filteredListings = remember(myListings, statusFilter) {
        val filter = statusFilter
        if (filter == null) myListings else myListings.filter { it.approvalStatus == filter.name }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Edit Profile & Adverts",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isCeoAdmin) "CEO Official Contact & Listings" else "Contact Info & Submitted Statuses",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("edit_profile_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Account"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 920.dp)
                    .testTag("edit_profile_list")
            ) {
                item {
                    ProfileIdentityHeaderCard(
                        displayName = resolvedName,
                        email = resolvedEmail,
                        phone = resolvedPhone,
                        isCeoAdmin = isCeoAdmin
                    )
                }

                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Update Contact Information",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Your shop name and Mobile Money number appear on advert submissions and buyer escrow payouts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = {
                                    fullName = it
                                    if (validationError != null) validationError = null
                                },
                                label = { Text("Full Name / Partner Shop Name") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                supportingText = { Text("${fullName.trim().length}/80 characters") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_profile_name_input")
                            )

                            OutlinedTextField(
                                value = phone,
                                onValueChange = {
                                    phone = it
                                    if (validationError != null) validationError = null
                                },
                                label = { Text("MTN MoMo / Contact Phone (+237...)") },
                                leadingIcon = { Icon(Icons.Filled.Call, contentDescription = null) },
                                supportingText = { Text("Used for buyer calls, MoMo payout verification, and admin checks") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_profile_phone_input")
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "Verified Google Account Email",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = resolvedEmail.ifBlank { "Signed-in Google Account" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            validationError?.let { errorText ->
                                Surface(
                                    color = StatusRejected.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, StatusRejected.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .testTag("edit_profile_validation_error")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = null,
                                            tint = StatusRejected
                                        )
                                        Text(
                                            text = errorText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = StatusRejected,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val trimmedName = fullName.trim()
                                    val trimmedPhone = phone.trim()
                                    when {
                                        trimmedName.length < 2 || trimmedName.length > 80 -> {
                                            validationError = "Please enter a full name or shop name between 2 and 80 characters."
                                        }
                                        trimmedPhone.length > 40 -> {
                                            validationError = "Phone number must be 40 characters or fewer."
                                        }
                                        else -> {
                                            validationError = null
                                            onSaveProfile(trimmedName, trimmedPhone)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("edit_profile_save_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Contact Information")
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "My Submitted Adverts (${myListings.size})",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Reviewed by CEO ${MarketplaceRepository.CEO_NAME} (${MarketplaceRepository.OWNER_EMAIL})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = onCreateAdvert,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("edit_profile_create_advert_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddCircleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Advert", maxLines = 1)
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_advert_summary_row")
                        ) {
                            FilterChip(
                                selected = statusFilter == null,
                                onClick = { statusFilter = null },
                                label = { Text("All (${myListings.size})") },
                                modifier = Modifier.testTag("profile_filter_all")
                            )
                            FilterChip(
                                selected = statusFilter == ApprovalStatus.PENDING_APPROVAL,
                                onClick = {
                                    statusFilter = if (statusFilter == ApprovalStatus.PENDING_APPROVAL) {
                                        null
                                    } else {
                                        ApprovalStatus.PENDING_APPROVAL
                                    }
                                },
                                label = { Text("Pending ($pendingCount)") },
                                modifier = Modifier.testTag("profile_filter_pending")
                            )
                            FilterChip(
                                selected = statusFilter == ApprovalStatus.APPROVED,
                                onClick = {
                                    statusFilter = if (statusFilter == ApprovalStatus.APPROVED) {
                                        null
                                    } else {
                                        ApprovalStatus.APPROVED
                                    }
                                },
                                label = { Text("Approved ($approvedCount)") },
                                modifier = Modifier.testTag("profile_filter_approved")
                            )
                            FilterChip(
                                selected = statusFilter == ApprovalStatus.REJECTED,
                                onClick = {
                                    statusFilter = if (statusFilter == ApprovalStatus.REJECTED) {
                                        null
                                    } else {
                                        ApprovalStatus.REJECTED
                                    }
                                },
                                label = { Text("Rejected ($rejectedCount)") },
                                modifier = Modifier.testTag("profile_filter_rejected")
                            )
                        }
                    }
                }

                if (myListings.isEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            EmptyStateCard(
                                icon = Icons.Filled.Campaign,
                                title = "No Adverts Submitted Yet",
                                subtitle = "Submit a product or land advert from the Sell tab to track approval and payment verification statuses here.",
                                modifier = Modifier.testTag("profile_no_adverts_empty")
                            )
                            Button(
                                onClick = onCreateAdvert,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Submit Your First Advert")
                            }
                        }
                    }
                } else if (filteredListings.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Filled.CheckCircle,
                            title = "No Adverts Match This Status",
                            subtitle = "Tap 'All' above to view every advert you have submitted."
                        )
                    }
                } else {
                    items(filteredListings, key = { "profile_advert_${it.id}" }) { listing ->
                        ProfileAdvertStatusCard(
                            listing = listing,
                            currencyCode = currencyCode,
                            onClick = { onListingClick(listing) }
                        )
                    }
                }

                item {
                    CeoSignatureFooter()
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileIdentityHeaderCard(
    displayName: String,
    email: String,
    phone: String,
    isCeoAdmin: Boolean
) {
    Surface(
        color = EmeraldDeep,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(GoldBright)
            ) {
                Icon(
                    imageVector = if (isCeoAdmin) Icons.Filled.Verified else Icons.Filled.Person,
                    contentDescription = displayName,
                    tint = InkDark,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = GoldBright.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(1.dp, GoldBright.copy(alpha = 0.6f), RoundedCornerShape(50))
                ) {
                    Text(
                        text = if (isCeoAdmin) "CEO OWNER ADMIN" else "PARTNER SELLER / CLIENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldBright,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(email)
                        if (phone.isNotBlank()) {
                            append(" • ")
                            append(phone)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfileAdvertStatusCard(
    listing: FirestoreListing,
    currencyCode: String,
    onClick: () -> Unit
) {
    val category = ListingCategory.fromName(listing.category)
    val paymentStatus = PaymentStatus.fromName(listing.paymentStatus)
    val paymentColor = when (paymentStatus) {
        PaymentStatus.NOT_REQUIRED, PaymentStatus.PAYMENT_VERIFIED -> StatusApproved
        PaymentStatus.PAYMENT_SUBMITTED -> StatusPending
        PaymentStatus.REFUNDED -> StatusRejected
    }

    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .testTag("profile_advert_card_${listing.id}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListingPhoto(
                    imageUri = listing.imageUri,
                    contentDescription = listing.title,
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ApprovalStatusBadge(statusName = listing.approvalStatus)
                        StatusPill(
                            label = paymentStatus.label,
                            color = paymentColor
                        )
                    }
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${Formatters.formatPrice(listing.priceXaf, currencyCode)} • ${category.label}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (listing.location.isNotBlank()) {
                        Text(
                            text = listing.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (listing.isOwnerListing) {
                        "CEO Direct Listing • 0% partner commission"
                    } else {
                        "Payment: ${listing.paymentMethod.ifBlank { "MTN / Orange Money" }} • " +
                            "Ref: ${listing.paymentReference.ifBlank { "Pending" }} • " +
                            "Fee: ${Formatters.formatPrice(listing.listingFeeXaf, currencyCode)} • " +
                            "${listing.commissionRatePercent}% partner commission"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }

            if (listing.adminNotes.isNotBlank()) {
                val noteColor = if (listing.approvalStatus == ApprovalStatus.REJECTED.name) {
                    StatusRejected
                } else {
                    MaterialTheme.colorScheme.secondary
                }
                Surface(
                    color = noteColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, noteColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = "CEO Admin Note: ${listing.adminNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = noteColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
