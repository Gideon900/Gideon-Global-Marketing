package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.local.EscrowStatus
import com.example.data.local.SessionPreferences
import com.example.data.remote.FirestoreMarketplaceRepository
import com.example.data.repository.MarketplaceRepository
import com.example.ui.auth.GideonAuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutGoogleAndFirebase
import com.example.ui.screens.AccountAdminScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AdminApprovalsScreen
import com.example.ui.screens.CreateAdvertScreen
import com.example.ui.screens.EditProfileScreen
import com.example.ui.screens.ListingDetailScreen
import com.example.ui.screens.MarketplaceScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class GideonTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    EXPLORE("Explore", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    SELL("Sell", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
    ACTIVITY("Activity", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    APPROVALS("Approvals", Icons.Filled.VerifiedUser, Icons.Outlined.VerifiedUser),
    ACCOUNT("Account", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)
}

@Composable
fun AppNavigation() {
    val firebaseUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val activeUser = firebaseUser
    if (activeUser == null) {
        GideonAuthScreen()
    } else {
        key(activeUser.uid) {
            val context = LocalContext.current
            val app = context.applicationContext
            val isCeoAdmin = activeUser.email.equals(MarketplaceRepository.OWNER_EMAIL, ignoreCase = true) &&
                activeUser.isEmailVerified
            val viewModel: MainViewModel = viewModel(
                key = activeUser.uid,
                factory = MainViewModel.Factory(
                    repository = FirestoreMarketplaceRepository(
                        db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id)),
                        auth = Firebase.auth,
                        sessionPreferences = SessionPreferences(app)
                    ),
                    currentUserId = activeUser.uid,
                    currentUserEmail = activeUser.email ?: "",
                    currentUserDisplayName = activeUser.displayName ?: "",
                    isCeoAdmin = isCeoAdmin
                )
            )
            GideonApp(
                viewModel = viewModel,
                currentUserId = activeUser.uid,
                currentUserEmail = activeUser.email ?: "",
                currentUserDisplayName = activeUser.displayName ?: "",
                isCeoAdmin = isCeoAdmin
            )
        }
    }
}

