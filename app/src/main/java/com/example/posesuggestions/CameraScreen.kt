package com.example.posesuggestions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.util.Locale

@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    val detectedPose by viewModel.detectedPose.collectAsState()
    val detectedPosePartner by viewModel.detectedPosePartner.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val currentScore by viewModel.currentScore.collectAsState()
    val guidanceMessage by viewModel.guidanceMessage.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()
    val ghostOpacity by viewModel.ghostOpacity.collectAsState()
    val challengeState by viewModel.challengeState.collectAsState()
    val challengeTimeLeft by viewModel.challengeTimeLeft.collectAsState()
    val recommendationText by viewModel.recommendationText.collectAsState()
    
    val isRecordingPose by viewModel.isRecordingPose.collectAsState()
    val replayFrame by viewModel.replayFrame.collectAsState()
    val isReplaying by viewModel.isReplaying.collectAsState()
    val lastCapturedPhoto by viewModel.lastCapturedPhoto.collectAsState()

    var showMarketplace by remember { mutableStateOf(false) }
    var showRecommendationDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            
            // Composition Grid
            CompositionOverlay(modifier = Modifier.fillMaxSize())

            SkeletonOverlay(
                modifier = Modifier.fillMaxSize(),
                detectedPose = detectedPose,
                detectedPosePartner = detectedPosePartner,
                templatePose = selectedTemplate,
                replayPose = replayFrame,
                currentScore = currentScore
            )

            // Heatmap Error Visualization
            PoseHeatmapOverlay(
                detectedPose = detectedPose,
                template = selectedTemplate,
                errorAnalysisEngine = viewModel.errorAnalysisEngine,
                modifier = Modifier.fillMaxSize()
            )

            // Ghost Image Overlay (Scale & Drag)
            GhostOverlay(
                template = selectedTemplate,
                opacity = ghostOpacity,
                modifier = Modifier.fillMaxSize()
            )

            // Top HUD: Similarity Score
            PremiumTopHUD(currentScore, selectedTemplate != null)

            // Replay HUD
            if (isReplaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Red.copy(alpha = 0.6f))
                        .clickable { viewModel.stopReplay() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("STOP REPLAY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Challenge UI
            if (challengeState != ChallengeState.IDLE) {
                ChallengeOverlay(
                    state = challengeState, 
                    timeLeft = challengeTimeLeft, 
                    score = currentScore,
                    highScore = selectedTemplate?.let { viewModel.getHighScore(it.id) } ?: 0
                )
            }

            // Guidance Message
            AnimatedVisibility(
                visible = guidanceMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = guidanceMessage ?: "",
                        color = Color.Cyan,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }

            // Opacity Slider (Right side)
            if (selectedTemplate != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .width(40.dp)
                        .height(200.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Slider(
                        value = ghostOpacity,
                        onValueChange = { viewModel.setGhostOpacity(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .graphicsLayer {
                                rotationZ = -90f
                                translationX = 0f
                            }
                            .width(160.dp)
                    )
                }
            }

            // Center Countdown
            PremiumCountdown(countdownValue)

            // Bottom Controls Panel
            PremiumBottomControls(
                modifier = Modifier.align(Alignment.BottomCenter),
                templates = templates,
                selectedTemplate = selectedTemplate,
                selectedCategory = selectedCategory,
                onCategorySelect = { viewModel.selectCategory(it) },
                onTemplateSelect = { viewModel.selectTemplate(it) },
                onExploreClick = { showMarketplace = true },
                onChallengeClick = { viewModel.startRandomChallenge() },
                onRecommendClick = { showRecommendationDialog = true },
                isRecording = isRecordingPose,
                onRecordToggle = {
                    if (isRecordingPose) {
                        viewModel.stopRecording("Pose_${System.currentTimeMillis()}")
                    } else {
                        viewModel.startRecording()
                    }
                },
                onFlipCamera = { viewModel.toggleCamera() },
                lastCapturedPhoto = lastCapturedPhoto,
                selectedDifficulty = viewModel.challengeDifficulty.collectAsState().value,
                onDifficultySelect = { viewModel.setChallengeDifficulty(it) }
            )

            if (showMarketplace) {
                MarketplaceBottomSheet(
                    viewModel = viewModel,
                    onDismiss = { showMarketplace = false }
                )
            }

            if (showRecommendationDialog) {
                RecommendationDialog(
                    onDismiss = { showRecommendationDialog = false },
                    onRecommend = { viewModel.getRecommendations(it) }
                )
            }

            // AI Recommendation Tooltip
            AnimatedVisibility(
                visible = recommendationText != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 280.dp).padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Cyan.copy(alpha = 0.9f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = recommendationText ?: "",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
        } else {
            PermissionRequestUI { launcher.launch(Manifest.permission.CAMERA) }
        }
    }
}

@Composable
fun PremiumTopHUD(score: Float, isTemplateSelected: Boolean) {
    AnimatedVisibility(
        visible = isTemplateSelected,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = if (score > 80f) Color.Cyan else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Match: ${score.toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Minimalist Progress Bar
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(score / 100f)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Cyan, Color(0xFFE91E63))
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun PremiumCountdown(count: Int?) {
    AnimatedVisibility(
        visible = count != null,
        enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count?.toString() ?: "",
                fontSize = 120.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                style = MaterialTheme.typography.displayLarge.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        blurRadius = 20f
                    )
                )
            )
        }
    }
}

