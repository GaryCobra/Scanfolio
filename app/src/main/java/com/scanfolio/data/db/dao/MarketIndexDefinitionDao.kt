package com.scanfolio.data.db.dao

import androidx.room.*
import com.scanfolio.data.db.entity.MarketIndexDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketIndexDefinitionDao {
    @Query("SELECT * FROM market_index_definitions ORDER BY created_at ASC")
    fun getAll(): Flow<List<MarketIndexDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(index: MarketIndexDefinitionEntity): Long

    @Update
    suspend fun update(index: MarketIndexDefinitionEntity)

    @Delete
    suspend fun delete(index: MarketIndexDefinitionEntity)
}
