package com.scanfolio.data.db.dao

import androidx.room.*
import com.scanfolio.data.db.entity.StockRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockRecordDao {
    @Query("SELECT * FROM stock_records ORDER BY last_screenshot DESC")
    fun getAll(): Flow<List<StockRecordEntity>>

    @Query("SELECT * FROM stock_records WHERE id = :id")
    suspend fun getById(id: Long): StockRecordEntity?

    @Query("SELECT * FROM stock_records WHERE code = :code AND name = :name LIMIT 1")
    suspend fun getByCodeAndName(code: String, name: String): StockRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stock: StockRecordEntity): Long

    @Update
    suspend fun update(stock: StockRecordEntity)

    @Delete
    suspend fun delete(stock: StockRecordEntity)
}
