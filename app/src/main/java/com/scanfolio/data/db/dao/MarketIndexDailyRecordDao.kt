package com.scanfolio.data.db.dao

import androidx.room.*
import com.scanfolio.data.db.entity.MarketIndexDailyRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketIndexDailyRecordDao {
    @Query("SELECT * FROM market_index_daily_records WHERE index_id = :indexId ORDER BY date DESC")
    fun getByIndexId(indexId: Long): Flow<List<MarketIndexDailyRecordEntity>>

    @Query("SELECT * FROM market_index_daily_records WHERE date = :date")
    fun getByDate(date: Long): Flow<List<MarketIndexDailyRecordEntity>>

    @Query("SELECT * FROM market_index_daily_records WHERE index_id = :indexId AND date = :date LIMIT 1")
    suspend fun getByIndexAndDate(indexId: Long, date: Long): MarketIndexDailyRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MarketIndexDailyRecordEntity): Long

    @Update
    suspend fun update(record: MarketIndexDailyRecordEntity)

    @Delete
    suspend fun delete(record: MarketIndexDailyRecordEntity)
}
