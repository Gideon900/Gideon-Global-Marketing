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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ApprovalStatus
import com.example.data.local.ListingCategory
import com.example.data.remote.FirestoreListing
import com.example.data.repository.MarketplaceRepository
import com.example.ui.components.ApprovalStatusBadge
import com.example.ui.components.CeoSignatureFooter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ListingPhoto
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.InkDark
import com.example.ui.theme.StatusApproved
import com.example.ui.theme.StatusRejected
import com.example.util.Formatters

@Composable
fun AdminApprovalsScreen(
    isCeoAdmin: Boolean,
    pendingListings: List<FirestoreListing>,
    allListings: List<FirestoreListing>,
    currencyCode: String,
    onApproveListing: (FirestoreListing) -> Unit,
    onRejectListing: (FirestoreListing, String) -> Unit,
    onListingClick: (FirestoreListing) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isCeoAdmin) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            EmptyStateCard(
                icon = Icons.Filled.Lock,
                title = "CEO Authorization Required",
                subtitle = "Only CEO ${MarketplaceRepository.CEO_NAME} (${MarketplaceRepository.OWNER_EMAIL}) can access the Admin Pending Adverts Queue.",
                modifier = Modifier.testTag("admin_access_denied_card")
            )
            CeoSignatureFooter()
        }
        return
    }

    val reviewedListings = remember(allListings) {
        allListings.filter { it.approvalStatus != ApprovalStatus.PENDING_APPROVAL.name }
    }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0 = Pending, 1 = Reviewed
    var noteRejectTarget by remember { mutableStateOf<FirestoreListing?>(null) }
    var customNote by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Surface(
                color = EmeraldDeep,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = GoldBright
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CEO Admin Approvals",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        Surface(
                            color = GoldBright,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${pendingListings.size} Pending",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = InkDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "CEO: ${MarketplaceRepository.CEO_NAME} • ${MarketplaceRepository.OWNER_EMAIL}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Inspect seller MTN MoMo / Orange Money payment proof and tap Approve or Reject for immediate status updates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "Pending (${pendingListings.size})",
                    "Reviewed (${reviewedListings.size})"
                ).forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedFilter == index,
                        onClick = { selectedFilter = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        modifier = Modifier.testTag("admin_approvals_filter_$index")
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }
        }

        if (selectedFilter == 0) {
            if (pendingListings.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.CheckCircle,
                        title = "No Pending Seller Adverts",
                        subtitle = "All submitted product and land adverts have been reviewed. New submissions with payment proofs will appear here."
                    )
                }
            } else {
                items(pendingListings, key = { "queue_${it.id}" }) { listing ->
                    OneTapAdvertModerationCard(
                        listing = listing,
                        currencyCode = currencyCode,
                        isPending = true,
                        onClick = { onListingClick(listing) },
                        onOneTapApprove = { onApproveListing(listing) },
                        onOneTapReject = {
                            onRejectListing(
                                listing,
                                "Payment unverified or advert declined by CEO ${MarketplaceRepository.CEO_NAME}."
                            )
                        },
                        onRejectWithNote = {
                            noteRejectTarget = listing
                            customNote = ""
                        }
                    )
                }
            }
        } else {
            if (reviewedListings.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.AdminPanelSettings,
                        title = "No Reviewed Adverts Yet",
                        subtitle = "Approved and rejected marketplace listings will appear here for audit or status updates."
                    )
                }
            } else {
                items(reviewedListings, key = { "reviewed_${it.id}" }) { listing ->
                    OneTapAdvertModerationCard(
                        listing = listing,
                        currencyCode = currencyCode,
                        isPending = false,
                        onClick = { onListingClick(listing) },
                        onOneTapApprove = { onApproveListing(listing) },
                        onOneTapReject = {
                            onRejectListing(
                                listing,
                                "Removed from live marketplace by CEO ${MarketplaceRepository.CEO_NAME}."
                            )
                        },
                        onRejectWithNote = {
                            noteRejectTarget = listing
                            customNote = ""
                        }
                    )
                }
            }
        }

        item {
            CeoSignatureFooter()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    val target = noteRejectTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { noteRejectTarget = null },
            title = { Text("Reject with Custom Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter an explanation for rejecting '${target.title}':",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = customNote,
                        onValueChange = { customNote = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRejectListing(target, customNote)
                        noteRejectTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected)
                ) {
                    Text("Reject Advert")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteRejectTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun OneTapAdvertModerationCard(
    listing: FirestoreListing,
    currencyCode: String,
    isPending: Boolean,
    onClick: () -> Unit,
    onOneTapApprove: () -> Unit,
    onOneTapReject: () -> Unit,
    onRejectWithNote: () -> Unit
) {
    val category = ListingCategory.fromName(listing.category)
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .testTag("admin_approval_card_${listing.id}")
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ApprovalStatusBadge(statusName = listing.approvalStatus)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${Formatters.formatPrice(listing.priceXaf, currencyCode)} • ${listing.location}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (listing.description.isNotBlank()) {
                Text(
                    text = listing.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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
                        text = if (listing.isOwnerListing) "CEO Direct Listing" else "Seller & Payment Verification",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Seller: ${listing.sellerName} (${listing.sellerEmail})\n" +
                            "Phone: ${listing.sellerPhone.ifBlank { listing.payerPhone }}\n" +
                            "Payment: ${listing.paymentMethod} • Ref: ${listing.paymentReference}\n" +
                            "Advert Fee: ${Formatters.formatPrice(listing.listingFeeXaf, currencyCode)} • Commission: ${listing.commissionRatePercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isPending || listing.approvalStatus == ApprovalStatus.APPROVED.name) {
                    OutlinedButton(
                        onClick = onOneTapReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejected),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("one_tap_reject_btn_${listing.id}")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject", maxLines = 1)
                    }
                }
                if (isPending || listing.approvalStatus == ApprovalStatus.REJECTED.name) {
                    Button(
                        onClick = onOneTapApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusApproved),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("one_tap_approve_btn_${listing.id}")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve", maxLines = 1)
                    }
                }
            }

            if (isPending) {
                TextButton(
                    onClick = onRejectWithNote,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Reject with custom note…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
