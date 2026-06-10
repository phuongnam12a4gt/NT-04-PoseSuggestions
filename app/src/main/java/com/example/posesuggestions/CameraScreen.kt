package com.example.posesuggestions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.Locale

@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    val detectedPose by viewModel.detectedPose.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTemplate by viewModel.selectedTemplate.collectAsState()
    val currentScore by viewModel.currentScore.collectAsState()
    val countdownValue by viewModel.countdownValue.collectAsState()

    val categories = listOf("cool", "selfie", "travel", "gym")

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
            SkeletonOverlay(
                modifier = Modifier.fillMaxSize(),
                detectedPose = detectedPose,
                templatePose = selectedTemplate
            )

            // Similarity Score
            if (selectedTemplate != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Similarity: ${currentScore.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = when {
                            currentScore > 80f -> Color.Green
                            currentScore > 50f -> Color.Yellow
                            else -> Color.White
                        }
                    )
                    LinearProgressIndicator(
                        progress = { currentScore / 100f },
                        modifier = Modifier
                            .width(200.dp)
                            .padding(top = 8.dp),
                        color = when {
                            currentScore > 80f -> Color.Green
                            currentScore > 50f -> Color.Yellow
                            else -> Color.White
                        },
                        trackColor = Color.Gray.copy(alpha = 0.5f)
                    )
                }
            }

            // Countdown Overlay
            countdownValue?.let { count ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            // UI Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                // Category Selector
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }) }
                        )
                    }
                }

                // Template Selector
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates) { template ->
                        Card(
                            onClick = { viewModel.selectTemplate(template) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTemplate?.id == template.id) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.size(100.dp, 120.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .padding(4.dp)
                                ) {
                                    // Placeholder for preview image
                                    Text("📸", modifier = Modifier.align(Alignment.Center))
                                }
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Button(
                onClick = {
                    launcher.launch(Manifest.permission.CAMERA)
                },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(text = "Request Camera Permission")
            }
        }
    }
}
