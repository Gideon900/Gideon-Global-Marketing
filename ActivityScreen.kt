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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.EscrowStatus
import com.example.data.remote.FirestoreEscrowTransaction
import com.example.data.remote.FirestoreListing
import com.example.ui.components.ApprovalStatusBadge
import com.example.ui.components.CeoSignatureFooter
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.EscrowStatusBadge
import com.example.ui.components.ListingPhoto
import com.example.util.Formatters

@Composable
fun ActivityScreen(
    currentUserId: String,
    myListings: List<FirestoreListing>,
    transactions: List<FirestoreEscrowTransaction>,
    currencyCode: String,
    onConfirmDelivery: (FirestoreEscrowTransaction) -> Unit,
    onListingClick: (FirestoreListing) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Activity & Escrow",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Track your submitted adverts, partner sales commissions, and protected escrow orders",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf(
                "Escrow Orders (${transactions.size})",
                "My Adverts (${myListings.size})"
            ).forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                    modifier = Modifier.testTag("activity_tab_$index")
                ) {
                    Text(label, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            if (transactions.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No Escrow Orders Yet",
                    subtitle = "Tap 'Buy via Secure Escrow' on any approved listing to initiate a protected transaction."
                )
                Spacer(modifier = Modifier.height(16.dp))
                CeoSignatureFooter()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        EscrowOrderCard(
                            tx = tx,
                            currentUserId = currentUserId,
                            currencyCode = currencyCode,
                            onConfirmDelivery = { onConfirmDelivery(tx) }
                        )
                    }
                    item {
                        CeoSignatureFooter()
                    }
                }
            }
        } else {
            if (myListings.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Filled.Campaign,
                    title = "No Submitted Adverts",
                    subtitle = "Use the Sell tab to advertise your land or products on Gideon Global."
                )
                Spacer(modifier = Modifier.height(16.dp))
                CeoSignatureFooter()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myListings, key = { it.id }) { listing ->
                        MyAdvertStatusCard(
                            listing = listing,
                            currencyCode = currencyCode,
                            onClick = { onListingClick(listing) }
                        )
                    }
                    item {
                        CeoSignatureFooter()
                    }
                }
            }
        }
    }
}

@Composable
private fun EscrowOrderCard(
    tx: FirestoreEscrowTransaction,
    currentUserId: String,
    currencyCode: String,
    onConfirmDelivery: () -> Unit
) {
    val isBuyer = tx.buyerUid == currentUserId
    val isSeller = tx.sellerUid == currentUserId

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tx.referenceCode,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                EscrowStatusBadge(statusName = tx.escrowStatus)
            }
            Text(
                text = tx.listingTitle,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = Formatters.formatPrice(tx.amountXaf, currencyCode),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Gross: ${Formatters.formatPrice(tx.amountXaf, currencyCode)} • " +
                        "Commission (${tx.commissionRatePercent}%): ${Formatters.formatPrice(tx.commissionXaf, currencyCode)} • " +
                        "Seller Net: ${Formatters.formatPrice(tx.netSellerPayoutXaf, currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                text = buildString {
                    append("Role: ${if (isBuyer) "Buyer" else if (isSeller) "Partner Seller" else "Admin View"}\n")
                    append("Channel: ${tx.paymentChannel}\n")
                    append("Payment Ref: ${tx.buyerPaymentRef}\n")
                    append("Buyer: ${tx.buyerName} (${tx.buyerPhone})\n")
                    append("Seller: ${tx.sellerName}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (tx.deliveryNote.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tx.deliveryNote,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            if (tx.escrowStatus == EscrowStatus.ESCROW_SECURED.name && isBuyer) {
                Button(
                    onClick = onConfirmDelivery,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm Item / Land Handover Received")
                }
            }
        }
    }
}

@Composable
private fun MyAdvertStatusCard(
    listing: FirestoreListing,
    currencyCode: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            ListingPhoto(
                imageUri = listing.imageUri,
                contentDescription = listing.title,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                ApprovalStatusBadge(statusName = listing.approvalStatus)
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2
                )
                Text(
                    text = Formatters.formatPrice(listing.priceXaf, currencyCode),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!listing.isOwnerListing) {
                    Text(
                        text = "Fee Ref: ${listing.paymentReference} • ${listing.commissionRatePercent}% partner commission",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (listing.adminNotes.isNotBlank()) {
                    Text(
                        text = "Admin: ${listing.adminNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
