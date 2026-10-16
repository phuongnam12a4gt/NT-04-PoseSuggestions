package com.ppnnttt.posesuggestions.ui.screens.editor

import com.ppnnttt.posesuggestions.R

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import android.widget.ImageView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableIntStateOf(0) }
    var isRemovingBackground by remember { mutableStateOf(false) }
    val savedMessage = stringResource(R.string.photo_saved)
    val errorMessage = stringResource(R.string.photo_save_failed)

    val savePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { destination ->
        if (destination != null) {
            val source = bitmap ?: return@rememberLauncherForActivityResult
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    saveEditedPhotoToUri(
                        context = context,
                        destination = destination,
                        source = source,
                        brightness = brightness,
                        contrast = contrast,
                        saturation = saturation,
                        rotation = rotation
                    )
                }
                snackbar.showSnackbar(if (saved) savedMessage else errorMessage)
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                bitmap = withContext(Dispatchers.IO) {
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        }
                    }.getOrNull()
                }
                brightness = 0f; contrast = 1f; saturation = 1f; rotation = 0
            }
        }
    }

    val segmenter = remember {
        SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()
        )
    }
    DisposableEffect(segmenter) {
        onDispose { segmenter.close() }
    }

    Scaffold(
        containerColor = Color(0xFF080808),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.photo_editor), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text(stringResource(R.string.back), color = Color.Cyan) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF15171B))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val source = bitmap
                    if (source == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.no_photo_selected), color = Color.White.copy(alpha = 0.65f))
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { picker.launch("image/*") }) {
                                Text(stringResource(R.string.choose_photo))
                            }
                        }
                    } else {
                        AndroidView(
                            factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
                            update = { imageView ->
                                imageView.setImageBitmap(source)
                                imageView.rotation = rotation.toFloat()
                                imageView.colorFilter = ColorMatrixColorFilter(editorColorMatrix(brightness, contrast, saturation))
                            },
                            modifier = Modifier.fillMaxSize().padding(8.dp)
                        )
                    }
                }
            }

            if (bitmap != null) {
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    EditorSlider(stringResource(R.string.brightness), brightness, -100f..100f) { brightness = it }
                    EditorSlider(stringResource(R.string.contrast), contrast, 0.5f..1.5f) { contrast = it }
                    EditorSlider(stringResource(R.string.saturation), saturation, 0f..2f) { saturation = it }
                    Button(
                        onClick = {
                            val source = bitmap ?: return@Button
                            isRemovingBackground = true
                            segmenter.process(InputImage.fromBitmap(source, 0))
                                .addOnSuccessListener { result ->
                                    result.foregroundBitmap?.let { foreground ->
                                        bitmap = foreground.copy(Bitmap.Config.ARGB_8888, false)
                                        brightness = 0f
                                        contrast = 1f
                                        saturation = 1f
                                        rotation = 0
                                    }
                                    isRemovingBackground = false
                                }
                                .addOnFailureListener {
                                    isRemovingBackground = false
                                    scope.launch { snackbar.showSnackbar(errorMessage) }
                                }
                        },
                        enabled = !isRemovingBackground,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        if (isRemovingBackground) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(stringResource(if (isRemovingBackground) R.string.extracting_subject else R.string.remove_background))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { rotation = (rotation + 90) % 360 }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.rotate))
                        }
                        OutlinedButton(onClick = { brightness = 0f; contrast = 1f; saturation = 1f; rotation = 0 }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.reset))
                        }
                        Button(
                            onClick = {
                                val fileName = "Pose_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
                                savePicker.launch(fileName)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.save)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

private fun editorColorMatrix(brightness: Float, contrast: Float, saturation: Float): ColorMatrix {
    val saturationMatrix = ColorMatrix().apply { setSaturation(saturation) }
    val translate = (1f - contrast) * 128f + brightness
    val adjustment = ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, translate,
        0f, contrast, 0f, 0f, translate,
        0f, 0f, contrast, 0f, translate,
        0f, 0f, 0f, 1f, 0f
    ))
    saturationMatrix.postConcat(adjustment)
    return saturationMatrix
}

private fun saveEditedPhotoToUri(
    context: android.content.Context,
    destination: android.net.Uri,
    source: Bitmap,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    rotation: Int
): Boolean = runCatching {
    val filtered = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    Canvas(filtered).drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(editorColorMatrix(brightness, contrast, saturation))
    })
    val output = if (rotation == 0) filtered else Bitmap.createBitmap(
        filtered, 0, 0, filtered.width, filtered.height,
        Matrix().apply { postRotate(rotation.toFloat()) }, true
    )
    val compressed = context.contentResolver.openOutputStream(destination, "w")?.use {
        output.compress(Bitmap.CompressFormat.PNG, 100, it)
    } ?: false
    if (output !== filtered) output.recycle()
    filtered.recycle()
    compressed
}.getOrDefault(false)
