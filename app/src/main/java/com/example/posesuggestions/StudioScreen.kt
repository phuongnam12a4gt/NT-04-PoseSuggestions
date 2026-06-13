package com.example.posesuggestions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.camera.view.PreviewView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val isProcessing by viewModel.isProcessing.collectAsState()
    val extractedPose by viewModel.extractedPose.collectAsState()
    val isCameraActive by viewModel.isCameraActive.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()
    
    var poseName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("cool") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.extractPoseFromImage(it) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.setCameraActive(true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pose Studio", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F0F))
            )
        },
        containerColor = Color(0xFF0F0F0F)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(if (isCameraActive) 0.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (extractedPose == null) {
                if (isCameraActive) {
                    // Camera Mode
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    viewModel.bindCamera(lifecycleOwner, this)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Close Camera Button
                        IconButton(
                            onClick = { viewModel.setCameraActive(false) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        ) {
                            Text("✕", color = Color.White, fontSize = 24.sp)
                        }

                        // Countdown Overlay
                        PremiumCountdown(countdownValue)

                        // Capture Button
                        if (countdownValue == null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 48.dp)
                                    .size(80.dp)
                                    .border(4.dp, Color.White, CircleShape)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .clickable { viewModel.startPoseCapture() }
                            )
                        }
                    }
                } else {
                    // Selection Menu
                    Text(
                        "CHOOSE A METHOD",
                        color = Color.Cyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Upload Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable(enabled = !isProcessing) { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(color = Color.Cyan)
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text("Upload Image", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        }

                        // 2. Pose Studio Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(200.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.Cyan.copy(alpha = 0.1f))
                                .border(1.dp, Color.Cyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                .clickable {
                                    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        viewModel.setCameraActive(true)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📸", fontSize = 32.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("Pose Studio", color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("(3s Timer)", color = Color.Cyan.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            } else {
                // Review & Save State (Sử dụng GhostOverlay để xem thử tính năng kéo/zoom)
                Text(
                    "CHẾ ĐỘ XEM THỬ (THỬ KÉO & ZOOM)",
                    color = Color.Cyan.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Hiển thị ảnh gốc đã chụp/upload phía dưới
                    val bitmap by viewModel.sourceBitmap.collectAsState()
                    bitmap?.let {
                        androidx.compose.foundation.Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }

                    // Hiển thị khung xương đã trích xuất
                    SkeletonOverlay(
                        modifier = Modifier.fillMaxSize(),
                        detectedPose = extractedPose,
                        currentScore = 100f // Đã trích xuất xong nên coi như khớp 100%
                    )

                    // Tạo một Template tạm thời với thuật toán chuẩn hóa mới
                    val tempTemplate = PoseTemplate(
                        id = "preview",
                        name = poseName,
                        category = selectedCategory,
                        difficulty = "Easy",
                        previewImage = "",
                        landmarks = normalizeLandmarks(extractedPose!!.landmarks, extractedPose!!.imageWidth, extractedPose!!.imageHeight)
                    )

                    // GhostOverlay để xem thử template sau khi chuẩn hóa (mờ hơn để thấy skeleton bên dưới)
                    GhostOverlay(
                        template = tempTemplate,
                        opacity = 0.3f,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = poseName,
                    onValueChange = { poseName = it },
                    label = { Text("Pose Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    )
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { 
                        viewModel.saveAsTemplate(
                            if (poseName.isBlank()) "Pose_${System.currentTimeMillis()}" else poseName, 
                            selectedCategory
                        )
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
                    enabled = true // Cho phép lưu ngay cả khi chưa đặt tên (sẽ lấy tên mặc định)
                ) {
                    Text(
                        if (poseName.isBlank()) "Quick Save to Library" else "Save as \"$poseName\"", 
                        color = Color.Black, 
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
