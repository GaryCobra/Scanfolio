package com.scanfolio.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "strategy_types")
data class StrategyTypeEntity(
    @PrimaryKey val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
