package com.example.thehairstory.ui.screens.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thehairstory.data.local.entity.MemberEntity
import com.example.thehairstory.data.local.entity.TransactionEntity
import com.example.thehairstory.data.local.entity.RewardEntity
import com.example.thehairstory.data.local.entity.ServiceEntity
import com.example.thehairstory.ui.theme.GoldAccent
import com.example.thehairstory.ui.theme.PinkDark
import com.example.thehairstory.ui.theme.PinkMain
import com.example.thehairstory.ui.theme.TextDark
import java.text.NumberFormat
import java.util.Locale

enum class TransactionSortOption {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC,
    ID_DESC,
    ID_ASC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    memberId: Int,
    viewModel: MemberDetailViewModel,
    isCustomerMode: Boolean = false,
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: (Int) -> Unit
) {
    LaunchedEffect(memberId) {
        viewModel.setMemberId(memberId)
    }

    val member by viewModel.member.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val servicesCatalog by viewModel.servicesCatalog.collectAsState()
    val rewardsCatalog by viewModel.rewardsCatalog.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    val currentMember = member
    val filteredRewards = remember(rewardsCatalog, currentMember, transactions) {
        if (currentMember == null) emptyList()
        else {
            val earned = currentMember.earnedPoints
            val memberTier = when {
                earned >= 800 -> "PLATINUM"
                earned >= 300 -> "GOLD"
                earned >= 100 -> "SILVER"
                else -> "BRONZE"
            }
            
            fun getTierLevel(t: String): Int = when(t.uppercase()) {
                "PLATINUM" -> 3
                "GOLD" -> 2
                "SILVER" -> 1
                else -> 0
            }

            val level = getTierLevel(memberTier)

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val oneYearAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, -1) }.time

            fun isRewardClaimed(reward: RewardEntity): Boolean {
                val isFreeDrinks = reward.name.contains("Minuman", ignoreCase = true) || reward.name.contains("Drink", ignoreCase = true)
                if (reward.pointsCost == 0 && !isFreeDrinks) {
                    return transactions.any { tx ->
                        val isWithinYear = try {
                            val txDateOnly = tx.date.substring(0, minOf(10, tx.date.length))
                            val parsedDate = sdf.parse(txDateOnly)
                            parsedDate != null && parsedDate.after(oneYearAgo)
                        } catch (e: Exception) {
                            false
                        }
                        if (isWithinYear) {
                            val hasCode = tx.rewardCodes.split(",").map { it.trim() }.contains(reward.rewardCode)
                            val hasName = tx.description.contains(reward.name, ignoreCase = true)
                            hasCode || hasName
                        } else {
                            false
                        }
                    }
                }
                return false
            }

            rewardsCatalog.filter { reward ->
                if (reward.pointsCost == 0) {
                    // Tier benefit
                    val isVoucher = reward.name.contains("Voucher", ignoreCase = true)
                    if (isVoucher) {
                        reward.minTier.uppercase() == memberTier
                    } else {
                        level >= getTierLevel(reward.minTier)
                    }
                } else {
                    // Point reward (available to everyone)
                    true
                }
            }.sortedWith(
                compareBy<RewardEntity> { isRewardClaimed(it) } // claimed goes to bottom
                    .thenByDescending { it.pointsCost > 0 } // point-cost rewards top
                    .thenBy { it.id }
            )
        }
    }

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showTierInfoDialog by remember { mutableStateOf(false) }
    var txSearchQuery by remember { mutableStateOf("") }
    var selectedTransactionForInvoice by remember { mutableStateOf<TransactionEntity?>(null) }
    var txSortOption by remember { mutableStateOf(TransactionSortOption.DATE_DESC) }
    var txSortMenuExpanded by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, txSearchQuery, txSortOption) {
        val baseList = if (txSearchQuery.isBlank()) transactions
        else {
            val q = txSearchQuery.trim().lowercase()
            transactions.filter {
                it.id.toString().contains(q) ||
                it.transactionCode.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.date.contains(q)
            }
        }
        
        when (txSortOption) {
            TransactionSortOption.DATE_DESC -> baseList.sortedWith(compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id })
            TransactionSortOption.DATE_ASC -> baseList.sortedWith(compareBy<TransactionEntity> { it.date }.thenBy { it.id })
            TransactionSortOption.AMOUNT_DESC -> baseList.sortedByDescending { it.amount }
            TransactionSortOption.AMOUNT_ASC -> baseList.sortedBy { it.amount }
            TransactionSortOption.ID_DESC -> baseList.sortedByDescending { it.id }
            TransactionSortOption.ID_ASC -> baseList.sortedBy { it.id }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Confirm Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onNavigateBack()
                }) {
                    Text("Log Out", color = PinkDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Invoice Dialog when history card is clicked
    if (selectedTransactionForInvoice != null) {
        val txn = selectedTransactionForInvoice!!
        val formattedAmount = if (txn.amount <= 0.0) "Rp0" else NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }.format(txn.amount).replace("Rp", "Rp ").replace(",00", "")

        val parsedItems = parseTransactionItems(txn.description, servicesCatalog, rewardsCatalog)

        AlertDialog(
            onDismissRequest = { selectedTransactionForInvoice = null },
            title = { Text("Invoice Receipt", color = PinkDark, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    val displayCode = txn.transactionCode.ifBlank { "TX" + String.format(Locale.US, "%04d", txn.id) }
                    Text("Transaction ID: $displayCode", fontWeight = FontWeight.Bold, color = PinkDark)
                    Text("Date: ${txn.date}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = PinkMain.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Services / Items:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = PinkDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    parsedItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val displayName = if (isCustomerMode) {
                                    cleanDescriptionForCustomer(item.name)
                                } else {
                                    if (item.code.isNotEmpty()) "[${item.code}] ${item.name}" else item.name
                                }
                                Text(displayName, style = MaterialTheme.typography.bodyMedium)
                                if (item.quantity > 1 && !item.isRedemption) {
                                    val formattedUnit = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                                        maximumFractionDigits = 0
                                    }.format(item.unitPrice).replace("Rp", "Rp ").replace(",00", "")
                                    Text("${item.quantity} x $formattedUnit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            
                            val displayTotal = if (item.isRedemption) {
                                if (item.subtotalPrice < 0) {
                                    val absFormatted = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                                        maximumFractionDigits = 0
                                    }.format(-item.subtotalPrice).replace("Rp", "Rp ").replace(",00", "")
                                    "-$absFormatted"
                                } else {
                                    "Rp0"
                                }
                            } else {
                                val formattedTotal = if (item.subtotalPrice <= 0.0) "Rp0" else NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                                    maximumFractionDigits = 0
                                }.format(item.subtotalPrice).replace("Rp", "Rp ").replace(",00", "")
                                formattedTotal
                            }
                            
                            val totalColor = MaterialTheme.colorScheme.onSurface
                            Text(
                                text = displayTotal,
                                style = MaterialTheme.typography.bodyMedium,
                                color = totalColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = PinkMain.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Payment:", fontWeight = FontWeight.Bold)
                        Text(text = if (txn.amount > 0) formattedAmount else "Rp0", fontWeight = FontWeight.Bold, color = PinkDark)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val sign = if (txn.pointsEarned >= 0) "+" else ""
                    val pointsColor = if (txn.pointsEarned >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Points Change:")
                        Text("$sign${txn.pointsEarned} pts", fontWeight = FontWeight.Bold, color = pointsColor)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTransactionForInvoice = null }) {
                    Text("Close", color = PinkDark)
                }
            }
        )
    }

    if (showTierInfoDialog) {
        TierInfoDialog(
            rewardsCatalog = rewardsCatalog,
            onDismissRequest = { showTierInfoDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isCustomerMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.thehairstory.R.drawable.logo_ths),
                                contentDescription = "Salon Logo",
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "The Hair Story",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PinkDark
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "Member Profile",
                            style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PinkDark
                                )
                        )
                    }
                },
                navigationIcon = {
                    if (!isCustomerMode) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PinkDark
                            )
                        }
                    }
                },
                actions = {
                    if (isCustomerMode) {
                        IconButton(onClick = { showLogoutConfirm = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = PinkDark
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        member?.let { currentMember ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Glassmorphism Premium Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { showTierInfoDialog = true }
                ) {
                    PremiumMembershipCard(member = currentMember, transactions = transactions)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Single Quick Action Button (Only visible to Staff/Kasir)
                if (!isCustomerMode) {
                    Button(
                        onClick = { onNavigateToAddTransaction(currentMember.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PinkMain,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Transaction")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "New Transaction", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Interactive Tab Selector (Only visible to Customer Mode)
                if (isCustomerMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabTitles = listOf("History", "Services", "Rewards")
                        tabTitles.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            val bgColor = if (isSelected) PinkMain else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            val textColor = if (isSelected) Color.White else PinkDark
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bgColor)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                if (!isCustomerMode || selectedTab == 0) {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PinkDark
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Transaction Search Bar with Sort Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = txSearchQuery,
                            onValueChange = { txSearchQuery = it },
                            placeholder = { Text("Search Transactions") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = PinkDark
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PinkMain,
                                focusedLabelColor = PinkMain
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box {
                            IconButton(onClick = { txSortMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort Transactions",
                                    tint = PinkDark
                                )
                            }
                            DropdownMenu(
                                expanded = txSortMenuExpanded,
                                onDismissRequest = { txSortMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Terbaru") },
                                    onClick = {
                                        txSortOption = TransactionSortOption.DATE_DESC
                                        txSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Terlama") },
                                    onClick = {
                                        txSortOption = TransactionSortOption.DATE_ASC
                                        txSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nominal Tertinggi") },
                                    onClick = {
                                        txSortOption = TransactionSortOption.AMOUNT_DESC
                                        txSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nominal Terendah") },
                                    onClick = {
                                        txSortOption = TransactionSortOption.AMOUNT_ASC
                                        txSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ID Terbesar") },
                                    onClick = {
                                        txSortOption = TransactionSortOption.ID_DESC
                                        txSortMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("ID Terkecil") },
                                    onClick = {
                                        txSortOption = TransactionSortOption.ID_ASC
                                        txSortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = PinkMain.copy(alpha = 0.2f), thickness = 1.dp)

                    if (filteredTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (transactions.isEmpty()) "No transactions recorded yet." else "No matching transactions found.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredTransactions, key = { it.id }) { txn ->
                                TransactionItemRow(
                                    transaction = txn,
                                    isCustomerMode = isCustomerMode,
                                    onClick = { selectedTransactionForInvoice = txn }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    Text(
                        text = "Our Services & Price List",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PinkDark
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HorizontalDivider(color = PinkMain.copy(alpha = 0.2f), thickness = 1.dp)
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(servicesCatalog, key = { it.id }) { service ->
                            val formattedPrice = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                                maximumFractionDigits = 0
                            }.format(service.price).replace("Rp", "Rp ").replace(",00", "")
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = service.name,
                                            color = TextDark,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Text(
                                        text = formattedPrice,
                                        color = PinkMain,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                } else {
                    Text(
                        text = "Loyalty Rewards",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PinkDark
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    HorizontalDivider(color = PinkMain.copy(alpha = 0.2f), thickness = 1.dp)
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredRewards, key = { it.id }) { reward ->
                            val formattedDiscount = if (reward.discountValue > 0) {
                                NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
                                    maximumFractionDigits = 0
                                }.format(reward.discountValue).replace("Rp", "Rp ").replace(",00", "")
                            } else null
                            
                            val isClaimedLastYear = remember(reward, transactions) {
                                val isFreeDrinks = reward.name.contains("Minuman", ignoreCase = true) || reward.name.contains("Drink", ignoreCase = true)
                                if (reward.pointsCost == 0 && !isFreeDrinks) {
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                    val oneYearAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, -1) }.time
                                    transactions.any { tx ->
                                        val isWithinYear = try {
                                            val txDateOnly = tx.date.substring(0, minOf(10, tx.date.length))
                                            val parsedDate = sdf.parse(txDateOnly)
                                            parsedDate != null && parsedDate.after(oneYearAgo)
                                        } catch (e: Exception) {
                                            false
                                        }
                                        if (isWithinYear) {
                                            val hasCode = tx.rewardCodes.split(",").map { it.trim() }.contains(reward.rewardCode)
                                            val hasName = tx.description.contains(reward.name, ignoreCase = true)
                                            hasCode || hasName
                                        } else {
                                            false
                                        }
                                    }
                                } else {
                                    false
                                }
                            }

                            val containerColor = if (isClaimedLastYear) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = containerColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isClaimedLastYear) 0.dp else 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = reward.name,
                                            color = if (isClaimedLastYear) PinkDark.copy(alpha = 0.5f) else PinkDark,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = when {
                                                reward.pointsCost > 0 -> "Cost: ${reward.pointsCost} points"
                                                else -> "Tier Benefit"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isClaimedLastYear) {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            }
                                        )
                                        if (isClaimedLastYear) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Claimed this year",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                    }
                                    if (formattedDiscount != null) {
                                        Text(
                                            text = "Save $formattedDiscount",
                                            color = if (isClaimedLastYear) GoldAccent.copy(alpha = 0.5f) else GoldAccent,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Loading member data...", color = PinkDark)
            }
        }
    }
}

@Composable
fun PremiumMembershipCard(member: MemberEntity, transactions: List<TransactionEntity> = emptyList()) {
    val resetDateStr = remember(member.id, transactions) {
        val oldestTx = transactions.minByOrNull { it.date }
        if (oldestTx != null && oldestTx.date.isNotEmpty()) {
            try {
                val dateOnly = oldestTx.date.substring(0, minOf(10, oldestTx.date.length))
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val parsedDate = sdf.parse(dateOnly)
                if (parsedDate != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = parsedDate
                    cal.add(java.util.Calendar.YEAR, 1)
                    sdf.format(cal.time)
                } else {
                    "2027-06-17"
                }
            } catch (e: Exception) {
                "2027-06-17"
            }
        } else {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.YEAR, 1)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            sdf.format(cal.time)
        }
    }

    // Tier based on EARNED points (total accumulated, never decremented)
    val earned = member.earnedPoints
    val tier = when {
        earned >= 800 -> "PLATINUM"
        earned >= 300 -> "GOLD"
        earned >= 100 -> "SILVER"
        else -> "BRONZE"
    }

    // Next tier threshold
    val (nextTierName, nextTierThreshold) = when {
        earned >= 800 -> Pair("PLATINUM", 800)
        earned >= 300 -> Pair("PLATINUM", 800)
        earned >= 100 -> Pair("GOLD", 300)
        else -> Pair("SILVER", 100)
    }
    val prevTierThreshold = when {
        earned >= 800 -> 800
        earned >= 300 -> 300
        earned >= 100 -> 100
        else -> 0
    }
    val progressFraction = if (nextTierThreshold == prevTierThreshold) 1f
    else ((earned - prevTierThreshold).toFloat() / (nextTierThreshold - prevTierThreshold)).coerceIn(0f, 1f)

    val tierColor = when (tier) {
        "PLATINUM" -> Color(0xFF90CAF9)
        "GOLD" -> GoldAccent
        "SILVER" -> Color(0xFFBDBDBD)
        else -> Color(0xFFCD7F32)
    }

    val cardBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFB6F92),
            Color(0xFFFF8FAB),
            Color(0xFFFFC2D1)
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = cardBrush)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$tier MEMBER",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier
                            .background(
                                color = tierColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                // Barcode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Code39Barcode(
                        text = member.memberCode,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Tier Progress Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$earned pts earned",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 9.sp
                            )
                        )
                        if (earned < 800) {
                            Text(
                                text = "$nextTierThreshold pts → $nextTierName",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 9.sp
                                )
                            )
                        } else {
                            Text(
                                text = "Max Tier ✦",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = tierColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(5.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.White, tierColor)
                                    ),
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tier resets on: $resetDateStr",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 9.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    )
                }

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = member.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = member.memberCode,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "POINTS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${member.points} pts",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Code39Barcode(text: String, modifier: Modifier = Modifier) {
    // Standard Code 39 patterns: 9 elements per character (5 bars + 4 spaces, alternating)
    // Each element is 'N' (narrow=1 unit) or 'W' (wide=2.5 units)
    // Pattern order: Bar Space Bar Space Bar Space Bar Space Bar
    // 1 = wide, 0 = narrow
    val code39Map = mapOf(
        '0' to intArrayOf(0,0,0,1,1,0,1,0,0), // NNNWWNWNN
        '1' to intArrayOf(1,0,0,1,0,0,0,0,1), // WNNWNNNNW
        '2' to intArrayOf(0,0,1,1,0,0,0,0,1), // NNWWNNNNW (fixed)
        '3' to intArrayOf(1,0,1,1,0,0,0,0,0), // WNWWNNNNW
        '4' to intArrayOf(0,0,0,1,1,0,0,0,1), // NNNWWNNNW ✓
        '5' to intArrayOf(1,0,0,1,1,0,0,0,0), // WNNWWNNN
        '6' to intArrayOf(0,0,1,1,1,0,0,0,0), // NNWWWNNN
        '7' to intArrayOf(0,0,0,1,0,0,1,0,1), // NNNWNNWNW
        '8' to intArrayOf(1,0,0,1,0,0,1,0,0), // WNNWNNWNN
        '9' to intArrayOf(0,0,1,1,0,0,1,0,0), // NNWWNNWNN
        'A' to intArrayOf(1,0,0,0,0,1,0,0,1), // WNNNNWNNW
        'B' to intArrayOf(0,0,1,0,0,1,0,0,1), // NNWNNWNNW
        'C' to intArrayOf(1,0,1,0,0,1,0,0,0), // WNWNNWNNN
        'D' to intArrayOf(0,0,0,0,1,1,0,0,1), // NNNNWWNNW
        'E' to intArrayOf(1,0,0,0,1,1,0,0,0), // WNNNNWWNN -> WNNNNWNNN fixed
        'F' to intArrayOf(0,0,1,0,1,1,0,0,0), // NNWNWWNNN
        'G' to intArrayOf(0,0,0,0,0,1,1,0,1), // NNNNNWWNW
        'H' to intArrayOf(1,0,0,0,0,1,1,0,0), // WNNNNWWNN
        'I' to intArrayOf(0,0,1,0,0,1,1,0,0), // NNWNNWWNN
        'J' to intArrayOf(0,0,0,0,1,1,1,0,0), // NNNNWWWNN
        'K' to intArrayOf(1,0,0,0,0,0,0,1,1), // WNNNNNNWW
        'L' to intArrayOf(0,0,1,0,0,0,0,1,1), // NNWNNNNWW
        'M' to intArrayOf(1,0,1,0,0,0,0,1,0), // WNWNNNNNW
        'N' to intArrayOf(0,0,0,0,1,0,0,1,1), // NNNNWNNWW
        'O' to intArrayOf(1,0,0,0,1,0,0,1,0), // WNNNNWNNW -> WNNNWNNNW
        'P' to intArrayOf(0,0,1,0,1,0,0,1,0), // NNWNWNNWW
        'Q' to intArrayOf(0,0,0,0,0,0,1,1,1), // NNNNNNWWW -> NNNNWNWWW
        'R' to intArrayOf(1,0,0,0,0,0,1,1,0), // WNNNNNWWN = 0x106 (ZXing)
        'S' to intArrayOf(0,0,1,0,0,0,1,1,0), // NNWNNNWWN = 0x046 (ZXing)
        'T' to intArrayOf(0,0,0,0,1,0,1,1,0), // NNNNWNWWN = 0x016 (ZXing)
        'U' to intArrayOf(1,1,0,0,0,0,0,0,1), // WWNNNNNNW
        'V' to intArrayOf(0,1,1,0,0,0,0,0,1), // NWWNNNNNW
        'W' to intArrayOf(1,1,1,0,0,0,0,0,0), // WWWNNNNN
        'X' to intArrayOf(0,1,0,0,1,0,0,0,1), // NWNNWNNNNW -> NWNNWNNNW
        'Y' to intArrayOf(1,1,0,0,1,0,0,0,0), // WWNNWNNNN
        'Z' to intArrayOf(0,1,1,0,1,0,0,0,0), // NWWNWNNNN
        '-' to intArrayOf(0,1,0,0,0,0,1,0,1), // NWNNNNWNW
        '.' to intArrayOf(1,1,0,0,0,0,1,0,0), // WWNNNNWNN
        ' ' to intArrayOf(0,1,1,0,0,0,1,0,0), // NWWNNNWNN
        '$' to intArrayOf(0,1,0,1,0,1,0,0,0), // NWNWNWNNN
        '/' to intArrayOf(0,1,0,1,0,0,0,1,0), // NWNWNNNWN
        '+' to intArrayOf(0,1,0,0,0,1,0,1,0), // NWNNNWNWN
        '%' to intArrayOf(0,0,0,1,0,1,0,1,0), // NNNWNWNWN
        '*' to intArrayOf(0,1,0,0,1,0,1,0,0)  // NWNNWNWNN  ← start/stop
    )

    val upperText = "*" + text.uppercase() + "*"
    val narrowRatio = 1f
    val wideRatio = 2.5f

    Canvas(modifier = modifier) {
        val quietZone = size.width * 0.04f
        val availableWidth = size.width - quietZone * 2f

        // Calculate total units across all chars
        var totalUnits = 0f
        for (char in upperText) {
            val pattern = code39Map[char] ?: continue
            for (bit in pattern) {
                totalUnits += if (bit == 1) wideRatio else narrowRatio
            }
            totalUnits += narrowRatio // inter-character gap
        }
        if (totalUnits == 0f) return@Canvas

        val pixelPerUnit = availableWidth / totalUnits
        var startX = quietZone

        for ((charIndex, char) in upperText.withIndex()) {
            val pattern = code39Map[char] ?: continue
            for (i in pattern.indices) {
                val units = if (pattern[i] == 1) wideRatio else narrowRatio
                val width = units * pixelPerUnit
                val isBar = i % 2 == 0 // even indices = bars (black), odd = spaces (white)
                if (isBar) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(startX, 0f),
                        size = Size(width, size.height)
                    )
                }
                startX += width
            }
            // Inter-character gap (narrow white space)
            if (charIndex < upperText.length - 1) {
                startX += narrowRatio * pixelPerUnit
            }
        }
    }
}


@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    isCustomerMode: Boolean,
    onClick: () -> Unit
) {
    val isRedemption = transaction.pointsEarned < 0
    val pointsColor = if (isRedemption) Color(0xFFD32F2F) else PinkMain
    val pointsSign = if (isRedemption) "" else "+"
    
    val formattedAmount = if (transaction.amount <= 0.0) "Rp0" else NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }.format(transaction.amount).replace("Rp", "Rp ").replace(",00", "")

    val displayDescription = if (isCustomerMode) cleanDescriptionForCustomer(transaction.description) else transaction.description

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val displayCode = transaction.transactionCode.ifBlank { "TX" + String.format(Locale.US, "%04d", transaction.id) }
                Text(
                    text = "Transaction ID: $displayCode",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PinkDark
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayDescription,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = transaction.date,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = PinkDark,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Text(
                text = "$pointsSign${transaction.pointsEarned} pts",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = pointsColor
                )
            )
        }
    }
}

@Composable
fun TierInfoDialog(
    rewardsCatalog: List<RewardEntity>,
    onDismissRequest: () -> Unit
) {
    var expandedTier by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Tutup", color = PinkDark, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = "Membership Tier Benefits",
                color = PinkDark,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Points & Membership Rules:",
                    fontWeight = FontWeight.Bold,
                    color = PinkDark,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Earn 1 point for every Rp10,000 spent.\n" +
                           "• Redeemed points (Available Points) DO NOT reduce yearly accumulated points (Earned Points) used for tier evaluation.\n" +
                           "• Tier is determined based on total points earned in the last 12 months (Earned Points).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tier Items
                val tiers = listOf(
                    TierData(
                        name = "BRONZE",
                        points = "0 - 99 Points",
                        color = Color(0xFFCD7F32),
                        benefits = rewardsCatalog.filter { it.pointsCost == 0 && it.minTier.equals("BRONZE", ignoreCase = true) }.map { it.name }.ifEmpty { listOf("No benefits") }
                    ),
                    TierData(
                        name = "SILVER",
                        points = "100 - 299 Points",
                        color = Color(0xFFBDBDBD),
                        benefits = rewardsCatalog.filter { it.pointsCost == 0 && it.minTier.equals("SILVER", ignoreCase = true) }.map { it.name }.ifEmpty { listOf("No benefits") }
                    ),
                    TierData(
                        name = "GOLD",
                        points = "300 - 799 Points",
                        color = GoldAccent,
                        benefits = rewardsCatalog.filter { it.pointsCost == 0 && it.minTier.equals("GOLD", ignoreCase = true) }.map { it.name }.ifEmpty { listOf("No benefits") }
                    ),
                    TierData(
                        name = "PLATINUM",
                        points = "800+ Points",
                        color = Color(0xFF90CAF9),
                        benefits = rewardsCatalog.filter { 
                            val isFreeDrinks = it.name.contains("Minuman", ignoreCase = true) || it.name.contains("Drink", ignoreCase = true)
                            it.pointsCost == 0 && (it.minTier.equals("PLATINUM", ignoreCase = true) || (it.minTier.equals("GOLD", ignoreCase = true) && isFreeDrinks))
                        }.map { 
                            if (it.minTier.equals("GOLD", ignoreCase = true)) {
                                it.name.replace("Benefit Gold", "Benefit Platinum", ignoreCase = true)
                                    .replace("Gold Benefit", "Platinum Benefit", ignoreCase = true)
                            } else {
                                it.name
                            }
                        }.ifEmpty { listOf("No benefits") }
                    )
                )

                tiers.forEach { tier ->
                    val isExpanded = expandedTier == tier.name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.dp, tier.color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                expandedTier = if (isExpanded) null else tier.name
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = tier.color.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isExpanded) "▼" else "▶",
                                        color = tier.color,
                                        modifier = Modifier.padding(end = 8.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = tier.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tier.color,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = tier.points,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = tier.color
                                )
                            }
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                tier.benefits.forEach { benefit ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✦",
                                            color = tier.color,
                                            modifier = Modifier.padding(end = 6.dp),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = benefit,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

data class TierData(
    val name: String,
    val points: String,
    val color: Color,
    val benefits: List<String>
)

fun cleanDescriptionForCustomer(description: String): String {
    val regex = Regex("""(SV\d{4}|RW\d{4}|RW-BENEFIT-[A-Z-]+)\s*""")
    return description.replace(regex, "").trim()
}

data class TransactionDisplayItem(
    val code: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotalPrice: Double,
    val isRedemption: Boolean
)

fun parseTransactionItems(
    description: String,
    services: List<com.example.thehairstory.data.local.entity.ServiceEntity>,
    rewards: List<com.example.thehairstory.data.local.entity.RewardEntity>
): List<TransactionDisplayItem> {
    val items = mutableListOf<TransactionDisplayItem>()
    val lines = description.split("\n")
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue

        // Check if it's a service line
        val serviceMatch = Regex("""^(SV\d{4})\s+(.*?)(?:\s+x(\d+))?$""").find(trimmed)
        if (serviceMatch != null) {
            val code = serviceMatch.groupValues[1]
            val name = serviceMatch.groupValues[2]
            val qtyStr = serviceMatch.groupValues[3]
            val qty = if (qtyStr.isNotEmpty()) qtyStr.toIntOrNull() ?: 1 else 1

            // Look up service in catalog
            val serviceEntity = services.find { it.serviceCode == code }
            val unitPrice = serviceEntity?.price ?: 0.0
            items.add(
                TransactionDisplayItem(
                    code = code,
                    name = name,
                    quantity = qty,
                    unitPrice = unitPrice,
                    subtotalPrice = unitPrice * qty,
                    isRedemption = false
                )
            )
            continue
        }

        // Check if it's a redeem line (either starting with "Redeem: " or containing "[Redeem: ...]")
        if (trimmed.contains("Redeem", ignoreCase = true)) {
            val rewardCodesInLine = Regex("""RW\d{4}""").findAll(trimmed).map { it.value }.toList()
            if (rewardCodesInLine.isNotEmpty()) {
                for (code in rewardCodesInLine) {
                    val rewardEntity = rewards.find { it.rewardCode == code }
                    if (rewardEntity != null) {
                        items.add(
                            TransactionDisplayItem(
                                code = code,
                                name = rewardEntity.name,
                                quantity = 1,
                                unitPrice = rewardEntity.discountValue,
                                subtotalPrice = -rewardEntity.discountValue,
                                isRedemption = true
                            )
                        )
                    }
                }
            } else {
                // If there are no reward codes, let's see if we can match the reward by name
                val cleanName = trimmed
                    .replace(Regex("""^Redeem:\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""^\[Redeem:\s*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\]$"""), "")
                    .trim()
                
                val rewardEntity = rewards.find { it.name.equals(cleanName, ignoreCase = true) }
                if (rewardEntity != null) {
                    items.add(
                        TransactionDisplayItem(
                            code = rewardEntity.rewardCode,
                            name = rewardEntity.name,
                            quantity = 1,
                            unitPrice = rewardEntity.discountValue,
                            subtotalPrice = -rewardEntity.discountValue,
                            isRedemption = true
                        )
                    )
                } else {
                    items.add(
                        TransactionDisplayItem(
                            code = "",
                            name = cleanName,
                            quantity = 1,
                            unitPrice = 0.0,
                            subtotalPrice = 0.0,
                            isRedemption = true
                        )
                    )
                }
            }
            continue
        }

        // Fallback for lines that don't match SV or Redeem (e.g. ad-hoc descriptions)
        items.add(
            TransactionDisplayItem(
                code = "",
                name = trimmed,
                quantity = 1,
                unitPrice = 0.0,
                subtotalPrice = 0.0,
                isRedemption = false
            )
        )
    }
    return items
}
