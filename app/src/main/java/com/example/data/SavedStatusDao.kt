package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedStatusDao {
    @Query("SELECT * FROM saved_statuses ORDER BY dateSaved DESC")
    fun getAllSaved(): Flow<List<SavedStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaved(status: SavedStatusEntity)

    @Query("DELETE FROM saved_statuses WHERE id = :id")
    suspend fun deleteSavedById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_statuses WHERE id = :id)")
    suspend fun isSaved(id: String): Boolean
}
