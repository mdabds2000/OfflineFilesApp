package com.example.offlinefilesapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fileentity")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String,
    val type: String,
    val size: Long,
    val dateAdded: Long,
    val previewType: String,
    val dateTrashed: Long? = null,
    val isTrashed: Boolean = false
)
