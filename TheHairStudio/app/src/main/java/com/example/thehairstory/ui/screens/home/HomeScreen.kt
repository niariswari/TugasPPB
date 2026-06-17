package com.example.thehairstory.ui.screens.home

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.core.content.ContextCompat
import com.example.thehairstory.data.local.entity.MemberEntity
import com.example.thehairstory.ui.theme.GoldAccent
import com.example.thehairstory.ui.theme.PinkDark
import com.example.thehairstory.ui.theme.PinkMain
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

enum class MemberSortOption {
    POINTS_DESC,
    POINTS_ASC,
    NAME_ASC,
    NAME_DESC,
    ID_ASC,
    ID_DESC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    onLogout: () -> Unit
) {
    val members by viewModel.allMembers.collectAsState()
    val totalMembers by viewModel.memberCount.collectAsState()

    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var scanInputCode by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }
    var memberSearchQuery by remember { mutableStateOf("") }

    var selectedTierFilter by remember { mutableStateOf("ALL") }
    var memberSortOption by remember { mutableStateOf(MemberSortOption.POINTS_DESC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val filteredMembers = remember(members, memberSearchQuery, selectedTierFilter, memberSortOption) {
        val q = memberSearchQuery.trim().lowercase()
        val list = members.filter {
            val matchesSearch = if (q.isBlank()) true else {
                it.id.toString().contains(q) ||
                it.name.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.phone.contains(q) ||
                it.memberCode.lowercase().contains(q)
            }
            
            val tier = when {
                it.earnedPoints >= 800 -> "PLATINUM"
                it.earnedPoints >= 300 -> "GOLD"
                it.earnedPoints >= 100 -> "SILVER"
                else -> "BRONZE"
            }
            val matchesTier = selectedTierFilter == "ALL" || tier == selectedTierFilter
            
            matchesSearch && matchesTier
        }
        
        when (memberSortOption) {
            MemberSortOption.POINTS_DESC -> list.sortedWith(compareByDescending<MemberEntity> { it.earnedPoints }.thenBy { it.name })
            MemberSortOption.POINTS_ASC -> list.sortedWith(compareBy<MemberEntity> { it.earnedPoints }.thenBy { it.name })
            MemberSortOption.NAME_ASC -> list.sortedBy { it.name }
            MemberSortOption.NAME_DESC -> list.sortedByDescending { it.name }
            MemberSortOption.ID_ASC -> list.sortedBy { it.id }
            MemberSortOption.ID_DESC -> list.sortedByDescending { it.id }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Confirm Log Out") },
            text = { Text("Are you sure you want to log out of the Staff account?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onLogout()
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

    // Camera Barcode Scanner Dialog
    if (showScanner) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val coroutineScope = rememberCoroutineScope()

        // AtomicBoolean to prevent multiple navigation triggers from analyzer thread
        val isNavigating = remember { AtomicBoolean(false) }

        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            )
        }

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
        }

        LaunchedEffect(Unit) {
            if (!hasPermission) {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }

        AlertDialog(
            onDismissRequest = {
                isNavigating.set(false)
                showScanner = false
            },
            title = { Text("Scan Member Card", color = PinkDark, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Point camera at the barcode on the customer's app",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Viewport Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasPermission) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx)
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().apply {
                                            setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()

                                        val options = BarcodeScannerOptions.Builder()
                                            .setBarcodeFormats(
                                                Barcode.FORMAT_CODE_39,
                                                Barcode.FORMAT_QR_CODE,
                                                Barcode.FORMAT_CODE_128
                                            )
                                            .build()
                                        val barcodeScanner = BarcodeScanning.getClient(options)

                                        val mainHandler = Handler(Looper.getMainLooper())

                                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                val image = InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.imageInfo.rotationDegrees
                                                )
                                                barcodeScanner.process(image)
                                                    .addOnSuccessListener { barcodes ->
                                                        for (barcode in barcodes) {
                                                            val rawValue = barcode.rawValue
                                                            Log.d("SCANNER", "Raw barcode: $rawValue | format: ${barcode.format}")
                                                            if (rawValue != null) {
                                                                // Clean: remove * (Code39 delimiters), trim whitespace
                                                                val cleanValue = rawValue
                                                                    .trim()
                                                                    .replace("*", "")
                                                                    .trim()
                                                                    .uppercase()
                                                                Log.d("SCANNER", "Clean value: $cleanValue")

                                                                if (cleanValue.startsWith("MB") && !isNavigating.getAndSet(true)) {
                                                                    // Switch to main thread to interact with Compose state
                                                                    mainHandler.post {
                                                                        coroutineScope.launch {
                                                                            try {
                                                                                Log.d("SCANNER", "Looking up member: $cleanValue")
                                                                                val member = viewModel.getMemberByCode(cleanValue)
                                                                                Log.d("SCANNER", "Member found: $member")
                                                                                if (member != null) {
                                                                                    showScanner = false
                                                                                    onNavigateToDetail(member.id)
                                                                                } else {
                                                                                    scanError = "Kode member tidak ditemukan ($cleanValue)!"
                                                                                    isNavigating.set(false)
                                                                                }
                                                                            } catch (e: Exception) {
                                                                                Log.e("SCANNER", "Error: ${e.message}")
                                                                                scanError = "Error: ${e.message}"
                                                                                isNavigating.set(false)
                                                                            }
                                                                        }
                                                                    }
                                                                    break
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("SCANNER", "Scan failed: ${e.message}")
                                                    }
                                                    .addOnCompleteListener {
                                                        imageProxy.close()
                                                    }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }

                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                cameraSelector,
                                                preview,
                                                imageAnalysis
                                            )
                                        } catch (e: Exception) {
                                            Log.e("SCANNER", "Camera bind failed: ${e.message}")
                                            e.printStackTrace()
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Red Laser Scanner Line Simulation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(Color.Red)
                                    .align(Alignment.Center)
                            )
                        } else {
                            Text(
                                text = "Akses kamera dibutuhkan untuk scan kartu.",
                                color = Color.White,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Manual Input for testing/fallback
                    Text(
                        text = "Atau masukkan kode manual:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = PinkDark,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = scanInputCode,
                            onValueChange = { scanInputCode = it },
                            label = { Text("Kode Member (cth: MB0001)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PinkMain,
                                focusedLabelColor = PinkMain
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val code = scanInputCode.trim().uppercase()
                                    val member = viewModel.getMemberByCode(code)
                                    if (member != null) {
                                        showScanner = false
                                        onNavigateToDetail(member.id)
                                    } else {
                                        scanError = "Member code not found ($code)!"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PinkMain),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Search", color = Color.White)
                        }
                    }

                    if (scanError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scanError ?: "",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    isNavigating.set(false)
                    showScanner = false
                }) {
                    Text("Cancel", color = PinkDark)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                },
                actions = {
                    IconButton(onClick = { showLogoutConfirm = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = PinkDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Scan Member Card FAB
                FloatingActionButton(
                    onClick = {
                        scanInputCode = ""
                        scanError = null
                        showScanner = true
                    },
                    containerColor = PinkMain,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = QrScannerIcon,
                        contentDescription = "Scan Card",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Unspecified
                    )
                }
                // Add Member FAB
                FloatingActionButton(
                    onClick = onNavigateToRegister,
                    containerColor = PinkMain,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Register Member",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Dashboard Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PinkMain, Color(0xFFFF8FAB))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Total Active Members",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalMembers Customers",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Member List",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PinkDark
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Tier Filter Chips
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tiers = listOf("ALL", "PLATINUM", "GOLD", "SILVER", "BRONZE")
                items(tiers) { tier ->
                    val isSelected = selectedTierFilter == tier
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = { selectedTierFilter = tier },
                        label = { Text(tier, fontWeight = FontWeight.Bold) },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PinkMain,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Member Search Bar with Sort Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = memberSearchQuery,
                    onValueChange = { memberSearchQuery = it },
                    placeholder = { Text("Search Member") },
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
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Members",
                            tint = PinkDark
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Poin Tertinggi") },
                            onClick = {
                                memberSortOption = MemberSortOption.POINTS_DESC
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Poin Terendah") },
                            onClick = {
                                memberSortOption = MemberSortOption.POINTS_ASC
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Nama A-Z") },
                            onClick = {
                                memberSortOption = MemberSortOption.NAME_ASC
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Nama Z-A") },
                            onClick = {
                                memberSortOption = MemberSortOption.NAME_DESC
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ID Terbesar (Terbaru)") },
                            onClick = {
                                memberSortOption = MemberSortOption.ID_DESC
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ID Terkecil (Terlama)") },
                            onClick = {
                                memberSortOption = MemberSortOption.ID_ASC
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (filteredMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (members.isEmpty()) "No members registered yet." else "No matching members found.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                        if (members.isEmpty()) {
                            Text(
                                text = "Tap + to register a new member.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMembers, key = { it.id }) { member ->
                        MemberItemCard(member = member, onClick = { onNavigateToDetail(member.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun MemberItemCard(
    member: MemberEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Member Avatar",
                    tint = PinkDark,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PinkDark
                    )
                )
                Text(
                    text = member.memberCode,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = member.phone,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${member.points} pts",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PinkMain
                    )
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Details",
                    tint = PinkDark.copy(alpha = 0.6f)
                )
            }
        }
    }
}

val QrScannerIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "QrScanner",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Top-left corner
        path(fill = SolidColor(Color.White)) {
            moveTo(4f, 8f)
            lineTo(4f, 5f)
            arcTo(1f, 1f, 0f, false, true, 5f, 4f)
            lineTo(8f, 4f)
            lineTo(8f, 6f)
            lineTo(6f, 6f)
            lineTo(6f, 8f)
            close()
        }
        // Top-right corner
        path(fill = SolidColor(Color.White)) {
            moveTo(20f, 8f)
            lineTo(20f, 5f)
            arcTo(1f, 1f, 0f, false, false, 19f, 4f)
            lineTo(16f, 4f)
            lineTo(16f, 6f)
            lineTo(18f, 6f)
            lineTo(18f, 8f)
            close()
        }
        // Bottom-left corner
        path(fill = SolidColor(Color.White)) {
            moveTo(4f, 16f)
            lineTo(4f, 19f)
            arcTo(1f, 1f, 0f, false, false, 5f, 20f)
            lineTo(8f, 20f)
            lineTo(8f, 18f)
            lineTo(6f, 18f)
            lineTo(6f, 16f)
            close()
        }
        // Bottom-right corner
        path(fill = SolidColor(Color.White)) {
            moveTo(20f, 16f)
            lineTo(20f, 19f)
            arcTo(1f, 1f, 0f, false, true, 19f, 20f)
            lineTo(16f, 20f)
            lineTo(16f, 18f)
            lineTo(18f, 18f)
            lineTo(18f, 16f)
            close()
        }
        // Horizontal red laser scan line in the middle
        path(fill = SolidColor(Color(0xFFFB6F92))) {
            moveTo(5f, 11.5f)
            lineTo(19f, 11.5f)
            lineTo(19f, 12.5f)
            lineTo(5f, 12.5f)
            close()
        }
    }.build()
}
