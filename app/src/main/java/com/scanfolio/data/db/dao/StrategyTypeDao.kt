package com.scanfolio.data.db.dao

import androidx.room.*
import com.scanfolio.data.db.entity.StrategyTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StrategyTypeDao {
    @Query("SELECT * FROM strategy_types ORDER BY created_at ASC")
    fun getAll(): Flow<List<StrategyTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(strategy: StrategyTypeEntity)

    @Query("DELETE FROM strategy_types WHERE name = :name")
    suspend fun deleteByName(name: String)
}
