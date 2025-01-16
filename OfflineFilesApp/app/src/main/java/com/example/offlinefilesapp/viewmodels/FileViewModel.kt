package com.example.offlinefilesapp.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.offlinefilesapp.data.models.FileEntity
import com.example.offlinefilesapp.data.repository.FileRepository
import kotlinx.coroutines.launch
import java.io.File

class FileViewModel(application: Application, private val repository: FileRepository) :
    AndroidViewModel(application) {

    private val _files = MutableLiveData<List<FileEntity>>()
    val files: LiveData<List<FileEntity>> = _files

    private val _trashedFiles = MutableLiveData<List<FileEntity>>()
    val trashedFiles: LiveData<List<FileEntity>> = _trashedFiles

    init {
        getAllFiles()
    }

    fun addFile(file: FileEntity) {
        viewModelScope.launch {
            repository.insertFile(file)
            getAllFiles()
        }
    }

    private fun getAllFiles() {
        viewModelScope.launch {
            _files.value = repository.getActiveFiles()
        }
    }

    fun getTrashedFiles() {
        viewModelScope.launch {
            _trashedFiles.value = repository.getTrashedFiles()
        }
    }

    fun trashFile(fileId: Long) {
        viewModelScope.launch {
            repository.trashFile(fileId)
            getAllFiles()
            getTrashedFiles()
        }
    }

    fun restoreFile(fileId: Long) {
        viewModelScope.launch {
            repository.restoreFile(fileId)
            getTrashedFiles()
            getAllFiles()
        }
    }

    fun deleteFile(fileId: Long, context: Context) {
        viewModelScope.launch {
            val file = repository.getFileById(fileId)
            if (file != null) {
                val fileUri = Uri.parse(file.path)
                try {
                    getApplication<Application>().contentResolver.releasePersistableUriPermission(
                        fileUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (deleteFileFromLocalStorage(context, fileUri)) {
                    repository.deleteFile(file.id)
                } else {
                    Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show()
                }
                getTrashedFiles()
            }
        }
    }

    private fun deleteFileFromLocalStorage(context: Context, fileUri: Uri): Boolean {
        return try {
            if ("file" == fileUri.scheme) {
                val file = File(fileUri.path ?: "")
                if (file.exists()) {
                    file.delete()
                } else {
                    false
                }
            } else {
                context.contentResolver.delete(fileUri, null, null) > 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to delete the file", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
