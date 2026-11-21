package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.EscrowStatus
import com.example.data.remote.FirestoreEscrowTransaction
import com.example.data.remote.FirestoreListing
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.data.remote.FirestoreUserProfile
import com.example.data.repository.MarketplaceRepository
import com.example.ui.PartnerCommissionSummary
import com.example.ui.components.ApprovalStatusBadge
import com.example.ui.components.CeoSignatureFooter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.EscrowStatusBadge
import com.example.ui.components.ListingPhoto
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.InkDark
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusRejected
import com.example.util.Formatters

@Composable
fun AccountAdminScreen(
    userProfile: FirestoreUserProfile?,
    userEmail: String,
    userDisplayName: String,
    isCeoAdmin: Boolean,
    pendingListings: List<FirestoreListing>,
    allListings: List<FirestoreListing>,
    transactions: List<FirestoreEscrowTransaction>,
    commissionSummary: PartnerCommissionSummary,
    currencyCode: String,
    onUpdateSellerProfile: (fullName: String, phone: String) -> Unit,
    onSignOut: () -> Unit,
    onApproveListing: (FirestoreListing) -> Unit,
    onRejectListing: (FirestoreListing, String) -> Unit,
    onDeleteListing: (String) -> Unit,
    onUpdateEscrowStatus: (FirestoreEscrowTransaction, EscrowStatus, String) -> Unit,
    onGoToSell: () -> Unit,
    onOpenEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var rejectingListing by remember { mutableStateOf<FirestoreListing?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = if (isCeoAdmin) "CEO Approval Studio" else "Account & Partner Seller Desk",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "CEO: ${MarketplaceRepository.CEO_NAME} • ${MarketplaceRepository.OWNER_EMAIL}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            LinkedOwnerIdentityCard(
                onCallPrimary = { Formatters.dialPhone(context, MarketplaceRepository.OWNER_PHONE_PRIMARY) },
                onCallSecondary = { Formatters.dialPhone(context, MarketplaceRepository.OWNER_PHONE_SECONDARY) },
                onEmailOwner = {
                    Formatters.sendEmail(
                        context,
                        MarketplaceRepository.OWNER_EMAIL,
                        "Gideon Global Marketing — Partner / Client Support"
                    )
                },
                onShareApp = {
                    Formatters.shareText(
                        context,
                        "Gideon Global Marketing",
                        "Shop verified land & products or advertise under approval on Gideon Global Marketing!\n" +
                            "MTN MoMo: ${MarketplaceRepository.OWNER_PHONE_PRIMARY} / Orange: ${MarketplaceRepository.OWNER_PHONE_SECONDARY}\n" +
                            "Email: ${MarketplaceRepository.OWNER_EMAIL}"
                    )
                }
            )
        }

        item {
            CurrentSessionAndRegistrationCard(
                userProfile = userProfile,
                userEmail = userEmail,
                userDisplayName = userDisplayName,
                isCeoAdmin = isCeoAdmin,
                onUpdateSellerProfile = onUpdateSellerProfile,
                onSignOut = onSignOut,
                onGoToSell = onGoToSell,
                onOpenEditProfile = onOpenEditProfile
            )
        }

        item {
            MobileMoneyPaymentDeskCard()
        }

        item {
            PartnerCommissionSummaryCard(
                summary = commissionSummary,
                isCeoAdmin = isCeoAdmin,
                currencyCode = currencyCode
            )
        }

        if (isCeoAdmin) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pending Payment Approvals (${pendingListings.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            if (pendingListings.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.CheckCircle,
                        title = "All Seller Adverts Reviewed",
                        subtitle = "New partner shop adverts with MTN MoMo or Orange Money payment references will appear here for your approval."
                    )
                }
            } else {
                items(pendingListings, key = { "pending_${it.id}" }) { listing ->
                    PendingApprovalCard(
                        listing = listing,
                        currencyCode = currencyCode,
                        onApprove = { onApproveListing(listing) },
                        onReject = {
                            rejectingListing = listing
                            rejectReason = ""
                        }
                    )
                }
            }

            item {
                Text(
                    text = "Escrow Transaction Control (${transactions.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.AdminPanelSettings,
                        title = "No Escrow Transactions",
                        subtitle = "Buyer escrow checkout submissions will appear here for payment confirmation and release."
                    )
                }
            } else {
                items(transactions, key = { "admin_tx_${it.id}" }) { tx ->
                    AdminEscrowControlCard(
                        tx = tx,
                        currencyCode = currencyCode,
                        onUpdateStatus = { nextStatus, note ->
                            onUpdateEscrowStatus(tx, nextStatus, note)
                        }
                    )
                }
            }

            item {
                Text(
                    text = "All Marketplace Listings (${allListings.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(allListings, key = { "manage_${it.id}" }) { listing ->
                ManageListingRow(
                    listing = listing,
                    currencyCode = currencyCode,
                    onDelete = { onDeleteListing(listing.id) }
                )
            }
        }

        item {
            MobileDownloadAndStorageNoticeCard()
            Spacer(modifier = Modifier.height(14.dp))
            CeoSignatureFooter()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val targetReject = rejectingListing
    if (targetReject != null) {
        AlertDialog(
            onDismissRequest = { rejectingListing = null },
            title = { Text("Reject Advert") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Provide a reason for '${targetReject.title}' (e.g. unverified payment reference):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Rejection Note") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectListing(targetReject, rejectReason)
                        rejectingListing = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text("Reject Advert")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingListing = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LinkedOwnerIdentityCard(
    onCallPrimary: () -> Unit,
    onCallSecondary: () -> Unit,
    onEmailOwner: () -> Unit,
    onShareApp: () -> Unit
) {
    Surface(
        color = EmeraldDeep,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = R.drawable.img_verified_seal,
                    contentDescription = "Gideon Global Official Crest",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gideon Global Marketing",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "CEO: ${MarketplaceRepository.CEO_NAME}",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldBright
                    )
                    Text(
                        text = "Admin: ${MarketplaceRepository.OWNER_EMAIL}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "MTN MoMo: ${MarketplaceRepository.OWNER_PHONE_PRIMARY} • Orange: 683 480 584",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCallPrimary,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBright),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MTN 676 831 188", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onCallSecondary,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBright),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Orange 683 480 584", maxLines = 1)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onEmailOwner,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldBright,
                        contentColor = InkDark
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Email Admin")
                }
                OutlinedButton(
                    onClick = onShareApp,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share App")
                }
            }
        }
    }
}

