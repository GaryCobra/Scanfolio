package com.scanfolio.data.db.dao

import androidx.room.*
import com.scanfolio.data.db.entity.ColumnDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ColumnDefinitionDao {
    @Query("SELECT * FROM column_definitions WHERE enabled = 1 ORDER BY sort_order ASC")
    fun getEnabledColumns(): Flow<List<ColumnDefinitionEntity>>

    @Query("SELECT * FROM column_definitions ORDER BY sort_order ASC")
    fun getAllColumns(): Flow<List<ColumnDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(column: ColumnDefinitionEntity)

    @Update
    suspend fun update(column: ColumnDefinitionEntity)

    @Delete
    suspend fun delete(column: ColumnDefinitionEntity)

    @Query("DELETE FROM column_definitions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE column_definitions SET sort_order = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)
}
