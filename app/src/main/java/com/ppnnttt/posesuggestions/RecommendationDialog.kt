package com.ppnnttt.posesuggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun RecommendationDialog(
    onDismiss: () -> Unit,
    onRecommend: (RecommendationInput) -> Unit
) {
    var location by remember { mutableStateOf("Outdoor") }
    var outfit by remember { mutableStateOf("Casual") }
    var gender by remember { mutableStateOf("Male") }
    var mood by remember { mutableStateOf("Confident") }

    val locations = listOf("Outdoor", "Indoor", "Nature", "Gym", "Street")
    val outfits = listOf("Casual", "Active", "Formal", "Streetwear")
    val genders = listOf("Male", "Female", "Non-binary")
    val moods = listOf("Confident", "Relaxed", "Energetic", "Serene", "Cool")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "AI Pose Suggestion",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                SelectionRow("Location", locations, location) { location = it }
                SelectionRow("Outfit", outfits, outfit) { outfit = it }
                SelectionRow("Gender", genders, gender) { gender = it }
                SelectionRow("Mood", moods, mood) { mood = it }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        onRecommend(RecommendationInput(location, outfit, gender, mood))
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan)
                ) {
                    Text("Generate Recommendation", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SelectionRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        LazyRow(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(options) { option ->
                val isSelected = selected == option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.Cyan else Color.White.copy(alpha = 0.05f))
                        .clickable { onSelect(option) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        option,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
