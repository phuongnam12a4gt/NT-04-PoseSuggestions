package com.ppnnttt.posesuggestions

import com.ppnnttt.posesuggestions.ui.screens.camera.CameraScreen
import com.ppnnttt.posesuggestions.ui.screens.camera.CameraViewModel
import com.ppnnttt.posesuggestions.ui.screens.editor.PhotoEditorScreen
import com.ppnnttt.posesuggestions.ui.screens.editor.VideoEditorScreen
import com.ppnnttt.posesuggestions.ui.screens.gallery.GalleryScreen
import com.ppnnttt.posesuggestions.ui.screens.gallery.GalleryViewModel
import com.ppnnttt.posesuggestions.ui.screens.main.MainScreen
import com.ppnnttt.posesuggestions.ui.screens.settings.SettingsScreen
import com.ppnnttt.posesuggestions.ui.screens.studio.StudioScreen
import com.ppnnttt.posesuggestions.ui.screens.studio.StudioViewModel

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
        delegate.localNightMode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        AppLanguageManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK)
        )
        setContent {
            androidx.compose.material3.MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(
                primary = androidx.compose.ui.graphics.Color.Cyan,
                onPrimary = androidx.compose.ui.graphics.Color.Black,
                surface = androidx.compose.ui.graphics.Color(0xFF17171F),
                background = androidx.compose.ui.graphics.Color(0xFF080808)
            )) {
            val navController = rememberNavController()
            val cameraViewModel: CameraViewModel = viewModel()
            val studioViewModel: StudioViewModel = viewModel()
            val galleryViewModel: GalleryViewModel = viewModel()

            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    MainScreen(
                        onNavigateToCamera = { navController.navigate("camera") },
                        onNavigateToMarketplace = { navController.navigate("explore") },
                        onNavigateToStudio = { navController.navigate("studio") },
                        onNavigateToPhotoEditor = { navController.navigate("photo_editor") },
                        onNavigateToVideoEditor = { navController.navigate("video_editor") },
                        onNavigateToGallery = { navController.navigate("gallery") },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable("camera") {
                    CameraScreen(
                        viewModel = cameraViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenGallery = { navController.navigate("gallery") }
                    )
                }
                composable("explore") {
                    CameraScreen(cameraViewModel, onNavigateBack = { navController.popBackStack() },
                        onOpenGallery = { navController.navigate("gallery") }, initialShowMarketplace = true)
                }
                composable("studio") {
                    StudioScreen(studioViewModel, onNavigateBack = { navController.popBackStack() })
                }
                composable("gallery") {
                    GalleryScreen(galleryViewModel, onNavigateBack = { navController.popBackStack() },
                        onTakePhoto = { navController.navigate("camera") },
                        onEditPhoto = { file ->
                            navController.currentBackStackEntry?.savedStateHandle?.set("edit_photo", file.absolutePath)
                            navController.navigate("photo_editor")
                        })
                }
                composable("settings") {
                    SettingsScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("photo_editor") {
                    PhotoEditorScreen(onNavigateBack = { navController.popBackStack() },
                        initialPhoto = navController.previousBackStackEntry?.savedStateHandle?.get<String>("edit_photo"))
                }
                composable("video_editor") {
                    VideoEditorScreen(onNavigateBack = { navController.popBackStack() })
                }
            }
            }
        }
    }
}
