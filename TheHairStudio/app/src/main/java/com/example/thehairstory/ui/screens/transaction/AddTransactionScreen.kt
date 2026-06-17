package com.example.thehairstory.ui.screens.transaction

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thehairstory.data.local.entity.RewardEntity
import com.example.thehairstory.data.local.entity.ServiceEntity
import com.example.thehairstory.ui.theme.GoldAccent
import com.example.thehairstory.ui.theme.PinkDark
import com.example.thehairstory.ui.theme.PinkLight
import com.example.thehairstory.ui.theme.PinkMain
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    memberId: Int,
    viewModel: AddTransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val member by viewModel.member.collectAsState()
    val memberTransactions by viewModel.memberTransactions.collectAsState()
    val selectedServices by viewModel.selectedServices.collectAsState()
    val selectedRewards by viewModel.selectedRewards.collectAsState()
    val servicesCatalog by viewModel.servicesCatalog.collectAsState()
    val rewardsCatalog by viewModel.rewardsCatalog.collectAsState()
    val state by viewModel.transactionState.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showTreatmentSheet by remember { mutableStateOf(false) }
    var showRewardSheet by remember { mutableStateOf(false) }

    val treatmentSheetState = rememberModalBottomSheetState()
    val rewardSheetState = rememberModalBottomSheetState()

    LaunchedEffect(memberId) {
        viewModel.loadMember(memberId)
    }

    LaunchedEffect(state) {
        if (state is TransactionState.Success) {
            viewModel.resetState()
            onNavigateBack()
        }
    }

    // Calculations
    val subtotal = selectedServices.entries.sumOf { (svc, qty) -> svc.price * qty }
    val discount = selectedRewards.sumOf { it.discountValue }
    val grandTotal = maxOf(0.0, subtotal - discount)
    val pointsEarned = (grandTotal / 10000).toInt()
    val pointsCost = selectedRewards.sumOf { it.pointsCost }
    val netPointsChange = pointsEarned - pointsCost

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Transaction") },
            text = { Text("Are you sure you want to save this transaction with a total of ${formatRupiah(grandTotal)}?") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    viewModel.submitTransaction(memberId)
                }) {
                    Text("Save", color = PinkDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Bottom Sheet for Service Selection
    if (showTreatmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTreatmentSheet = false },
            sheetState = treatmentSheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Select Service / Treatment",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PinkDark
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable services list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    servicesCatalog.forEach { svc ->
                        val qty = selectedServices[svc] ?: 0
                        val isSelected = qty > 0
                        ServiceCard(
                            service = svc,
                            quantity = qty,
                            isSelected = isSelected,
                            onClick = { viewModel.toggleService(svc) },
                            onIncrease = { viewModel.incrementService(svc) },
                            onDecrease = { viewModel.decrementService(svc) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showTreatmentSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkMain)
                ) {
                    Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Modal Bottom Sheet for Reward Selection
    if (showRewardSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRewardSheet = false },
            sheetState = rewardSheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Redeem Loyalty Reward",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PinkDark
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select rewards to apply (multiple rewards can be selected if points are sufficient).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable rewards list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val sortedRewards = remember(rewardsCatalog, memberTransactions) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        
                        rewardsCatalog.map { reward ->
                            val isFreeDrinks = reward.name.contains("Minuman", ignoreCase = true) || reward.name.contains("Drink", ignoreCase = true)
                            val isClaimedLastYear = if (reward.pointsCost == 0 && !isFreeDrinks) {
                                val oneYearAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.YEAR, -1) }.time
                                memberTransactions.any { tx ->
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
                            Pair(reward, isClaimedLastYear)
                        }.sortedWith(
                            compareBy<Pair<RewardEntity, Boolean>> { it.second }
                                .thenByDescending { it.first.pointsCost > 0 }
                                .thenBy { it.first.id }
                        )
                    }

                    sortedRewards.forEach { (reward, isClaimedLastYear) ->
                        val isSelected = selectedRewards.contains(reward)

                        val totalCostOfOthers = selectedRewards.filter { it != reward }.sumOf { it.pointsCost }
                        val isPointsSufficient = (member?.points ?: 0) >= (totalCostOfOthers + reward.pointsCost)
                        val isEnabled = isPointsSufficient && !isClaimedLastYear

                        RewardSelectionCard(
                            reward = reward,
                            isSelected = isSelected,
                            isEnabled = isEnabled,
                            isClaimedLastYear = isClaimedLastYear,
                            onClick = { viewModel.toggleReward(reward) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showRewardSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkMain)
                ) {
                    Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transaksi Baru",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PinkDark
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PinkDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Member Info Box
            member?.let { currentMember ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PinkLight.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Customer: ${currentMember.name}",
                                fontWeight = FontWeight.Bold,
                                color = PinkDark,
                                fontSize = 16.sp
                            )
                            val totalRedeemCost = selectedRewards.sumOf { it.pointsCost }
                            val remainingPoints = currentMember.points - totalRedeemCost
                            Text(
                                text = "Current Points: ${currentMember.points} pts (Remaining: $remainingPoints pts)",
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SELECTION ROW 1: SERVICES TRIGGER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTreatmentSheet = true }
                    .border(1.dp, PinkMain.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.thehairstory.R.drawable.logo_ths),
                        contentDescription = "Salon Logo",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Select Service / Treatment",
                            fontWeight = FontWeight.Bold,
                            color = PinkDark,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (selectedServices.isEmpty()) "No services selected yet" else "${selectedServices.values.sum()} Services Selected",
                            color = if (selectedServices.isEmpty()) Color.Gray else PinkMain,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SELECTION ROW 2: REWARDS TRIGGER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRewardSheet = true }
                    .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Redeem Loyalty Reward",
                            fontWeight = FontWeight.Bold,
                            color = PinkDark,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (selectedRewards.isEmpty()) "No rewards selected yet" else "${selectedRewards.size} Rewards Selected",
                            color = if (selectedRewards.isEmpty()) Color.Gray else GoldAccent,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: INVOICE / RECEIPT
            if (selectedServices.isNotEmpty()) {
                InvoiceReceiptCard(
                    selectedServices = selectedServices,
                    selectedRewards = selectedRewards,
                    subtotal = subtotal,
                    discount = discount,
                    grandTotal = grandTotal,
                    pointsEarned = pointsEarned,
                    pointsCost = pointsCost,
                    netPointsChange = netPointsChange
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (state is TransactionState.Error) {
                    Text(
                        text = (state as TransactionState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = state !is TransactionState.Loading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkMain,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (state is TransactionState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Save Transaction",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Please select at least one treatment above to record a transaction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceCard(
    service: ServiceEntity,
    quantity: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) PinkMain else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PinkLight else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
            ) {
                Text(
                    text = service.name,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PinkDark else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatRupiah(service.price),
                    color = if (isSelected) PinkDark.copy(alpha = 0.8f) else PinkMain,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (isSelected) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(PinkMain, RoundedCornerShape(6.dp))
                            .clickable(onClick = onDecrease),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    
                    Text(
                        text = quantity.toString(),
                        fontWeight = FontWeight.Bold,
                        color = PinkDark,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(PinkMain, RoundedCornerShape(6.dp))
                            .clickable(onClick = onIncrease),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RewardSelectionCard(
    reward: RewardEntity,
    isSelected: Boolean,
    isEnabled: Boolean,
    isClaimedLastYear: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        Color(0xFFFFF9E6)
    } else if (isEnabled && !isClaimedLastYear) {
        MaterialTheme.colorScheme.surface
    } else {
        Color(0xFFF5F5F5) // Disabled or claimed card background
    }

    val contentColor = if (isEnabled && !isClaimedLastYear) {
        PinkDark
    } else {
        Color(0xFF9E9E9E) // Dimmed text color
    }

    val pointsColor = if (isEnabled && !isClaimedLastYear) {
        GoldAccent
    } else {
        Color(0xFF9E9E9E)
    }

    val discountColor = if (isEnabled && !isClaimedLastYear) {
        Color(0xFF2E7D32)
    } else {
        Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled && !isClaimedLastYear, onClick = onClick)
            .let {
                if (isEnabled && !isClaimedLastYear) {
                    it.border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) GoldAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    it
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled && !isClaimedLastYear) 1.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reward.name,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (reward.pointsCost > 0) "${reward.pointsCost} Points" else "Tier Benefit",
                    color = pointsColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isClaimedLastYear) {
                Text(
                    text = "Redeemed",
                    color = Color(0xFF9E9E9E), // Grey color matching other disabled reward cards
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "-${formatRupiah(reward.discountValue)}",
                    color = discountColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun InvoiceReceiptCard(
    selectedServices: Map<ServiceEntity, Int>,
    selectedRewards: List<RewardEntity>,
    subtotal: Double,
    discount: Double,
    grandTotal: Double,
    pointsEarned: Int,
    pointsCost: Int,
    netPointsChange: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "INVOICE / SHOPPING RECEIPT",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = PinkDark,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(16.dp))

            selectedServices.forEach { (svc, qty) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = if (qty > 1) "${svc.name} x$qty" else svc.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = formatRupiah(svc.price * qty),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Subtotal", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = formatRupiah(subtotal),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            if (selectedRewards.isNotEmpty()) {
                selectedRewards.forEach { reward ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "Redeem: ${reward.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "-${formatRupiah(reward.discountValue)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            HorizontalDivider(
                color = PinkMain.copy(alpha = 0.2f),
                thickness = 1.5.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Payment",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PinkDark,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = formatRupiah(grandTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PinkDark,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PinkLight.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Points Change Summary:",
                        fontWeight = FontWeight.Bold,
                        color = PinkDark,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Points Earned (+)", style = MaterialTheme.typography.bodySmall)
                        Text(text = "+$pointsEarned pts", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                    if (selectedRewards.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Redeem Points (-)", style = MaterialTheme.typography.bodySmall)
                            Text(text = "-$pointsCost pts", color = PinkDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun formatRupiah(amount: Double): String {
    if (amount <= 0.0) return "Rp0"
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ").replace(",00", "")
}
