package com.scanfolio.data.repository

import com.scanfolio.data.db.dao.MarketIndexDailyRecordDao
import com.scanfolio.data.db.dao.MarketIndexDefinitionDao
import com.scanfolio.data.db.entity.MarketIndexDailyRecordEntity
import com.scanfolio.data.db.entity.MarketIndexDefinitionEntity
import kotlinx.coroutines.flow.Flow

class MarketIndexRepository(
    private val defDao: MarketIndexDefinitionDao,
    private val dailyDao: MarketIndexDailyRecordDao
) {
    fun getAllDefinitions(): Flow<List<MarketIndexDefinitionEntity>> = defDao.getAll()
    suspend fun addDefinition(index: MarketIndexDefinitionEntity): Long = defDao.insert(index)
    suspend fun updateDefinition(index: MarketIndexDefinitionEntity) = defDao.update(index)
    suspend fun deleteDefinition(index: MarketIndexDefinitionEntity) = defDao.delete(index)

    fun getRecordsByIndex(indexId: Long): Flow<List<MarketIndexDailyRecordEntity>> =
        dailyDao.getByIndexId(indexId)

    fun getRecordsByDate(date: Long): Flow<List<MarketIndexDailyRecordEntity>> =
        dailyDao.getByDate(date)

    suspend fun getRecordByIndexAndDate(indexId: Long, date: Long): MarketIndexDailyRecordEntity? =
        dailyDao.getByIndexAndDate(indexId, date)

    suspend fun addRecord(record: MarketIndexDailyRecordEntity): Long = dailyDao.insert(record)
    suspend fun updateRecord(record: MarketIndexDailyRecordEntity) = dailyDao.update(record)
    suspend fun deleteRecord(record: MarketIndexDailyRecordEntity) = dailyDao.delete(record)
}