@Composable
private fun CurrentSessionAndRegistrationCard(
    userProfile: FirestoreUserProfile?,
    userEmail: String,
    userDisplayName: String,
    isCeoAdmin: Boolean,
    onUpdateSellerProfile: (fullName: String, phone: String) -> Unit,
    onSignOut: () -> Unit,
    onGoToSell: () -> Unit,
    onOpenEditProfile: () -> Unit
) {
    val resolvedName = userProfile?.fullName?.ifBlank { null }
        ?: userDisplayName.ifBlank { userEmail.substringBefore("@").ifBlank { "Partner Seller" } }
    val resolvedEmail = userProfile?.email?.ifBlank { null } ?: userEmail
    var editName by remember(userProfile?.fullName) { mutableStateOf(resolvedName) }
    var editPhone by remember(userProfile?.phone) { mutableStateOf(userProfile?.phone ?: "") }

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
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = resolvedName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$resolvedEmail • ${if (isCeoAdmin) "CEO OWNER ADMIN" else "PARTNER SELLER / CLIENT"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Text(
                text = if (isCeoAdmin) "CEO Official Contact Profile" else "Partner Seller Registration Details",
                style = MaterialTheme.typography.titleSmall
            )
            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("Full Name / Partner Shop Name") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("seller_name_input")
            )
            OutlinedTextField(
                value = editPhone,
                onValueChange = { editPhone = it },
                label = { Text("MTN MoMo / Contact Phone (+237...)") },
                leadingIcon = { Icon(Icons.Filled.Call, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("seller_phone_input")
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { onUpdateSellerProfile(editName, editPhone) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_seller_profile_button")
                ) {
                    Text("Save Profile")
                }
                Button(
                    onClick = onGoToSell,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isCeoAdmin) "Publish Advert" else "Submit Advert")
                }
            }
            OutlinedButton(
                onClick = onOpenEditProfile,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_edit_profile_button")
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile & My Advert Statuses")
            }
        }
    }
}

