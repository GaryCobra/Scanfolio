package com.scanfolio.data.db.entity

import androidx.room.*

@Entity(
    tableName = "market_index_daily_records",
    foreignKeys = [ForeignKey(
        entity = MarketIndexDefinitionEntity::class,
        parentColumns = ["id"],
        childColumns = ["index_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["index_id", "date"], unique = true)]
)
data class MarketIndexDailyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "index_id") val indexId: Long,
    val date: Long,
    @ColumnInfo(name = "close_value") val closeValue: Double,
    @ColumnInfo(name = "change_percent") val changePercent: Double,
    @ColumnInfo(name = "kdj_status") val kdjStatus: String? = null
)
