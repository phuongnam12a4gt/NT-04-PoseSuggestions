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
            val studioViewModel: StudioViewModel = viewModel()
            val galleryViewModel: GalleryViewModel = viewModel()

            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    MainScreen(
                        onNavigateToCamera = { navController.navigate("camera") },
                        onNavigateToMarketplace = { navController.navigate("camera") },
                        onNavigateToStudio = { navController.navigate("studio") },
                        onNavigateToChallenges = { navController.navigate("camera") },
                        onNavigateToGallery = { navController.navigate("gallery") }
                    )
                }
                composable("camera") {
                    CameraScreen(cameraViewModel)
                }
                composable("studio") {
                    StudioScreen(studioViewModel, onNavigateBack = { navController.popBackStack() })
                }
                composable("gallery") {
                    GalleryScreen(galleryViewModel, onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}
