package com.ppnnttt.posesuggestions.ui.screens.camera

import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun CameraPreview(viewModel: CameraViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val front by viewModel.isFrontCamera.collectAsState()
    val preview = remember(context) { PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FILL_CENTER
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    } }
    AndroidView(factory = { preview }, modifier = modifier)
    DisposableEffect(owner, front, preview) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.bindCamera(owner, preview)
            if (event == Lifecycle.Event.ON_STOP) viewModel.releaseCamera()
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) viewModel.bindCamera(owner, preview)
        onDispose { owner.lifecycle.removeObserver(observer); viewModel.releaseCamera() }
    }
}
