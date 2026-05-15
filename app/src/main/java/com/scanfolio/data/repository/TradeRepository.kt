package com.scanfolio.data.repository

import com.scanfolio.data.db.dao.TradeRecordDao
import com.scanfolio.data.db.entity.TradeRecordEntity
import kotlinx.coroutines.flow.Flow

class TradeRepository(private val dao: TradeRecordDao) {
    fun getByStockId(stockId: Long): Flow<List<TradeRecordEntity>> =
        dao.getByStockId(stockId)

    fun getBySuccess(isSuccess: Boolean): Flow<List<TradeRecordEntity>> =
        dao.getBySuccess(isSuccess)

    fun getAll(): Flow<List<TradeRecordEntity>> = dao.getAll()

    suspend fun insert(trade: TradeRecordEntity): Long = dao.insert(trade)

    suspend fun update(trade: TradeRecordEntity) = dao.update(trade)

    suspend fun delete(trade: TradeRecordEntity) = dao.delete(trade)
}
