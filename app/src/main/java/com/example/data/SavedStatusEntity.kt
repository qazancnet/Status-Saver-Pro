package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_statuses")
data class SavedStatusEntity(
    @PrimaryKey val id: String,
    val uriString: String,
    val fileName: String,
    val isVideo: Boolean,
    val size: Long,
    val dateSaved: Long,
    val originalPath: String,
    val editedPath: String? = null,
    val hasEdits: Boolean = false
)
