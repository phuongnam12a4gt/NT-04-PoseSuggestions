package com.ppnnttt.posesuggestions.ui.screens.editor

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ppnnttt.posesuggestions.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var uri by remember { mutableStateOf<Uri?>(null) }
    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var duration by remember { mutableIntStateOf(0) }
    var position by remember { mutableIntStateOf(0) }
    var start by remember { mutableIntStateOf(0) }
    var end by remember { mutableIntStateOf(0) }
    var keepAudio by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf<VideoView?>(null) }
    var exportSelection by remember { mutableStateOf<VideoSelection?>(null) }
    var discardAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var savedSelection by remember { mutableStateOf<VideoSelection?>(null) }
    val selection = uri?.let { VideoSelection(it, start, end, keepAudio) }
    val changed = duration > 0 && (start > 0 || end != duration || !keepAudio) && selection != savedSelection
    fun guarded(action: () -> Unit) { if (changed) discardAction = action else action() }
    androidx.activity.compose.BackHandler(enabled = changed || exporting) { if (!exporting) guarded(onNavigateBack) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { picked ->
        if (picked != null) {
            view?.stopPlayback(); player = null; view = null; uri = picked
            duration = 0; position = 0; start = 0; end = 0; keepAudio = true; playing = false; savedSelection = null
        }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { destination ->
        val snapshot = exportSelection
        if (destination != null && snapshot != null) scope.launch {
            exporting = true; view?.pause(); playing = false
            try {
                withContext(Dispatchers.IO) { exportTrimmedVideo(context, snapshot, destination) }
                savedSelection = snapshot
                snackbar.showSnackbar(context.getString(R.string.video_exported))
            } catch (e: Exception) { snackbar.showSnackbar(context.getString(R.string.video_error)) }
            finally { exporting = false; exportSelection = null }
        }
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { view?.pause(); playing = false }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); view?.stopPlayback(); player = null; view = null }
    }
    LaunchedEffect(view, playing, end) {
        while (playing) {
            view?.let { video ->
                position = video.currentPosition
                if (position >= end) { video.pause(); video.seekTo(start); position = start; playing = false }
            }
            delay(100)
        }
    }
    Scaffold(containerColor = Color(0xFF0B0B10), snackbarHost = { SnackbarHost(snackbar) }, topBar = {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = !exporting, onClick = { guarded(onNavigateBack) }) { Text(stringResource(R.string.back)) }
            Text(stringResource(R.string.video_editor), color = Color.White, modifier = Modifier.weight(1f))
            Button(enabled = duration > 0 && end > start && !exporting, onClick = {
                exportSelection = selection; exporter.launch("Pose_${System.currentTimeMillis()}.mp4")
            }) { Text(stringResource(if (exporting) R.string.video_exporting else R.string.video_export)) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black), Alignment.Center) {
                val source = uri
                if (source == null) Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.video_empty), color = Color.White)
                    Text(stringResource(R.string.video_hint), color = Color.LightGray, modifier = Modifier.padding(vertical = 12.dp))
                    Button(onClick = { picker.launch("video/*") }) { Text(stringResource(R.string.video_pick)) }
                } else key(source) {
                    AndroidView(factory = { ctx -> VideoView(ctx).apply {
                        view = this
                        setOnPreparedListener { prepared ->
                            player = prepared
                            duration = prepared.duration.coerceAtLeast(0); end = duration
                            prepared.setVolume(if (keepAudio) 1f else 0f, if (keepAudio) 1f else 0f)
                            seekTo(1)
                        }
                        setOnCompletionListener { playing = false; seekTo(start); position = start }
                        setOnErrorListener { _, _, _ ->
                            duration = 0; playing = false
                            scope.launch { snackbar.showSnackbar(context.getString(R.string.video_load_error)) }; true
                        }
                        setVideoURI(source)
                    } }, modifier = Modifier.fillMaxSize())
                }
                if (exporting) CircularProgressIndicator()
            }
            Column(Modifier.fillMaxWidth().navigationBarsPadding().heightIn(max = 310.dp).verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (uri != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(enabled = duration > 0 && !exporting, onClick = {
                            view?.let { video ->
                                if (video.isPlaying) { video.pause(); playing = false }
                                else { if (video.currentPosition !in start until end) video.seekTo(start); video.start(); playing = true }
                            }
                        }) { Text(stringResource(if (playing) R.string.pause else R.string.play)) }
                        Spacer(Modifier.width(12.dp))
                        Text("${formatTime(position)} / ${formatTime(duration)}", color = Color.White)
                    }
                    Slider(value = position.toFloat().coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                        onValueChange = { position = it.toInt(); view?.seekTo(position) },
                        valueRange = 0f..duration.coerceAtLeast(1).toFloat(), enabled = duration > 0 && !exporting)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${stringResource(R.string.start)} ${formatTime(start)}", color = Color.White)
                        Text("${stringResource(R.string.end)} ${formatTime(end)}", color = Color.White)
                    }
                    RangeSlider(value = start.toFloat()..end.toFloat(), valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                        enabled = duration > 0 && !exporting, onValueChange = { range ->
                            val minLength = minOf(1000, duration)
                            if (range.endInclusive - range.start >= minLength) {
                                start = range.start.toInt(); end = range.endInclusive.toInt()
                                view?.pause(); playing = false; view?.seekTo(start); position = start
                            }
                        })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.video_audio), color = Color.White, modifier = Modifier.weight(1f))
                        Switch(checked = keepAudio, enabled = !exporting, onCheckedChange = {
                            keepAudio = it
                            view?.pause(); playing = false
                            player?.setVolume(if (it) 1f else 0f, if (it) 1f else 0f)
                        })
                    }
                    Text(stringResource(R.string.video_trim_hint), color = Color.LightGray)
                    TextButton(enabled = !exporting, onClick = { guarded { picker.launch("video/*") } }) { Text(stringResource(R.string.video_pick)) }
                }
            }
        }
    }
    discardAction?.let { action -> AlertDialog(onDismissRequest = { discardAction = null },
        title = { Text(stringResource(R.string.discard_edits)) }, text = { Text(stringResource(R.string.discard_message)) },
        confirmButton = { TextButton(onClick = { discardAction = null; action() }) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton(onClick = { discardAction = null }) { Text(stringResource(R.string.cancel)) } }) }
}