@Composable
fun PremiumBottomControls(
    modifier: Modifier = Modifier,
    templates: List<PoseTemplate>,
    selectedTemplate: PoseTemplate?,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    onTemplateSelect: (PoseTemplate) -> Unit,
    onExploreClick: () -> Unit,
    onChallengeClick: () -> Unit,
    onRecommendClick: () -> Unit,
    isRecording: Boolean,
    onRecordToggle: () -> Unit,
    onFlipCamera: () -> Unit,
    lastCapturedPhoto: java.io.File?,
    selectedDifficulty: String,
    onDifficultySelect: (String) -> Unit
) {
    val categories = listOf("All", "cool", "selfie", "travel", "gym")
    val difficulties = listOf("Easy", "Medium", "Hard")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // AI & Marketplace Actions
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Marketplace Entry
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { onExploreClick() }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Market", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // AI Recommendation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Cyan.copy(alpha = 0.2f))
                    .clickable { onRecommendClick() }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Suggest", color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Challenge Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Yellow.copy(alpha = 0.2f))
                    .clickable { onChallengeClick() }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Challenge", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Difficulty Selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            difficulties.forEach { diff ->
                val isSelected = selectedDifficulty == diff
                Text(
                    text = diff,
                    color = if (isSelected) Color.Yellow else Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .clickable { onDifficultySelect(diff) }
                )
            }
        }
        // Glassmorphism Template Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                .padding(vertical = 12.dp)
        ) {
            Column {
                // Category Tabs (TikTok Style)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Text(
                            text = category.uppercase(Locale.ROOT),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .clickable { onCategorySelect(category) }
                        )
                    }
                }

                // Horizontal Template List
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(templates) { template ->
                        PremiumTemplateItem(
                            template = template,
                            isSelected = selectedTemplate?.id == template.id,
                            onClick = { onTemplateSelect(template) }
                        )
                    }
                }
            }
        }

        // Shutter & Actions Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (lastCapturedPhoto != null) {
                    AsyncImage(
                        model = lastCapturedPhoto,
                        contentDescription = "Last captured",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                }
            }

            // Main Premium Shutter Button
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                // Shutter ring
            }

            // Flip Camera Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onFlipCamera() },
                contentAlignment = Alignment.Center
            ) {
                Text("🔄", fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun PremiumTemplateItem(
    template: PoseTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                .border(
                    width = if (isSelected) 2.dp else 0.dp,
                    color = if (isSelected) Color.Cyan else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("📸", fontSize = 32.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = template.name,
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ChallengeOverlay(state: ChallengeState, timeLeft: Int, score: Float, highScore: Int = 0) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (state == ChallengeState.COUNTDOWN) Color.Black.copy(alpha = 0.5f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            ChallengeState.COUNTDOWN -> {
                Text(
                    text = "GET READY!\n$timeLeft",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Yellow,
                    modifier = Modifier.padding(32.dp)
                )
            }
            ChallengeState.ACTIVE -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 100.dp, end = 24.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "TIME: $timeLeft",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (timeLeft <= 3) Color.Red else Color.White
                    )
                    Text(
                        text = "SCORE: ${score.toInt()}%",
                        fontSize = 20.sp,
                        color = Color.Cyan
                    )
                    if (highScore > 0) {
                        Text(
                            text = "BEST: $highScore%",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            ChallengeState.FINISHED -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(48.dp)
                        .border(2.dp, Color.Cyan, RoundedCornerShape(32.dp))
                        .padding(24.dp)
                ) {
                    Text(
                        text = if (score >= 80f) "POSE MASTER!" else "WELL DONE!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (score >= 80f) Color.Cyan else Color.White
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Your Accuracy",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${score.toInt()}%",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Yellow
                    )
                    Spacer(Modifier.height(24.dp))
                    LinearProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color.Cyan,
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
fun PermissionRequestUI(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Camera access is needed", color = Color.White)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
            ) {
                Text("Grant Permission", color = Color.Black)
            }
        }
    }
}