@Composable
private fun MobileMoneyPaymentDeskCard() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Payment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verified MTN Mobile Money & Orange Desk",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = "• MTN Mobile Money: ${MarketplaceRepository.OWNER_PHONE_PRIMARY}\n" +
                    "• Orange Money: ${MarketplaceRepository.OWNER_PHONE_SECONDARY} (683480584)\n" +
                    "• Approval Email: ${MarketplaceRepository.OWNER_EMAIL}\n" +
                    "All partner seller listing fees and escrow deposits are verified by CEO ${MarketplaceRepository.CEO_NAME} before advert publication or payout release.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun PartnerCommissionSummaryCard(
    summary: PartnerCommissionSummary,
    isCeoAdmin: Boolean,
    currencyCode: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCeoAdmin) "Partner Shop Commission Ledger" else "Partner Shop Commission Terms",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = "Standard partner shop commission: ${FirestoreMarketplaceRepository.PARTNER_SHOP_COMMISSION_PERCENT}% on completed escrow sales (0% on CEO direct listings).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Partner Orders Tracked: ${summary.partnerOrderCount}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "Gross Partner Volume: ${Formatters.formatPrice(summary.grossPartnerSalesXaf, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Platform Commission (${FirestoreMarketplaceRepository.PARTNER_SHOP_COMMISSION_PERCENT}%): ${Formatters.formatPrice(summary.totalCommissionsXaf, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Net Seller Payouts: ${Formatters.formatPrice(summary.totalNetPayoutsXaf, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingApprovalCard(
    listing: FirestoreListing,
    currencyCode: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .testTag("pending_approval_card_${listing.id}")
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
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ApprovalStatusBadge(statusName = listing.approvalStatus)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${Formatters.formatPrice(listing.priceXaf, currencyCode)} • ${listing.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Payment Proof Submitted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Seller: ${listing.sellerName} (${listing.sellerEmail})\n" +
                            "Channel: ${listing.paymentMethod}\n" +
                            "Ref ID: ${listing.paymentReference}\n" +
                            "Payer Phone: ${listing.payerPhone}\n" +
                            "Fee Due: ${Formatters.formatPrice(listing.listingFeeXaf, currencyCode)} • Commission: ${listing.commissionRatePercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reject_advert_btn_${listing.id}")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("approve_advert_btn_${listing.id}")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Verify & Approve")
                }
            }
        }
    }
}

@Composable
private fun AdminEscrowControlCard(
    tx: FirestoreEscrowTransaction,
    currencyCode: String,
    onUpdateStatus: (EscrowStatus, String) -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tx.referenceCode, style = MaterialTheme.typography.titleSmall)
                EscrowStatusBadge(statusName = tx.escrowStatus)
            }
            Text(
                text = "${tx.listingTitle} — ${Formatters.formatPrice(tx.amountXaf, currencyCode)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Commission (${tx.commissionRatePercent}%): ${Formatters.formatPrice(tx.commissionXaf, currencyCode)} • " +
                    "Net Seller Payout: ${Formatters.formatPrice(tx.netSellerPayoutXaf, currencyCode)}\n" +
                    "Buyer: ${tx.buyerName} (${tx.buyerPhone})\n" +
                    "Channel: ${tx.paymentChannel} • Ref: ${tx.buyerPaymentRef}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (EscrowStatus.fromName(tx.escrowStatus)) {
                EscrowStatus.ESCROW_PENDING_VERIFY -> {
                    Button(
                        onClick = {
                            onUpdateStatus(
                                EscrowStatus.ESCROW_SECURED,
                                "Escrow payment verified by Gideon Global Admin."
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Confirm Escrow Payment Received")
                    }
                }
                EscrowStatus.ESCROW_SECURED, EscrowStatus.ITEM_DELIVERED -> {
                    Button(
                        onClick = {
                            onUpdateStatus(
                                EscrowStatus.COMPLETED_RELEASED,
                                "Handover verified. Funds released by Gideon Global Admin."
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Release Escrow Funds & Complete")
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun ManageListingRow(
    listing: FirestoreListing,
    currencyCode: String,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    text = "${Formatters.formatPrice(listing.priceXaf, currencyCode)} • ${listing.sellerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                ApprovalStatusBadge(statusName = listing.approvalStatus)
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete listing",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MobileDownloadAndStorageNoticeCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mobile Installation & Cloud Sync",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = "To install Gideon Global Marketing directly on your Android phone, generate the signed APK/AAB from the AI Studio settings menu and install it on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.local_storage_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
