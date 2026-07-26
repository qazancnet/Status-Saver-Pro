package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FileItem
import com.example.data.FileManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileManagerRepository(application)
    private val sharedPrefs = application.getSharedPreferences("StatusSaverPrefs", android.content.Context.MODE_PRIVATE)

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files = _files.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            val savedUriStr = sharedPrefs.getString("saf_folder_uri", null)
            val safUri = if (savedUriStr != null) Uri.parse(savedUriStr) else null

            repository.scanWhatsAppMedia(safUri).collect { scannedFiles ->
                _files.value = scannedFiles
                _isLoading.value = false
            }
        }
    }

    fun deleteFile(fileItem: FileItem) {
        val success = repository.deleteFile(fileItem)
        if (success) {
            val updated = _files.value.filter { it.uri != fileItem.uri && it.filePath != fileItem.filePath }
            _files.value = updated
        }
    }
}
