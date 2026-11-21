package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.ListingCategory
import com.example.data.remote.FirestoreListing
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.data.remote.FirestoreUserProfile
import com.example.data.repository.MarketplaceRepository
import com.example.ui.components.ApprovalStatusBadge
import com.example.ui.components.CeoSignatureFooter
import com.example.ui.components.ListingPhoto
import com.example.ui.components.VerifiedSealBadge
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.InkDark
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    listing: FirestoreListing,
    currencyCode: String,
    userProfile: FirestoreUserProfile?,
    onBack: () -> Unit,
    onCreateEscrowOrder: (
        buyerName: String,
        buyerEmail: String,
        buyerPhone: String,
        paymentChannel: String,
        buyerPaymentRef: String,
        onDone: () -> Unit
    ) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val category = ListingCategory.fromName(listing.category)
    val commission = remember(listing) {
        FirestoreMarketplaceRepository.calculateCommission(
            grossAmountXaf = listing.priceXaf,
            isOwnerListing = listing.isOwnerListing,
            ratePercent = listing.commissionRatePercent
        )
    }
    var showEscrowSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            Formatters.dialPhone(
                                context,
                                listing.sellerPhone.ifBlank { MarketplaceRepository.OWNER_PHONE_PRIMARY }
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("detail_call_button")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = "Call seller")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call")
                    }
                    Button(
                        onClick = { showEscrowSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("detail_buy_escrow_button")
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buy via Secure Escrow")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
            ) {
                ListingPhoto(
                    imageUri = listing.imageUri,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("detail_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Marketplace",
                                tint = Color.White
                            )
                        }
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape
                    ) {
                        IconButton(
                            onClick = {
                                val priceStr = Formatters.formatPrice(listing.priceXaf, currencyCode)
                                Formatters.shareText(
                                    context,
                                    listing.title,
                                    "${listing.title} — $priceStr (${listing.location})\nApproved on Gideon Global Marketing (CEO: ${MarketplaceRepository.CEO_NAME}).\nContact: ${MarketplaceRepository.OWNER_PHONE_PRIMARY} / ${MarketplaceRepository.OWNER_EMAIL}"
                                )
                            },
                            modifier = Modifier.testTag("detail_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share listing",
                                tint = Color.White
                            )
                        }
                    }
                }

                VerifiedSealBadge(
                    isOwnerListing = listing.isOwnerListing,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    ApprovalStatusBadge(statusName = listing.approvalStatus)
                }

                Text(
                    text = Formatters.formatPrice(listing.priceXaf, currencyCode),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = listing.location,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (listing.specs.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Key Specifications",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listing.specs,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = listing.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = R.drawable.img_verified_seal,
                                contentDescription = "Verified by Gideon Global",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = listing.sellerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (listing.isOwnerListing) {
                                        "Official Gideon Global Marketing Listing (0% Commission)"
                                    } else {
                                        "Approved Partner Shop • ${commission.commissionRatePercent}% Platform Commission"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    Formatters.dialPhone(context, MarketplaceRepository.OWNER_PHONE_PRIMARY)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Admin Desk", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = {
                                    Formatters.sendEmail(
                                        context,
                                        MarketplaceRepository.OWNER_EMAIL,
                                        "Inquiry: ${listing.title}",
                                        "Hello CEO ${MarketplaceRepository.CEO_NAME} (Gideon Global Marketing),\n\nI am interested in '${listing.title}' (${listing.location}). Please share next steps for secure inspection and escrow purchase."
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Email CEO", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Escrow Protection",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gideon Global Escrow Guarantee: Funds are held safely via MTN MoMo (${MarketplaceRepository.OWNER_PHONE_PRIMARY}) or Orange Money (${MarketplaceRepository.OWNER_PHONE_SECONDARY}) and only released after handover.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                CeoSignatureFooter()
            }
        }
    }

    if (showEscrowSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var buyerName by remember { mutableStateOf(userProfile?.fullName ?: "") }
        var buyerEmail by remember { mutableStateOf(userProfile?.email ?: "") }
        var buyerPhone by remember { mutableStateOf(userProfile?.phone ?: "") }
        var selectedChannel by remember { mutableStateOf("MTN Mobile Money (+237 676 831 188)") }
        var paymentRef by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { showEscrowSheet = false },
            sheetState = sheetState
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Secure Escrow Checkout",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${listing.title} • ${Formatters.formatPrice(listing.priceXaf, currencyCode)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Escrow & Partner Shop Commission Breakdown",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total Buyer Escrow: ${Formatters.formatPrice(commission.grossAmountXaf, currencyCode)}\n" +
                                "Gideon Global Commission (${commission.commissionRatePercent}%): ${Formatters.formatPrice(commission.commissionXaf, currencyCode)}\n" +
                                "Net Seller Payout: ${Formatters.formatPrice(commission.netSellerPayoutXaf, currencyCode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = EmeraldDeep,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Official Gideon Global Escrow Accounts",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldBright
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MTN Mobile Money: ${MarketplaceRepository.OWNER_PHONE_PRIMARY}\nOrange Money: ${MarketplaceRepository.OWNER_PHONE_SECONDARY}\nCEO Approval Desk: ${MarketplaceRepository.OWNER_EMAIL}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val mtn = "MTN Mobile Money (+237 676 831 188)"
                    val orange = "Orange Money (+237 683 480 584)"
                    listOf("MTN MoMo" to mtn, "Orange Money" to orange).forEach { (shortLabel, full) ->
                        val selected = selectedChannel == full
                        Surface(
                            color = if (selected) GoldBright else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            onClick = { selectedChannel = full },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = shortLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) InkDark else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = buyerName,
                    onValueChange = { buyerName = it },
                    label = { Text("Your Full Name") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = buyerPhone,
                    onValueChange = { buyerPhone = it },
                    label = { Text("Your Phone (+237...)") },
                    leadingIcon = { Icon(Icons.Filled.Call, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = buyerEmail,
                    onValueChange = { buyerEmail = it },
                    label = { Text("Your Email") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paymentRef,
                    onValueChange = { paymentRef = it },
                    label = { Text("MoMo / Orange Transaction ID") },
                    placeholder = { Text("e.g. MP261119.1422.A10492") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("escrow_payment_ref_input")
                )

                Button(
                    onClick = {
                        onCreateEscrowOrder(
                            buyerName,
                            buyerEmail,
                            buyerPhone,
                            selectedChannel,
                            paymentRef
                        ) {
                            showEscrowSheet = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_escrow_order_btn")
                ) {
                    Text("Submit Escrow Transaction")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
