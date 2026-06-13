package com.example.posesuggestions

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreview(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()

    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { previewView ->
            // Reading isFrontCamera here tells Compose to re-run this block when it changes
            isFrontCamera.let { 
                viewModel.bindCamera(lifecycleOwner, previewView)
            }
        }
    )
    
    // Trigger update when camera toggles
    LaunchedEffect(isFrontCamera) {
        // The update block of AndroidView will be called automatically if bindCamera is reactive
    }
}
