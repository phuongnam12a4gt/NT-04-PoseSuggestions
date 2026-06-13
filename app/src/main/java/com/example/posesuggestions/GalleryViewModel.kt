package com.example.posesuggestions

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val _capturedImages = MutableStateFlow<List<File>>(emptyList())
    val capturedImages = _capturedImages.asStateFlow()

    fun loadImages() {
        viewModelScope.launch {
            val directory = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val files = directory?.listFiles { file -> 
                file.isFile && file.extension.lowercase() == "jpg" 
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
            _capturedImages.value = files
        }
    }

    fun deleteImage(file: File) {
        if (file.exists()) {
            file.delete()
            loadImages()
        }
    }
}
