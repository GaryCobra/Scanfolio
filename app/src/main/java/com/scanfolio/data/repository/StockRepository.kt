package com.scanfolio.data.repository

import com.scanfolio.data.db.dao.StockRecordDao
import com.scanfolio.data.db.entity.StockRecordEntity
import kotlinx.coroutines.flow.Flow

class StockRepository(private val dao: StockRecordDao) {
    fun getAll(): Flow<List<StockRecordEntity>> = dao.getAll()

    suspend fun getById(id: Long): StockRecordEntity? = dao.getById(id)

    suspend fun findByCodeAndName(code: String, name: String): StockRecordEntity? =
        dao.getByCodeAndName(code, name)

    suspend fun insert(stock: StockRecordEntity): Long = dao.insert(stock)

    suspend fun update(stock: StockRecordEntity) = dao.update(stock)

    suspend fun delete(stock: StockRecordEntity) = dao.delete(stock)

    suspend fun mergeScreenshotData(
        code: String,
        name: String,
        newData: Map<String, String>
    ): Long {
        val existing = findByCodeAndName(code, name)
        return if (existing != null) {
            val merged = existing.dataColumns + newData
            val updated = existing.copy(
                dataColumns = merged,
                lastScreenshot = System.currentTimeMillis()
            )
            dao.update(updated)
            existing.id
        } else {
            val entity = StockRecordEntity(
                code = code,
                name = name,
                dataColumns = newData
            )
            dao.insert(entity)
        }
    }
}
