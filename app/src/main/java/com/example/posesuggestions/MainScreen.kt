package com.example.posesuggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToMarketplace: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onNavigateToChallenges: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFF0F0F0F)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Hello, Creator!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Ready for a perfect shot?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Hero Section: AI Camera Entry
            HeroCard(onClick = onNavigateToCamera)

            Spacer(Modifier.height(32.dp))

            Text(
                "Explore Features",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Features Grid
            val features = listOf(
                FeatureItem("Marketplace", "Find new poses", Icons.Default.ThumbUp, Color(0xFF6200EE), onNavigateToMarketplace),
                FeatureItem("Studio", "Import & Create", Icons.Default.Star, Color(0xFF03DAC5), onNavigateToStudio),
                FeatureItem("Challenges", "Daily Goals", Icons.Default.Star, Color(0xFFFFAB00), onNavigateToChallenges),
                FeatureItem("History", "Your Gallery", Icons.Default.List, Color(0xFFE91E63), {})
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(features) { feature ->
                    FeatureCard(feature)
                }
            }
        }
    }
}

@Composable
fun HeroCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF00F2FE), Color(0xFF4FACFE))
                    )
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    "AI Smart Camera",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Real-time guidance for\nprofessional photos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Start Posing",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp),
                tint = Color.Black.copy(alpha = 0.1f)
            )
        }
    }
}

data class FeatureItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun FeatureCard(item: FeatureItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { item.onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(item.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = item.color)
            }
            Column {
                Text(
                    item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    item.description,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
