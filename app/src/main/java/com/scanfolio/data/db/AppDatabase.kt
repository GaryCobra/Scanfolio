package com.scanfolio.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.scanfolio.data.db.dao.*
import com.scanfolio.data.db.entity.*

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `market_index_definitions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `code` TEXT NOT NULL DEFAULT '', `created_at` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `market_index_daily_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `index_id` INTEGER NOT NULL, `date` INTEGER NOT NULL, `close_value` REAL NOT NULL, `change_percent` REAL NOT NULL, `kdj_status` TEXT, FOREIGN KEY(`index_id`) REFERENCES `market_index_definitions`(`id`) ON DELETE CASCADE)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_market_index_daily_records_index_id_date` ON `market_index_daily_records` (`index_id`, `date`)")
        db.execSQL("ALTER TABLE `trade_records` ADD COLUMN `is_virtual` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `trade_records` ADD COLUMN `virtual_capital` REAL")
    }
}

@Database(
    entities = [
        StockRecordEntity::class,
        TradeRecordEntity::class,
        ColumnDefinitionEntity::class,
        StrategyTypeEntity::class,
        MarketIndexDefinitionEntity::class,
        MarketIndexDailyRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockRecordDao(): StockRecordDao
    abstract fun tradeRecordDao(): TradeRecordDao
    abstract fun columnDefinitionDao(): ColumnDefinitionDao
    abstract fun strategyTypeDao(): StrategyTypeDao
    abstract fun marketIndexDefinitionDao(): MarketIndexDefinitionDao
        abstract fun marketIndexDailyRecordDao(): MarketIndexDailyRecordDao

    companion object {

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `stock_records` ADD COLUMN `strategy_name` TEXT")
                db.execSQL("ALTER TABLE `trade_records` ADD COLUMN `quantity` INTEGER")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "scanfolio.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
        }
    }
}
