package com.scanfolio.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.scanfolio.data.db.dao.*
import com.scanfolio.data.db.entity.*

@Database(
    entities = [
        StockRecordEntity::class,
        TradeRecordEntity::class,
        ColumnDefinitionEntity::class,
        StrategyTypeEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockRecordDao(): StockRecordDao
    abstract fun tradeRecordDao(): TradeRecordDao
    abstract fun columnDefinitionDao(): ColumnDefinitionDao
    abstract fun strategyTypeDao(): StrategyTypeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "scanfolio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