@Composable
fun GideonApp(
    viewModel: MainViewModel,
    currentUserId: String,
    currentUserEmail: String,
    currentUserDisplayName: String,
    isCeoAdmin: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val approvedListings by viewModel.filteredApprovedListings.collectAsStateWithLifecycle()
    val pendingListings by viewModel.pendingApprovalListings.collectAsStateWithLifecycle()
    val allListings by viewModel.allAccessibleListings.collectAsStateWithLifecycle()
    val mySubmittedListings by viewModel.mySubmittedListings.collectAsStateWithLifecycle()
    val transactions by viewModel.visibleTransactions.collectAsStateWithLifecycle()
    val commissionSummary by viewModel.commissionSummary.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val currencyCode by viewModel.currencyCode.collectAsStateWithLifecycle()

    val visibleTabs = remember(isCeoAdmin) {
        if (isCeoAdmin) {
            GideonTab.entries
        } else {
            GideonTab.entries.filter { it != GideonTab.APPROVALS }
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(GideonTab.EXPLORE) }
    var selectedListingId by rememberSaveable { mutableStateOf<String?>(null) }
    var showEditProfile by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isCeoAdmin, currentTab) {
        if (!isCeoAdmin && currentTab == GideonTab.APPROVALS) {
            currentTab = GideonTab.EXPLORE
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val selectedListing = selectedListingId?.let { id ->
        allListings.firstOrNull { it.id == id }
    }

    if (selectedListing != null) {
        ListingDetailScreen(
            listing = selectedListing,
            currencyCode = currencyCode,
            userProfile = userProfile,
            onBack = { selectedListingId = null },
            onCreateEscrowOrder = { buyerName, buyerEmail, buyerPhone, channel, ref, onDone ->
                viewModel.createEscrowPurchase(
                    listing = selectedListing,
                    buyerName = buyerName,
                    buyerEmail = buyerEmail,
                    buyerPhone = buyerPhone,
                    paymentChannel = channel,
                    buyerPaymentRef = ref
                ) {
                    onDone()
                    selectedListingId = null
                    currentTab = GideonTab.ACTIVITY
                }
            }
        )
        return
    }

    if (showEditProfile) {
        EditProfileScreen(
            userProfile = userProfile,
            userEmail = currentUserEmail,
            userDisplayName = currentUserDisplayName,
            isCeoAdmin = isCeoAdmin,
            myListings = mySubmittedListings,
            currencyCode = currencyCode,
            onSaveProfile = viewModel::updateSellerProfile,
            onListingClick = { selectedListingId = it.id },
            onCreateAdvert = {
                showEditProfile = false
                currentTab = GideonTab.SELL
            },
            onBack = { showEditProfile = false }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Gideon Global Marketing")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Discover Gideon Global Marketing — browse products and property listings, or advertise your business.\n\nGet the app project and latest download here: https://github.com/Gideon900/Gideon-Global-Marketing"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Gideon Global Marketing"))
                    }
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share app")
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        visibleTabs.forEach { tab ->
                            val selected = currentTab == tab
                            val showAdminBadge = tab == GideonTab.APPROVALS && isCeoAdmin && pendingListings.isNotEmpty()
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (showAdminBadge) {
                                                Badge { Text("${pendingListings.size}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(0.1f))
                        visibleTabs.forEach { tab ->
                            val selected = currentTab == tab
                            val showAdminBadge = tab == GideonTab.APPROVALS && isCeoAdmin && pendingListings.isNotEmpty()
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (showAdminBadge) {
                                                Badge { Text("${pendingListings.size}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 920.dp)

                    when (currentTab) {
                        GideonTab.EXPLORE -> {
                            MarketplaceScreen(
                                listings = approvedListings,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                currencyCode = currencyCode,
                                onSearchChange = viewModel::updateSearchQuery,
                                onCategorySelect = viewModel::selectCategory,
                                onToggleCurrency = viewModel::toggleCurrency,
                                onListingClick = { selectedListingId = it.id },
                                onSellClick = { currentTab = GideonTab.SELL },
                                modifier = contentModifier
                            )
                        }
                        GideonTab.SELL -> {
                            CreateAdvertScreen(
                                userProfile = userProfile,
                                isCeoAdmin = isCeoAdmin,
                                currencyCode = currencyCode,
                                onSubmitListing = { title, desc, cat, price, loc, specs, img, phone, method, ref, payer, onDone ->
                                    viewModel.submitListing(
                                        title = title,
                                        description = desc,
                                        category = cat,
                                        priceXaf = price,
                                        location = loc,
                                        specs = specs,
                                        imageUri = img,
                                        contactPhone = phone,
                                        paymentMethod = method,
                                        paymentReference = ref,
                                        payerPhone = payer
                                    ) {
                                        onDone()
                                        currentTab = if (isCeoAdmin) GideonTab.EXPLORE else GideonTab.ACTIVITY
                                    }
                                },
                                modifier = contentModifier
                            )
                        }
                        GideonTab.ACTIVITY -> {
                            ActivityScreen(
                                currentUserId = currentUserId,
                                myListings = mySubmittedListings,
                                transactions = transactions,
                                currencyCode = currencyCode,
                                onConfirmDelivery = { tx ->
                                    viewModel.updateEscrowStatus(
                                        tx,
                                        EscrowStatus.ITEM_DELIVERED,
                                        "Buyer confirmed handover. Ready for Gideon Global final release."
                                    )
                                },
                                onListingClick = { selectedListingId = it.id },
                                modifier = contentModifier
                            )
                        }
                        GideonTab.APPROVALS -> {
                            AdminApprovalsScreen(
                                isCeoAdmin = isCeoAdmin,
                                pendingListings = pendingListings,
                                allListings = allListings,
                                currencyCode = currencyCode,
                                onApproveListing = { viewModel.approveListing(it) },
                                onRejectListing = { listing, reason -> viewModel.rejectListing(listing, reason) },
                                onListingClick = { selectedListingId = it.id },
                                modifier = contentModifier
                            )
                        }
                        GideonTab.ACCOUNT -> {
                            AccountAdminScreen(
                                userProfile = userProfile,
                                userEmail = currentUserEmail,
                                userDisplayName = currentUserDisplayName,
                                isCeoAdmin = isCeoAdmin,
                                pendingListings = pendingListings,
                                allListings = allListings,
                                transactions = transactions,
                                commissionSummary = commissionSummary,
                                currencyCode = currencyCode,
                                onUpdateSellerProfile = viewModel::updateSellerProfile,
                                onSignOut = {
                                    signOutGoogleAndFirebase(
                                        credentialManager = credentialManager,
                                        scope = coroutineScope
                                    )
                                },
                                onApproveListing = { viewModel.approveListing(it) },
                                onRejectListing = { listing, reason -> viewModel.rejectListing(listing, reason) },
                                onDeleteListing = viewModel::deleteListing,
                                onUpdateEscrowStatus = viewModel::updateEscrowStatus,
                                onGoToSell = { currentTab = GideonTab.SELL },
                                onOpenEditProfile = { showEditProfile = true },
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
