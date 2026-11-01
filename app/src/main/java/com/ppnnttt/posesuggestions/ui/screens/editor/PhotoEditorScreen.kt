package com.ppnnttt.posesuggestions.ui.screens.editor

import androidx.core.graphics.createBitmap
import androidx.core.graphics.withTranslation
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ppnnttt.posesuggestions.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(
    onNavigateBack: () -> Unit,
    initialPhoto: String? = null,
    model: PhotoEditorViewModel = viewModel()
) {
    val resources = androidx.compose.ui.platform.LocalResources.current
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var discard by rememberSaveable { mutableStateOf(false) }
    var selectedTool by rememberSaveable { mutableIntStateOf(0) }
    val edits = model.edits
    LaunchedEffect(initialPhoto) { model.initialize(initialPhoto) }
    LaunchedEffect(model.message, resources) {
        model.message?.let {
            snackbar.showSnackbar(resources.getString(it))
            model.clearMessage()
        }
    }
    fun back() { if (!model.busy) { if (model.dirty) discard = true else onNavigateBack() } }
    BackHandler(enabled = model.dirty || model.busy) { back() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(model::importPhoto)
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png"), model::export)
    if (discard) AlertDialog(
        onDismissRequest = { discard = false },
        title = { Text(stringResource(R.string.discard_edits)) },
        text = { Text(stringResource(R.string.discard_message)) },
        confirmButton = { TextButton(onClick = { discard = false; onNavigateBack() }) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(stringResource(R.string.cancel)) } }
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.photo_editor)) },
                navigationIcon = { IconButton(enabled = !model.busy, onClick = ::back) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                } },
                actions = {
                    Button(enabled = model.original != null && !model.busy, onClick = {
                        if (model.beginExport()) {
                            try { exporter.launch("Pose_${System.currentTimeMillis()}.png") }
                            catch (e: Exception) { model.export(null) }
                        }
                    }, modifier = Modifier.padding(end = 12.dp)) { Text(stringResource(R.string.save)) }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        val toolsHeight = minOf(330.dp, maxHeight * 0.5f)
        Column(Modifier.fillMaxSize()) {
            Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF15171B))) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val source = model.source
                    if (source == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.no_photo_selected))
                            Button(enabled = !model.busy, onClick = { picker.launch("image/*") }) { Text(stringResource(R.string.choose_photo)) }
                        }
                    } else AndroidView(
                        factory = { PhotoPreview(it) },
                        update = { it.source = source; it.edits = edits; it.invalidate() },
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                    if (model.busy) CircularProgressIndicator()
                }
            }
            if (model.original != null) {
                Column(Modifier.fillMaxWidth().heightIn(max = toolsHeight).verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selectedTool == 1, { selectedTool = if (selectedTool == 1) 0 else 1 }, label = { Text(stringResource(R.string.brightness)) }) }
                        item { FilterChip(selectedTool == 2, { selectedTool = if (selectedTool == 2) 0 else 2 }, label = { Text(stringResource(R.string.contrast)) }) }
                        item { FilterChip(selectedTool == 3, { selectedTool = if (selectedTool == 3) 0 else 3 }, label = { Text(stringResource(R.string.saturation)) }) }
                        item { FilterChip(selectedTool == 4, { selectedTool = if (selectedTool == 4) 0 else 4 }, label = { Text(stringResource(R.string.remove_background)) }) }
                    }
                    when (selectedTool) {
                        1 -> EditorSlider(stringResource(R.string.brightness), edits.brightness, -100f..100f, !model.busy) { model.update(edits.copy(brightness = it)) }
                        2 -> EditorSlider(stringResource(R.string.contrast), edits.contrast, 0.5f..1.5f, !model.busy) { model.update(edits.copy(contrast = it)) }
                        3 -> EditorSlider(stringResource(R.string.saturation), edits.saturation, 0f..2f, !model.busy) { model.update(edits.copy(saturation = it)) }
                        4 -> {
                            val backgroundLabel = stringResource(R.string.remove_background)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(backgroundLabel, Modifier.weight(1f))
                                Switch(checked = edits.removeBackground, onCheckedChange = { model.toggleBackground() }, enabled = !model.busy,
                                    modifier = Modifier.semantics { contentDescription = backgroundLabel })
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(enabled = !model.busy, onClick = { model.update(edits.copy(rotation = (edits.rotation + 90) % 360)) }) { Text(stringResource(R.string.rotate)) }
                        TextButton(enabled = !model.busy, onClick = model::reset) { Text(stringResource(R.string.reset)) }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun EditorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(String.format(androidx.compose.ui.platform.LocalLocale.current.platformLocale, "%.2f", value), style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, enabled = enabled, modifier = Modifier.semantics { contentDescription = label })
    }
}

private class PhotoPreview(context: Context) : View(context) {
    var source: Bitmap? = null
    var edits = PhotoEdits()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = source ?: return
        val (outputWidth, outputHeight) = rotatedPhotoSize(bitmap, edits.rotation)
        val scale = minOf(width.toFloat() / outputWidth, height.toFloat() / outputHeight)
        canvas.withTranslation((width - outputWidth * scale) / 2f, (height - outputHeight * scale) / 2f) {
            scale(scale, scale)
            drawEditedPhoto(this, bitmap, edits)
        }
    }
}

internal fun rotatedPhotoSize(source: Bitmap, rotation: Int): Pair<Int, Int> =
    if (rotation % 180 == 0) source.width to source.height else source.height to source.width

private fun editorColorMatrix(edits: PhotoEdits): ColorMatrix {
    val translate = (1f - edits.contrast) * 128f + edits.brightness
    return ColorMatrix().apply {
        setSaturation(edits.saturation)
        postConcat(ColorMatrix(floatArrayOf(
            edits.contrast, 0f, 0f, 0f, translate,
            0f, edits.contrast, 0f, 0f, translate,
            0f, 0f, edits.contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )))
    }
}

private fun drawEditedPhoto(canvas: Canvas, source: Bitmap, edits: PhotoEdits) {
    val (width, height) = rotatedPhotoSize(source, edits.rotation)
    val matrix = Matrix().apply {
        postTranslate(-source.width / 2f, -source.height / 2f)
        postRotate(edits.rotation.toFloat())
        postTranslate(width / 2f, height / 2f)
    }
    canvas.drawBitmap(source, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(editorColorMatrix(edits))
    })
}

internal fun renderEditedPhoto(source: Bitmap, edits: PhotoEdits): Bitmap {
    val (width, height) = rotatedPhotoSize(source, edits.rotation)
    val output = createBitmap(width, height)
    try { drawEditedPhoto(Canvas(output), source, edits) }
    catch (e: Exception) { output.recycle(); throw e }
    return output
}

internal fun saveEditedPhotoToUri(
    context: Context, destination: Uri, source: Bitmap,
    brightness: Float, contrast: Float, saturation: Float, rotation: Int
): Boolean = runCatching {
    val output = renderEditedPhoto(source, PhotoEdits(brightness, contrast, saturation, rotation))
    try {
        context.contentResolver.openOutputStream(destination, "w")?.use {
            output.compress(Bitmap.CompressFormat.PNG, 100, it)
        } ?: false
    } finally { output.recycle() }
}.getOrDefault(false)
