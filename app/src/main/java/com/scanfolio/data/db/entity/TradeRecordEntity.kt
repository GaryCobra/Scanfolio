package com.scanfolio.data.db.entity

import androidx.room.*

@Entity(
    tableName = "trade_records",
    foreignKeys = [ForeignKey(
        entity = StockRecordEntity::class,
        parentColumns = ["id"],
        childColumns = ["stock_record_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("stock_record_id")]
)
data class TradeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "stock_record_id") val stockRecordId: Long,
    @ColumnInfo(name = "buy_time") val buyTime: Long,
    @ColumnInfo(name = "buy_price") val buyPrice: Double,
    @ColumnInfo(name = "sell_time") val sellTime: Long? = null,
    @ColumnInfo(name = "holding_duration") val holdingDuration: Long? = null,
    @ColumnInfo(name = "profit_ratio") val profitRatio: Double? = null,
    @ColumnInfo(name = "profit_amount") val profitAmount: Double? = null,
    @ColumnInfo(name = "strategy_name") val strategyName: String,
    @ColumnInfo(name = "quantity") val quantity: Int? = null,
    @ColumnInfo(name = "is_success") val isSuccess: Boolean,
    @ColumnInfo(name = "is_virtual") val isVirtual: Boolean = false,
    @ColumnInfo(name = "virtual_capital") val virtualCapital: Double? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
