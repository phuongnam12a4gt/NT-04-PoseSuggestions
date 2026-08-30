package com.ppnnttt.posesuggestions

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguageManager.applySavedLanguage(this)
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
                        onNavigateToGallery = { navController.navigate("gallery") },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable("camera") {
                    CameraScreen(
                        viewModel = cameraViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("studio") {
                    StudioScreen(studioViewModel, onNavigateBack = { navController.popBackStack() })
                }
                composable("gallery") {
                    GalleryScreen(galleryViewModel, onNavigateBack = { navController.popBackStack() })
                }
                composable("settings") {
                    SettingsScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
        }
    }
}
