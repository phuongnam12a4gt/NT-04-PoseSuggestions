package com.example.posesuggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceBottomSheet(
    viewModel: CameraViewModel,
    onDismiss: () -> Unit
) {
    val templates by viewModel.templates.collectAsState()
    val recentTemplates by viewModel.recentTemplates.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val recordedPoses by viewModel.recordedPoses.collectAsState()

    val categories = listOf("All", "cool", "selfie", "travel", "gym")
    val difficulties = listOf("All", "Easy", "Medium", "Hard")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Pose Marketplace",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search poses...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Filters
            LazyRow(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category) }
                    )
                }
            }

            LazyRow(
                modifier = Modifier.padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(difficulties) { difficulty ->
                    FilterChip(
                        selected = selectedDifficulty == difficulty,
                        onClick = { viewModel.selectDifficulty(difficulty) },
                        label = { Text(difficulty) }
                    )
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // My Recordings
                if (recordedPoses.isNotEmpty()) {
                    item {
                        Text("My Recordings", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                            items(recordedPoses) { recording ->
                                Card(
                                    modifier = Modifier.size(120.dp, 80.dp).clickable { 
                                        viewModel.playRecording(recording, 1000, 1000) // Default dimensions for replay
                                        onDismiss() 
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                                ) {
                                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
                                        Text(recording.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text("${recording.frames.size} frames", color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Recently Used
                if (recentTemplates.isNotEmpty()) {
                    item {
                        Text("Recently Used", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                            items(recentTemplates) { template ->
                                PoseCard(template, favorites.contains(template.id), { viewModel.toggleFavorite(template.id) }, { viewModel.selectTemplate(template); onDismiss() })
                            }
                        }
                    }
                }

                // All Templates
                item {
                    Text("Explore Poses", color = Color.Gray, style = MaterialTheme.typography.labelLarge)
                }
                
                items(templates.chunked(2)) { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        rowItems.forEach { template ->
                            Box(modifier = Modifier.weight(1f)) {
                                PoseCard(template, favorites.contains(template.id), { viewModel.toggleFavorite(template.id) }, { viewModel.selectTemplate(template); onDismiss() })
                            }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PoseCard(
    template: PoseTemplate,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📸", fontSize = 40.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(template.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(template.difficulty, color = Color.Cyan, fontSize = 10.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(template.category, color = Color.Gray, fontSize = 10.sp)
                }
            }

            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else Color.White
                )
            }
        }
    }
}
