package com.scanfolio.data.repository

import com.scanfolio.data.db.dao.ColumnDefinitionDao
import com.scanfolio.data.db.dao.StrategyTypeDao
import com.scanfolio.data.db.entity.*
import kotlinx.coroutines.flow.Flow

class SettingsRepository(
    private val columnDao: ColumnDefinitionDao,
    private val strategyDao: StrategyTypeDao
) {
    fun getEnabledColumns(): Flow<List<ColumnDefinitionEntity>> = columnDao.getEnabledColumns()
    fun getAllColumns(): Flow<List<ColumnDefinitionEntity>> = columnDao.getAllColumns()
    suspend fun getAllColumnsSync(): List<ColumnDefinitionEntity> = columnDao.getAllSync()
    suspend fun addColumn(column: ColumnDefinitionEntity) = columnDao.insert(column)
    suspend fun addColumns(columns: List<ColumnDefinitionEntity>) = columnDao.insertAll(columns)
    suspend fun updateColumn(column: ColumnDefinitionEntity) = columnDao.update(column)
    suspend fun deleteColumn(id: Long) = columnDao.deleteById(id)
    suspend fun reorderColumn(id: Long, order: Int) = columnDao.updateSortOrder(id, order)
    suspend fun markAsBuiltIn(id: Long) = columnDao.updateIsBuiltIn(id, true)

    fun getStrategies(): Flow<List<StrategyTypeEntity>> = strategyDao.getAll()
    suspend fun addStrategy(name: String) =
        strategyDao.insert(StrategyTypeEntity(name = name))
    suspend fun deleteStrategy(name: String) = strategyDao.deleteByName(name)
}
