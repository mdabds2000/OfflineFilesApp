package com.example.offlinefilesapp.data.repository

import com.example.offlinefilesapp.data.database.FileDao
import com.example.offlinefilesapp.data.models.FileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class FileRepository(private val fileDao: FileDao) {

    suspend fun getActiveFiles(): List<FileEntity> {
        return fileDao.getActiveFiles()
    }

    suspend fun getTrashedFiles(): List<FileEntity> {
        return fileDao.getTrashedFiles()
    }

    suspend fun trashFile(id: Long) {
        fileDao.trashFile(id, Date().time)
    }

    suspend fun restoreFile(id: Long) {
        fileDao.restoreFile(id)
    }

    suspend fun deleteFile(id: Long) {
        fileDao.deleteFile(id)
    }

    suspend fun insertFile(file: FileEntity) {
        fileDao.insert(file)
    }

    suspend fun getFileById(fileId: Long): FileEntity? {
        return withContext(Dispatchers.IO) {
            fileDao.getFileById(fileId)
        }
    }
}
