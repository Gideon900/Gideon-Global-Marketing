package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ListingCategory
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.data.remote.FirestoreUserProfile
import com.example.data.repository.MarketplaceRepository
import com.example.ui.components.CeoSignatureFooter
import com.example.ui.components.ListingPhoto
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldBright
import com.example.ui.theme.InkDark
import com.example.util.Formatters

@Composable
fun CreateAdvertScreen(
    userProfile: FirestoreUserProfile?,
    isCeoAdmin: Boolean,
    currencyCode: String,
    onSubmitListing: (
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
        onDone: () -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf(ListingCategory.LAND) }
    var title by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var specs by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactPhone by remember(userProfile?.phone) { mutableStateOf(userProfile?.phone ?: "") }
    var pickedImageUri by remember { mutableStateOf("") }

    var paymentMethod by remember { mutableStateOf("MTN Mobile Money (+237 676 831 188)") }
    var paymentRef by remember { mutableStateOf("") }
    var payerPhone by remember(userProfile?.phone) { mutableStateOf(userProfile?.phone ?: "") }
    var formError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = Formatters.copyPickedImageToInternalStorage(context, uri)
            pickedImageUri = savedPath ?: uri.toString()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = if (isCeoAdmin) "Publish Official Advert" else "Submit Advert for CEO Approval",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isCeoAdmin) {
                    "Signed in as CEO ${MarketplaceRepository.CEO_NAME} (${MarketplaceRepository.OWNER_EMAIL}). Your land & product listings go live immediately with 0% commission."
                } else {
                    "Pay the advert fee via MTN MoMo or Orange Money, then submit your payment reference for CEO ${MarketplaceRepository.CEO_NAME} approval."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            val previewUri = pickedImageUri.ifBlank {
                if (selectedCategory == ListingCategory.LAND) "drawable://img_land_estate" else "drawable://img_products_showcase"
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.large)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
                    .clickable {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("pick_advert_photo_box")
            ) {
                ListingPhoto(
                    imageUri = previewUri,
                    contentDescription = "Selected listing photo",
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Choose photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (pickedImageUri.isBlank()) "Choose Photo" else "Change Photo",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ListingCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (selectedCategory == ListingCategory.LAND) "Land / Property Title" else "Product / Item Title") },
                placeholder = { Text("e.g. 600 m² Titled Plot in Limbe") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("advert_title_input")
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Price (FCFA)") },
                    placeholder = { Text("e.g. 15000000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("advert_price_input")
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("City / Area") },
                    placeholder = { Text("Douala, Bonaberi") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("advert_location_input")
                )
            }
        }

        item {
            OutlinedTextField(
                value = specs,
                onValueChange = { specs = it },
                label = { Text(if (selectedCategory == ListingCategory.LAND) "Plot Size & Land Title Status" else "Key Specs / Condition") },
                placeholder = {
                    Text(
                        if (selectedCategory == ListingCategory.LAND) {
                            "e.g. 600 m² • Titre Foncier • Roadside"
                        } else {
                            "e.g. Brand New • 12 Months Warranty"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("advert_specs_input")
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Full Description") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("advert_description_input")
            )
        }

        item {
            OutlinedTextField(
                value = contactPhone,
                onValueChange = { contactPhone = it },
                label = { Text("Contact Phone") },
                leadingIcon = { Icon(Icons.Filled.Call, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!isCeoAdmin) {
            item {
                Surface(
                    color = EmeraldDeep,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Payment,
                                contentDescription = null,
                                tint = GoldBright
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Advert Fee & Partner Commission Terms",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Listing Fee for ${selectedCategory.label}: ${Formatters.formatPrice(selectedCategory.feeXaf, currencyCode)}\n" +
                                "Partner Shop Sales Commission: ${FirestoreMarketplaceRepository.PARTNER_SHOP_COMMISSION_PERCENT}% on completed escrow sales",
                            style = MaterialTheme.typography.titleSmall,
                            color = GoldBright
                        )
                        Text(
                            text = "Send listing fee to Gideon Global Marketing:\n• MTN MoMo: ${MarketplaceRepository.OWNER_PHONE_PRIMARY}\n• Orange Money: ${MarketplaceRepository.OWNER_PHONE_SECONDARY}\n• CEO Approval Desk: ${MarketplaceRepository.OWNER_EMAIL}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { Formatters.dialPhone(context, MarketplaceRepository.OWNER_PHONE_PRIMARY) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBright)
                            ) {
                                Text("Call MTN")
                            }
                            OutlinedButton(
                                onClick = { Formatters.dialPhone(context, MarketplaceRepository.OWNER_PHONE_SECONDARY) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBright)
                            ) {
                                Text("Call Orange")
                            }
                        }
                    }
                }
            }

            item {
                val mtn = "MTN Mobile Money (+237 676 831 188)"
                val orange = "Orange Money (+237 683 480 584)"
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("MTN MoMo" to mtn, "Orange Money" to orange).forEach { (shortLabel, full) ->
                        val selected = paymentMethod == full
                        Surface(
                            color = if (selected) GoldBright else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { paymentMethod = full }
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
            }

            item {
                OutlinedTextField(
                    value = paymentRef,
                    onValueChange = { paymentRef = it },
                    label = { Text("Payment Transaction Reference ID *") },
                    placeholder = { Text("e.g. MP261119.1245.A88921") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advert_payment_ref_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = payerPhone,
                    onValueChange = { payerPhone = it },
                    label = { Text("Payer Mobile Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CEO Direct Publishing: 0 FCFA listing fee • 0% commission • Publishes live immediately with Gideon Official badge.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (formError != null) {
            item {
                Text(
                    text = formError!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Button(
                onClick = {
                    val priceVal = priceText.toLongOrNull() ?: 0L
                    when {
                        title.trim().length < 4 -> formError = "Please enter a clear title (min 4 characters)"
                        priceVal <= 0L -> formError = "Please enter a valid price in FCFA"
                        location.trim().length < 2 -> formError = "Please enter the city or location"
                        description.trim().length < 10 -> formError = "Please add a helpful description"
                        !isCeoAdmin && paymentRef.trim().length < 4 -> {
                            formError = "Enter your MTN MoMo / Orange Money transaction reference ID so CEO ${MarketplaceRepository.CEO_NAME} can approve your advert"
                        }
                        else -> {
                            formError = null
                            onSubmitListing(
                                title,
                                description,
                                selectedCategory,
                                priceVal,
                                location,
                                specs,
                                pickedImageUri,
                                contactPhone,
                                paymentMethod,
                                paymentRef,
                                payerPhone
                            ) {
                                title = ""
                                priceText = ""
                                location = ""
                                specs = ""
                                description = ""
                                pickedImageUri = ""
                                paymentRef = ""
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_advert_button")
            ) {
                Icon(Icons.Filled.Publish, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCeoAdmin) "Publish Official Advert" else "Submit Payment & Advert for CEO Approval",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            CeoSignatureFooter()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
