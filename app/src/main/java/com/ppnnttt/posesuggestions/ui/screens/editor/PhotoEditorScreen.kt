package com.ppnnttt.posesuggestions.ui.screens.editor

import android.graphics.*
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.ppnnttt.posesuggestions.R
import com.ppnnttt.posesuggestions.loadPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class Tool(val label: Int) { FILTER(R.string.filters), ADJUST(R.string.adjust), CROP(R.string.crop), AI(R.string.remove_background) }
internal enum class Preset(val b: Float, val c: Float, val s: Float, val w: Float) {
    ORIGINAL(0f, 1f, 1f, 0f), POP(4f, 1.18f, 1.25f, 2f), WARM(5f, 1.05f, 1.08f, 24f),
    COOL(2f, 1.08f, .95f, -24f), MONO(4f, 1.14f, 0f, 0f), FADE(12f, .86f, .82f, 6f)
}
internal enum class Ratio(val title: String, val value: Float?) { ORIGINAL("", null), SQUARE("1:1", 1f), PORTRAIT("4:5", .8f), STORY("9:16", .5625f), WIDE("16:9", 16f/9f) }
internal data class Edit(val brightness: Float = 0f, val contrast: Float = 1f, val saturation: Float = 1f,
    val rotation: Int = 0, val flipH: Boolean = false, val flipV: Boolean = false,
    val preset: Preset = Preset.ORIGINAL, val ratio: Ratio = Ratio.ORIGINAL)
private data class Snapshot(val source: Bitmap, val edit: Edit = Edit(), val cutout: Boolean = false)
private val ink = Color(0xFFF7F7FA)

@Composable
fun PhotoEditorScreen(onNavigateBack: () -> Unit, initialPhoto: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var state by remember { mutableStateOf<Snapshot?>(null) }
    var baseline by remember { mutableStateOf<Snapshot?>(null) }
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var undo by remember { mutableStateOf(listOf<Snapshot>()) }
    var redo by remember { mutableStateOf(listOf<Snapshot>()) }
    var tool by remember { mutableStateOf(Tool.FILTER) }
    var before by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var exportState by remember { mutableStateOf<Snapshot?>(null) }
    var discardAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val dirty = state != baseline
    fun commit(next: Snapshot) {
        state?.let { if (next != it) { undo = (undo + it).takeLast(30); redo = emptyList(); state = next } }
    }
    fun guarded(action: () -> Unit) { if (dirty) discardAction = action else action() }
    fun load(uri: Uri) = scope.launch {
        busy = true
        try {
            val bitmap = withContext(Dispatchers.IO) { loadPhoto(context, uri) }
            original = bitmap; state = Snapshot(bitmap); baseline = state
            undo = emptyList(); redo = emptyList(); before = false
        } catch (e: Exception) { snackbar.showSnackbar(context.getString(R.string.photo_load_failed)) }
        finally { busy = false }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) load(uri) }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val snapshot = exportState
        if (uri != null && snapshot != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val output = render(snapshot.source, snapshot.edit)
                    try { context.contentResolver.openOutputStream(uri, "w")!!.use {
                        check(output.compress(Bitmap.CompressFormat.PNG, 100, it))
                    } } finally { output.recycle() }
                }
                baseline = snapshot
                snackbar.showSnackbar(context.getString(R.string.photo_saved))
            } catch (e: Exception) { snackbar.showSnackbar(context.getString(R.string.photo_save_failed)) }
            finally { busy = false; exportState = null }
        }
    }
    LaunchedEffect(initialPhoto) { initialPhoto?.let { load(Uri.fromFile(File(it))) } }
    BackHandler(enabled = dirty || busy) { if (!busy) guarded(onNavigateBack) }
    val segmenter = remember { SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()) }
    DisposableEffect(segmenter) { onDispose { segmenter.close() } }
    val current = state
    val preview by produceState<Bitmap?>(null, current, before) {
        value = if (before) original else current?.let { snapshot -> withContext(Dispatchers.Default) {
            val src = snapshot.source
            val scale = minOf(1f, 1200f / maxOf(src.width, src.height))
            val small = Bitmap.createScaledBitmap(src, maxOf(1, (src.width * scale).toInt()), maxOf(1, (src.height * scale).toInt()), true)
            try { render(small, snapshot.edit) } finally { if (small !== src) small.recycle() }
        } }
    }
    Scaffold(containerColor = Color(0xFF0B0B10), snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !busy, onClick = { guarded(onNavigateBack) }) { Text(stringResource(R.string.back)) }
                Text(stringResource(R.string.photo_editor), color = ink, modifier = Modifier.weight(1f))
                Button(enabled = current != null && !busy, onClick = {
                    exportState = state; saver.launch("Pose_${System.currentTimeMillis()}.png")
                }) { Text(stringResource(R.string.export)) }
            }
            if (current != null) Row {
                TextButton(enabled = undo.isNotEmpty() && !busy, onClick = {
                    redo = redo + current; state = undo.last(); undo = undo.dropLast(1)
                }) { Text(stringResource(R.string.undo)) }
                TextButton(enabled = redo.isNotEmpty() && !busy, onClick = {
                    undo = undo + current; state = redo.last(); redo = redo.dropLast(1)
                }) { Text(stringResource(R.string.redo)) }
                TextButton(enabled = !busy, onClick = { before = !before }) {
                    Text(stringResource(if (before) R.string.show_edit else R.string.show_original))
                }
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.fillMaxWidth().weight(1f).padding(12.dp).background(Color(0xFF17171F), RoundedCornerShape(20.dp)), Alignment.Center) {
                if (current == null) Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.no_photo_selected), color = ink)
                    Spacer(Modifier.height(16.dp))
                    Button(enabled = !busy, onClick = { picker.launch("image/*") }) { Text(stringResource(R.string.choose_photo)) }
                } else preview?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize().padding(8.dp), contentScale = ContentScale.Fit) }
                if (busy) CircularProgressIndicator()
            }
            if (current != null) Surface(color = Color(0xFF17171F)) {
                Column(Modifier.navigationBarsPadding().heightIn(max = 310.dp).verticalScroll(rememberScrollState()).padding(12.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tool.entries.forEach { item -> FilterChip(selected = tool == item, enabled = !busy,
                            onClick = { before = false; tool = item }, label = { Text(stringResource(item.label)) }) }
                    }
                    val e = current.edit
                    when (tool) {
                        Tool.FILTER -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Preset.entries.forEach { preset -> FilterChip(selected = e.preset == preset, enabled = !busy,
                                onClick = { commit(current.copy(edit = e.copy(preset = preset))) },
                                label = { Text(if (preset == Preset.ORIGINAL) stringResource(R.string.original) else preset.name.lowercase().replaceFirstChar { it.uppercase() }) }) }
                        }
                        Tool.ADJUST -> {
                            Adjustment(R.string.brightness, e.brightness, -100f..100f, !busy) { commit(current.copy(edit = e.copy(brightness = it))) }
                            Adjustment(R.string.contrast, e.contrast, .5f..1.6f, !busy) { commit(current.copy(edit = e.copy(contrast = it))) }
                            Adjustment(R.string.saturation, e.saturation, 0f..2f, !busy) { commit(current.copy(edit = e.copy(saturation = it))) }
                        }
                        Tool.CROP -> {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Ratio.entries.forEach { ratio -> FilterChip(selected = e.ratio == ratio, enabled = !busy,
                                    onClick = { commit(current.copy(edit = e.copy(ratio = ratio))) }, label = { Text(if (ratio == Ratio.ORIGINAL) stringResource(R.string.original) else ratio.title) }) }
                            }
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                TextButton(enabled = !busy, onClick = { commit(current.copy(edit = e.copy(rotation = (e.rotation + 90) % 360))) }) { Text(stringResource(R.string.rotate)) }
                                TextButton(enabled = !busy, onClick = { commit(current.copy(edit = e.copy(flipH = !e.flipH))) }) { Text(stringResource(R.string.flip_horizontal)) }
                                TextButton(enabled = !busy, onClick = { commit(current.copy(edit = e.copy(flipV = !e.flipV))) }) { Text(stringResource(R.string.flip_vertical)) }
                            }
                        }
                        Tool.AI -> {
                            Text(stringResource(R.string.background_hint), color = ink)
                            Button(enabled = !busy && !current.cutout, onClick = {
                                busy = true
                                segmenter.process(InputImage.fromBitmap(current.source, 0))
                                    .addOnSuccessListener { result ->
                                        busy = false
                                        val foreground = result.foregroundBitmap
                                        if (foreground != null) commit(current.copy(source = foreground.copy(Bitmap.Config.ARGB_8888, false), cutout = true))
                                        else scope.launch { snackbar.showSnackbar(context.getString(R.string.background_failed)) }
                                    }.addOnFailureListener { busy = false; scope.launch { snackbar.showSnackbar(context.getString(R.string.background_failed)) } }
                            }) { Text(stringResource(R.string.remove_background)) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(enabled = !busy, onClick = { guarded { picker.launch("image/*") } }) { Text(stringResource(R.string.replace)) }
                        TextButton(enabled = !busy && original != null, onClick = { commit(Snapshot(original!!)); before = false }) { Text(stringResource(R.string.reset)) }
                    }
                }
            }
        }
    }
    discardAction?.let { action -> AlertDialog(onDismissRequest = { discardAction = null },
        title = { Text(stringResource(R.string.discard_edits)) }, text = { Text(stringResource(R.string.discard_message)) },
        confirmButton = { TextButton(onClick = { discardAction = null; action() }) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton(onClick = { discardAction = null }) { Text(stringResource(R.string.cancel)) } }) }
}

