package com.scanfolio.data.db.entity

import androidx.room.*

@Entity(tableName = "stock_records")
data class StockRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    @ColumnInfo(name = "data_columns")
    val dataColumns: String = "{}",
    @ColumnInfo(name = "last_screenshot")
    val lastScreenshot: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
