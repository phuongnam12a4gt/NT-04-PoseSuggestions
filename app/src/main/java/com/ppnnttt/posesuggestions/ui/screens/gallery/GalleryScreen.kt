package com.ppnnttt.posesuggestions.ui.screens.gallery

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.ppnnttt.posesuggestions.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(viewModel: GalleryViewModel, onNavigateBack: () -> Unit,
    onTakePhoto: () -> Unit = {}, onEditPhoto: (File) -> Unit = {}) {
    val images by viewModel.capturedImages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val deleteFailed by viewModel.deleteFailed.collectAsState()
    var selected by remember { mutableStateOf<File?>(null) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val error = stringResource(R.string.photo_save_failed)
    LaunchedEffect(Unit) { viewModel.loadImages() }
    LaunchedEffect(deleteFailed) { if (deleteFailed) { snackbar.showSnackbar(error); viewModel.clearError() } }
    Scaffold(containerColor = Color(0xFF0F0F0F), snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        TopAppBar(title = { Text(stringResource(R.string.captured_poses), color = Color.White) },
            navigationIcon = { IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = Color.White)
            } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F0F)))
    }) { padding ->
        if (loading) Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
        else if (images.isEmpty()) Column(Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.no_captured_poses), color = Color.White)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onTakePhoto) { Text(stringResource(R.string.capture_photo)) }
        } else LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding)) {
            items(images, key = { it.absolutePath }) { file ->
                Card(Modifier.fillMaxWidth().aspectRatio(.75f).clickable { selected = file }, shape = RoundedCornerShape(16.dp)) {
                    AsyncImage(file, stringResource(R.string.open_photo), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
    }
    selected?.let { file ->
        Dialog(onDismissRequest = { selected = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
                TextButton(onClick = { selected = null }) { Text(stringResource(R.string.close), color = Color.White) }
                AsyncImage(file, stringResource(R.string.open_photo), Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Fit)
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = context.contentResolver.getType(uri) ?: "image/jpeg"; putExtra(Intent.EXTRA_STREAM, uri)
                            clipData = ClipData.newRawUri("photo", uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_photo)))
                    }) { Text(stringResource(R.string.share_photo), color = Color.Cyan) }
                    TextButton(onClick = { selected = null; onEditPhoto(file) }) { Text(stringResource(R.string.photo_editor), color = Color.White) }
                    TextButton(onClick = { pendingDelete = file }) { Text(stringResource(R.string.delete), color = Color(0xFFFF8A80)) }
                }
            }
        }
    }
    pendingDelete?.let { file ->
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text(stringResource(R.string.delete_photo_title)) },
            text = { Text(stringResource(R.string.delete_photo_message)) },
            confirmButton = { TextButton(onClick = { viewModel.deleteImage(file); pendingDelete = null; selected = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) } })
    }
}
