package com.example.posesuggestions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val cameraViewModel: CameraViewModel = viewModel()

            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    MainScreen(
                        onNavigateToCamera = { navController.navigate("camera") },
                        onNavigateToMarketplace = { navController.navigate("camera") }, // Open camera then marketplace for now
                        onNavigateToStudio = { /* TODO */ },
                        onNavigateToChallenges = { navController.navigate("camera") }
                    )
                }
                composable("camera") {
                    CameraScreen(cameraViewModel)
                }
            }
        }
    }
}