internal data class VideoSelection(val source: Uri, val startMs: Int, val endMs: Int, val keepAudio: Boolean)
private fun formatTime(ms: Int): String { val seconds = ms.coerceAtLeast(0)/1000; return "%d:%02d".format(seconds/60, seconds%60) }

/** Remux from a decodable video keyframe, preserving orientation and optional audio. */
internal fun exportTrimmedVideo(context: Context, selection: VideoSelection, destination: Uri) {
    require(selection.endMs > selection.startMs)
    val temp = File.createTempFile("pose-export-", ".mp4", context.cacheDir)
    val extractor = MediaExtractor()
    var muxer: MediaMuxer? = null
    try {
        context.contentResolver.openFileDescriptor(selection.source, "r")!!.use { extractor.setDataSource(it.fileDescriptor) }
        val videoTrack = (0 until extractor.trackCount).first { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }
        extractor.selectTrack(videoTrack)
        extractor.seekTo(selection.startMs * 1000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val originUs = extractor.sampleTime.coerceAtLeast(0)
        extractor.unselectTrack(videoTrack)
        val endUs = selection.endMs * 1000L
        require(endUs > originUs)
        val output = MediaMuxer(temp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxer = output
        val videoFormat = extractor.getTrackFormat(videoTrack)
        val metadata = android.media.MediaMetadataRetriever()
        try {
            metadata.setDataSource(context, selection.source)
            metadata.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
                ?.takeIf { it in listOf(0,90,180,270) }?.let { output.setOrientationHint(it) }
        } finally { metadata.release() }
        val tracks = (0 until extractor.trackCount).filter { index ->
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
            index == videoTrack || (selection.keepAudio && mime.startsWith("audio/"))
        }.map { it to output.addTrack(extractor.getTrackFormat(it)) }
        output.start()
        var buffer = ByteBuffer.allocate(1024*1024)
        val info = MediaCodec.BufferInfo()
        var videoSamples = 0
        tracks.forEach { (inputTrack, outputTrack) ->
            extractor.selectTrack(inputTrack)
            extractor.seekTo(originUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val format = extractor.getTrackFormat(inputTrack)
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                val required = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                require(required <= 32*1024*1024)
                if (required > buffer.capacity()) buffer = ByteBuffer.allocate(required)
            }
            while (true) {
                val timeUs = extractor.sampleTime
                if (timeUs < 0 || timeUs >= endUs) break
                if (timeUs >= originUs) {
                    buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    require(extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED == 0)
                    val flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    info.set(0, size, timeUs-originUs, flags)
                    output.writeSampleData(outputTrack, buffer, info)
                    if (inputTrack == videoTrack) videoSamples++
                }
                if (!extractor.advance()) break
            }
            extractor.unselectTrack(inputTrack)
        }
        check(videoSamples > 0)
        output.stop()
        context.contentResolver.openOutputStream(destination, "w")!!.use { target -> temp.inputStream().use { it.copyTo(target) } }
    } finally {
        runCatching { muxer?.release() }; extractor.release(); temp.delete()
    }
}
