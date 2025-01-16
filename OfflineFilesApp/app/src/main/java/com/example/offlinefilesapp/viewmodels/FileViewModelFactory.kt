package com.example.offlinefilesapp.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.offlinefilesapp.data.repository.FileRepository

@Suppress("UNCHECKED_CAST")
class FileViewModelFactory(private val repository: FileRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileViewModel::class.java)) {
            return FileViewModel(Application(),repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
