package com.ppnnttt.posesuggestions.ui.screens.gallery

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val _capturedImages = MutableStateFlow<List<File>>(emptyList())
    val capturedImages = _capturedImages.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()
    private val _deleteFailed = MutableStateFlow(false)
    val deleteFailed = _deleteFailed.asStateFlow()
    fun clearError() { _deleteFailed.value = false }
    fun loadImages() {
        viewModelScope.launch {
            _loading.value = true
            _capturedImages.value = withContext(Dispatchers.IO) {
                getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?.listFiles { file -> file.isFile && file.extension.lowercase() in setOf("jpg", "jpeg", "png") }
                    ?.sortedByDescending { it.lastModified() } ?: emptyList()
            }
            _loading.value = false
        }
    }
    fun deleteImage(file: File) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) { runCatching { !file.exists() || file.delete() }.getOrDefault(false) }
            if (deleted) loadImages() else _deleteFailed.value = true
        }
    }
}
