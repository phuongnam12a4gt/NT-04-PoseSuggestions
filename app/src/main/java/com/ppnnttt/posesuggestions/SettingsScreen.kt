package com.ppnnttt.posesuggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class LanguageOption(val tag: String, val nativeName: String, val flag: String)

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf(AppLanguageManager.getLanguage(context)) }
    val languages = listOf(
        LanguageOption("en", "English", "EN"),
        LanguageOption("vi", "Tiếng Việt", "VI"),
        LanguageOption("th", "ไทย", "TH"),
        LanguageOption("ko", "한국어", "KO"),
        LanguageOption("ja", "日本語", "JA")
    )

    Scaffold(
        containerColor = Color(0xFF080808),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNavigateBack) {
                    Text(stringResource(R.string.back), color = Color.Cyan)
                }
                Text(
                    stringResource(R.string.settings),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)
        ) {
            Text(
                stringResource(R.string.language),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.language_description),
                color = Color.White.copy(alpha = 0.58f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            languages.forEach { language ->
                val selected = selectedLanguage == language.tag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) Color.Cyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
                        .clickable {
                            selectedLanguage = language.tag
                            AppLanguageManager.setLanguage(context, language.tag)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape)
                            .background(if (selected) Color.Cyan else Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(language.flag, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Black)
                    }
                    Text(
                        language.nativeName,
                        color = Color.White,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f).padding(horizontal = 14.dp)
                    )
                    RadioButton(
                        selected = selected,
                        onClick = {
                            selectedLanguage = language.tag
                            AppLanguageManager.setLanguage(context, language.tag)
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = Color.Cyan)
                    )
                }
            }
        }
    }
}
