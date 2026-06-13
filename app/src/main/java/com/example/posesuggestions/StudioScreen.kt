package com.example.posesuggestions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    onNavigateBack: () -> Unit
) {
    val isProcessing by viewModel.isProcessing.collectAsState()
    val extractedPose by viewModel.extractedPose.collectAsState()
    
    var poseName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("cool") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.extractPoseFromImage(it) }
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (extractedPose == null) {
                // Initial State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable(enabled = !isProcessing) { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.Cyan)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Upload Image to Extract Pose", color = Color.Gray)
                        }
                    }
                }
            } else {
                // Review & Save State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    SkeletonOverlay(
                        modifier = Modifier.fillMaxSize(),
                        detectedPose = extractedPose
                    )
                }

                Spacer(Modifier.height(24.dp))

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
                        viewModel.saveAsTemplate(poseName, selectedCategory)
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
                    enabled = poseName.isNotBlank()
                ) {
                    Text("Save to My Library", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
