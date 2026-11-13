package com.ppnnttt.posesuggestions.ui.screens.editor

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.ppnnttt.posesuggestions.R
import com.ppnnttt.posesuggestions.loadPhoto
import kotlinx.coroutines.*

internal data class PhotoEdits(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val rotation: Int = 0,
    val removeBackground: Boolean = false
)

class PhotoEditorViewModel(application: Application) : AndroidViewModel(application) {
    var original by mutableStateOf<Bitmap?>(null); private set
    var foreground by mutableStateOf<Bitmap?>(null); private set
    internal var edits by mutableStateOf(PhotoEdits()); private set
    private var savedEdits = PhotoEdits()
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<Int?>(null); private set
    private var initialized = false
    private var closed = false
    private val segmenter = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
    )
    val source: Bitmap? get() = if (edits.removeBackground) foreground else original
    val dirty: Boolean get() = original != null && edits != savedEdits

    fun initialize(path: String?) {
        if (initialized) return
        initialized = true
        if (!path.isNullOrBlank()) importPhoto(Uri.fromFile(java.io.File(path)))
    }

    fun importPhoto(uri: Uri) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { loadPhoto(getApplication(), uri) }
                original = loaded
                foreground = null
                edits = PhotoEdits()
                savedEdits = edits
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message = R.string.photo_load_failed }
            finally { busy = false }
        }
    }

    internal fun update(value: PhotoEdits) { if (!busy) edits = value }
    fun reset() { if (!busy) edits = PhotoEdits() }
    fun toggleBackground() {
        if (busy) return
        if (edits.removeBackground || foreground != null) {
            edits = edits.copy(removeBackground = !edits.removeBackground)
            return
        }
        val bitmap = original ?: return
        busy = true
        try {
            segmenter.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { result ->
                    if (!closed) {
                        val cutout = result.foregroundBitmap
                        if (cutout != null) {
                            foreground = cutout
                            edits = edits.copy(removeBackground = true)
                        } else message = R.string.background_failed
                        busy = false
                    }
                }
                .addOnFailureListener {
                    if (!closed) { message = R.string.background_failed; busy = false }
                }
        } catch (e: Exception) { message = R.string.background_failed; busy = false }
    }

    // Lock the session while the system document picker is open as well as during export.
    fun beginExport(): Boolean {
        if (busy || source == null) return false
        busy = true
        return true
    }
    fun export(destination: Uri?) {
        if (destination == null) { busy = false; return }
        val bitmap = source ?: run { busy = false; return }
        val snapshot = edits
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    saveEditedPhotoToUri(getApplication(), destination, bitmap,
                        snapshot.brightness, snapshot.contrast, snapshot.saturation, snapshot.rotation)
                }
                if (success) savedEdits = snapshot
                message = if (success) R.string.photo_saved else R.string.photo_save_failed
            } finally { busy = false }
        }
    }
    fun clearMessage() { message = null }
    override fun onCleared() {
        closed = true
        segmenter.close()

    }
}
