package com.scanfolio.data.db.dao

import androidx.room.*
import com.scanfolio.data.db.entity.TradeRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeRecordDao {
    @Query("SELECT * FROM trade_records WHERE stock_record_id = :stockId ORDER BY created_at DESC")
    fun getByStockId(stockId: Long): Flow<List<TradeRecordEntity>>

    @Query("SELECT * FROM trade_records WHERE is_success = :isSuccess")
    fun getBySuccess(isSuccess: Boolean): Flow<List<TradeRecordEntity>>

    @Query("SELECT * FROM trade_records WHERE is_virtual = :isVirtual")
    fun getByVirtual(isVirtual: Boolean): Flow<List<TradeRecordEntity>>

    @Query("SELECT * FROM trade_records")
    fun getAll(): Flow<List<TradeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trade: TradeRecordEntity): Long

    @Update
    suspend fun update(trade: TradeRecordEntity)

    @Delete
    suspend fun delete(trade: TradeRecordEntity)
}