@Composable private fun Adjustment(label: Int, value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, onChange: (Float) -> Unit) {
    Text(stringResource(label), color = ink)
    Slider(value = value, onValueChange = onChange, valueRange = range, enabled = enabled)
}

private fun colorMatrix(e: Edit): ColorMatrix {
    val p = e.preset; val c = e.contrast * p.c; val b = e.brightness + p.b + (1 - c) * 128; val w = p.w
    return ColorMatrix().apply {
        setSaturation(e.saturation * p.s)
        postConcat(ColorMatrix(floatArrayOf(c*(1+w/180),0f,0f,0f,b, 0f,c,0f,0f,b, 0f,0f,c*(1-w/180),0f,b, 0f,0f,0f,1f,0f)))
    }
}

/** Preview and export deliberately share this exact rotate -> crop -> filter pipeline. */
internal fun render(source: Bitmap, edit: Edit): Bitmap {
    val transform = Matrix().apply {
        postScale(if (edit.flipH) -1f else 1f, if (edit.flipV) -1f else 1f)
        postRotate(edit.rotation.toFloat())
    }
    val transformed = Bitmap.createBitmap(source, 0, 0, source.width, source.height, transform, true)
    val ratio = edit.ratio.value
    val width = if (ratio != null && transformed.width.toFloat()/transformed.height > ratio) maxOf(1, (transformed.height*ratio).toInt()) else transformed.width
    val height = if (ratio != null && transformed.width.toFloat()/transformed.height <= ratio) maxOf(1, (transformed.width/ratio).toInt()) else transformed.height
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Canvas(output).drawBitmap(transformed, -(transformed.width-width)/2f, -(transformed.height-height)/2f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(colorMatrix(edit)) })
    if (transformed !== source) transformed.recycle()
    return output
}
