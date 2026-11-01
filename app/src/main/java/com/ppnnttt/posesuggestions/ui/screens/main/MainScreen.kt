package com.ppnnttt.posesuggestions.ui.screens.main

import com.ppnnttt.posesuggestions.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
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
    onNavigateToVideoEditor: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(24.dp))
            
            // App Identity
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))),
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

            Spacer(Modifier.height(20.dp))

            // Hero Section
            MainActionCard(
                title = stringResource(R.string.smart_photo_assistant),
                subtitle = stringResource(R.string.smart_photo_description),
                icon = com.ppnnttt.posesuggestions.ui.AppIcons.Camera,
                gradient = listOf(Color(0xFF204A47), Color(0xFF182E30)),
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
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToMarketplace
                    )
                    SmallFeatureCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.history),
                        icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.List,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToGallery
                    )
                }
                
                SmallFeatureCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.photo_editor),
                    icon = Icons.Default.Edit,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToPhotoEditor
                )

                SmallFeatureCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.video_editor),
                    icon = Icons.Default.PlayArrow,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToVideoEditor
                )
            }

            Spacer(Modifier.height(20.dp))
            
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .heightIn(min = 144.dp)
            .background(Brush.linearGradient(gradient), RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp).align(Alignment.CenterStart).fillMaxWidth(0.88f)) {
                Surface(color = Color.White.copy(alpha = 0.2f), shape = CircleShape) {
                    Text(stringResource(R.string.most_popular), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = onClick) { Text(stringResource(R.string.capture_photo)) }
            }
            Icon(icon, contentDescription = null, modifier = Modifier.size(120.dp).align(Alignment.CenterEnd).offset(x = 30.dp), tint = Color.White.copy(alpha = 0.1f))
        }
    }
}

@Composable
fun SecondaryActionCard(title: String, description: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun SmallFeatureCard(modifier: Modifier, title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = modifier.heightIn(min = 72.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
    }
}
