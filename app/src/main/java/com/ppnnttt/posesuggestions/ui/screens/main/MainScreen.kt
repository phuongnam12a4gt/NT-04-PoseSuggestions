package com.ppnnttt.posesuggestions.ui.screens.main

import com.ppnnttt.posesuggestions.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToMarketplace: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onNavigateToPhotoEditor: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF080808)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            
            // App Identity
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color.Cyan, Color.Magenta))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "POSE AI",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Hero Section
            MainActionCard(
                title = stringResource(R.string.smart_photo_assistant),
                subtitle = stringResource(R.string.smart_photo_description),
                icon = Icons.Default.Add,
                gradient = listOf(Color(0xFF6200EE), Color(0xFFB00020)),
                onClick = onNavigateToCamera
            )

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.create_more),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Features Grid
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SecondaryActionCard(
                    title = stringResource(R.string.pose_studio),
                    description = stringResource(R.string.pose_studio_description),
                    icon = Icons.Default.Add,
                    color = Color(0xFF03DAC5),
                    onClick = onNavigateToStudio
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SmallFeatureCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.explore),
                        icon = Icons.Default.Search,
                        color = Color(0xFFBB86FC),
                        onClick = onNavigateToMarketplace
                    )
                    SmallFeatureCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.history),
                        icon = Icons.Default.List,
                        color = Color(0xFFE91E63),
                        onClick = onNavigateToGallery
                    )
                }
                
                SmallFeatureCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.photo_editor),
                    icon = Icons.Default.Edit,
                    color = Color(0xFFFFAB00),
                    onClick = onNavigateToPhotoEditor
                )
            }
            
            Spacer(Modifier.weight(1f))
            
            // Quick Tip
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Yellow)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.zoom_tip),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MainActionCard(title: String, subtitle: String, icon: ImageVector, gradient: List<Color>, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(gradient))) {
            Column(modifier = Modifier.padding(24.dp).align(Alignment.CenterStart).fillMaxWidth(0.7f)) {
                Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape) {
                    Text(stringResource(R.string.most_popular), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(120.dp).align(Alignment.CenterEnd).offset(x = 30.dp), tint = Color.White.copy(alpha = 0.1f))
        }
    }
}

@Composable
fun SecondaryActionCard(title: String, description: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(description, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun SmallFeatureCard(modifier: Modifier, title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(80.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
