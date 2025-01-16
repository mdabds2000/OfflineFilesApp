package com.example.offlinefilesapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.offlinefilesapp.data.models.FileEntity

@Dao
interface FileDao {

    @Insert
    suspend fun insert(file: FileEntity)

    @Update
    suspend fun update(file: FileEntity)

    @Query("SELECT * FROM fileentity WHERE id = :id")
    suspend fun getFileById(id: Long): FileEntity?

    @Query("SELECT * FROM fileentity WHERE isTrashed = 1")
    suspend fun getTrashedFiles(): List<FileEntity>

    @Query("SELECT * FROM fileentity WHERE isTrashed = 0")
    suspend fun getActiveFiles(): List<FileEntity>

    @Query("UPDATE fileentity SET isTrashed = 1, dateTrashed = :timestamp WHERE id = :id")
    suspend fun trashFile(id: Long, timestamp: Long)

    @Query("UPDATE fileentity SET isTrashed = 0 WHERE id = :id")
    suspend fun restoreFile(id: Long)

    @Query("DELETE FROM fileentity WHERE id = :id")
    suspend fun deleteFile(id: Long)
}
