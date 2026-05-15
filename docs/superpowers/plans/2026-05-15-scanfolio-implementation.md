# Scanfolio Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a fully offline Android app that OCR-scans 同花顺 stock screenshots, merges multi-screenshot data by stock code, records trades with buy/sell info and strategy outcomes, and provides success/failure comparative analysis.

**Architecture:** Single-activity Kotlin + Jetpack Compose app with Room database. 4 feature modules (scan, portfolio, analysis, settings) sharing a common data layer. Tesseract handles offline OCR. MPAndroidChart renders comparison charts.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Tesseract (tess-two), MPAndroidChart, Navigation Compose

**File Structure:**
```
app/src/main/java/com/scanfolio/
├── ScanfolioApp.kt
├── MainActivity.kt
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt
│   │   ├── Converters.kt
│   │   ├── dao/
│   │   │   ├── StockRecordDao.kt
│   │   │   ├── TradeRecordDao.kt
│   │   │   ├── StrategyTypeDao.kt
│   │   │   └── ColumnDefinitionDao.kt
│   │   └── entity/
│   │       ├── StockRecordEntity.kt
│   │       ├── TradeRecordEntity.kt
│   │       ├── StrategyTypeEntity.kt
│   │       └── ColumnDefinitionEntity.kt
│   └── repository/
│       ├── StockRepository.kt
│       ├── TradeRepository.kt
│       └── SettingsRepository.kt
├── ocr/
│   ├── OcrEngine.kt
│   ├── ImagePreprocessor.kt
│   ├── TableAnalyzer.kt
│   ├── HeaderRecognizer.kt
│   └── OcrResult.kt
├── ui/
│   ├── navigation/AppNavigation.kt
│   ├── theme/Theme.kt
│   ├── portfolio/
│   │   ├── PortfolioScreen.kt
│   │   ├── PortfolioViewModel.kt
│   │   ├── StockDetailScreen.kt
│   │   └── TradeFormSheet.kt
│   ├── scan/
│   │   ├── ScanScreen.kt
│   │   ├── ScanViewModel.kt
│   │   ├── ScanPreviewScreen.kt
│   │   └── ScanPreviewViewModel.kt
│   ├── analysis/
│   │   ├── AnalysisScreen.kt
│   │   └── AnalysisViewModel.kt
│   └── settings/
│       ├── SettingsScreen.kt
│       ├── SettingsViewModel.kt
│       ├── ColumnManageScreen.kt
│       └── StrategyManageScreen.kt
└── util/
    ├── JsonUtils.kt
    ├── DateUtils.kt
    └── ExportImportManager.kt
```

---

### Task 1: Project Scaffolding — Gradle, Manifest, Application, Theme

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (project-level)
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/scanfolio/ScanfolioApp.kt`
- Create: `app/src/main/java/com/scanfolio/MainActivity.kt`
- Create: `app/src/main/java/com/scanfolio/ui/theme/Theme.kt`
- Create: `app/src/main/res/values/themes.xml`
- Create: `gradle.properties`
- Create: `app/proguard-rules.pro`

- [ ] **Step 1: Create project-level build files**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Scanfolio"
include(":app")
```

`build.gradle.kts` (project):
```kotlin
plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 2: Create app build.gradle.kts with dependencies**

`app/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.scanfolio"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.scanfolio"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
    kotlinOptions { jvmTarget = "17" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Tesseract
    implementation("com.rmtheis:tess-two:9.1.0")

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // JSON
    implementation("com.google.code.gson:gson:2.11.0")

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
}
```

- [ ] **Step 3: Create AndroidManifest.xml**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <uses-feature android:name="android.hardware.camera" android:required="false" />

    <application
        android:name=".ScanfolioApp"
        android:allowBackup="true"
        android:label="Scanfolio"
        android:supportsRtl="true"
        android:theme="@style/Theme.Scanfolio">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Scanfolio">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 4: Create Application class**

`app/src/main/java/com/scanfolio/ScanfolioApp.kt`:
```kotlin
package com.scanfolio

import android.app.Application
import com.scanfolio.data.db.AppDatabase

class ScanfolioApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
```

- [ ] **Step 5: Create theme files**

`app/src/main/res/values/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Scanfolio" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

`app/src/main/java/com/scanfolio/ui/theme/Theme.kt`:
```kotlin
package com.scanfolio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = OnPrimaryColor,
    primaryContainer = PrimaryContainerColor,
    secondary = SecondaryColor,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColorDark,
    onPrimary = OnPrimaryColorDark,
    primaryContainer = PrimaryContainerColorDark,
)

@Composable
fun ScanfolioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

Note: Define color constants directly as `Color(0xFF...)` values at the top of Theme.kt.

- [ ] **Step 6: Create MainActivity with navigation scaffold**

`app/src/main/java/com/scanfolio/MainActivity.kt`:
```kotlin
package com.scanfolio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.scanfolio.ui.navigation.AppNavigation
import com.scanfolio.ui.theme.ScanfolioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScanfolioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
```

- [ ] **Step 7: Commit**

```
git init
git add -A
git commit -m "feat: scaffold Android project with Compose, Room, Tesseract"
```

---

### Task 2: Data Layer — Entities and Type Converters

**Files:**
- Create: `app/src/main/java/com/scanfolio/data/db/entity/ColumnDefinitionEntity.kt`
- Create: `app/src/main/java/com/scanfolio/data/db/entity/StrategyTypeEntity.kt`
- Create: `app/src/main/java/com/scanfolio/data/db/entity/StockRecordEntity.kt`
- Create: `app/src/main/java/com/scanfolio/data/db/entity/TradeRecordEntity.kt`
- Create: `app/src/main/java/com/scanfolio/data/db/Converters.kt`

- [ ] **Step 1: Create ColumnDefinitionEntity**

`app/src/main/java/com/scanfolio/data/db/entity/ColumnDefinitionEntity.kt`:
```kotlin
package com.scanfolio.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "column_definitions")
data class ColumnDefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    val enabled: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: Create StrategyTypeEntity**

`app/src/main/java/com/scanfolio/data/db/entity/StrategyTypeEntity.kt`:
```kotlin
package com.scanfolio.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "strategy_types")
data class StrategyTypeEntity(
    @PrimaryKey val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: Create StockRecordEntity**

`app/src/main/java/com/scanfolio/data/db/entity/StockRecordEntity.kt`:
```kotlin
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
```

- [ ] **Step 4: Create TradeRecordEntity**

`app/src/main/java/com/scanfolio/data/db/entity/TradeRecordEntity.kt`:
```kotlin
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
    @ColumnInfo(name = "is_success") val isSuccess: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 5: Create TypeConverters**

`app/src/main/java/com/scanfolio/data/db/Converters.kt`:
```kotlin
package com.scanfolio.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String =
        gson.toJson(value)

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return try { gson.fromJson(value, type) } catch (_: Exception) { emptyMap() }
    }
}
```

- [ ] **Step 6: Commit**

```
git add -A
git commit -m "feat: define Room entities with type converters"
```

---

### Task 3: Data Layer — DAOs

**Files:**
- Create: `app/src/main/java/com/scanfolio/data/db/dao/ColumnDefinitionDao.kt`
- Create: `app/src/main/java/com/scanfolio/data/db/dao/StrategyTypeDao.kt`
- Create: `app/src/main/java/com/scanfolio/data/db/dao/StockRecordDao.kt`
- Create: `app/src/main/java/com/scanfolio/data/db/dao/TradeRecordDao.kt`
- Create: `app/src/main/java/com/scanfolio/data/db/AppDatabase.kt`

- [ ] **Step 1: Create ColumnDefinitionDao**

`app/src/main/java/com/scanfolio/data/db/dao/ColumnDefinitionDao.kt`:
```kotlin
package com.scanfolio.data.db.dao

import androidx.room.*
import com.scanfolio.data.db.entity.ColumnDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ColumnDefinitionDao {
    @Query("SELECT * FROM column_definitions WHERE enabled = 1 ORDER BY sort_order ASC")
    fun getEnabledColumns(): Flow<List<ColumnDefinitionEntity>>

    @Query("SELECT * FROM column_definitions ORDER BY sort_order ASC")
    fun getAllColumns(): Flow<List<ColumnDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(column: ColumnDefinitionEntity)

    @Update
    suspend fun update(column: ColumnDefinitionEntity)

    @Delete
    suspend fun delete(column: ColumnDefinitionEntity)

    @Query("DELETE FROM column_definitions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE column_definitions SET sort_order = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)
}
```

- [ ] **Step 2: Create StrategyTypeDao**

`app/src/main/java/com/scanfolio/data/db/dao/StrategyTypeDao.kt`:
```kotlin
package com.scanfolio.data.db.dao

import androidx.room.*
import com.scanfolio.data.db.entity.StrategyTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StrategyTypeDao {
    @Query("SELECT * FROM strategy_types ORDER BY created_at ASC")
    fun getAll(): Flow<List<StrategyTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(strategy: StrategyTypeEntity)

    @Query("DELETE FROM strategy_types WHERE name = :name")
    suspend fun deleteByName(name: String)
}
```

- [ ] **Step 3: Create StockRecordDao**

`app/src/main/java/com/scanfolio/data/db/dao/StockRecordDao.kt`:
```kotlin
package com.scanfolio.data.db.dao

import androidx.room.*
import com.scanfolio.data.db.entity.StockRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockRecordDao {
    @Query("SELECT * FROM stock_records ORDER BY last_screenshot DESC")
    fun getAll(): Flow<List<StockRecordEntity>>

    @Query("SELECT * FROM stock_records WHERE id = :id")
    suspend fun getById(id: Long): StockRecordEntity?

    @Query("SELECT * FROM stock_records WHERE code = :code AND name = :name LIMIT 1")
    suspend fun getByCodeAndName(code: String, name: String): StockRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stock: StockRecordEntity): Long

    @Update
    suspend fun update(stock: StockRecordEntity)

    @Delete
    suspend fun delete(stock: StockRecordEntity)
}
```

- [ ] **Step 4: Create TradeRecordDao**

`app/src/main/java/com/scanfolio/data/db/dao/TradeRecordDao.kt`:
```kotlin
package com.scanfolio.data.db.dao

import androidx.room.*
import com.scanfolio.data.db.entity.TradeRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeRecordDao {
    @Query("SELECT * FROM trade_records WHERE stock_record_id = :stockId ORDER BY created_at DESC")
    fun getByStockId(stockId: Long): Flow<List<TradeRecordEntity>>

    @Query("SELECT * FROM trade_records WHERE is_success = :isSuccess")
    fun getBySuccess(isSuccess: Boolean): Flow<List<TradeRecordEntity>>

    @Query("SELECT * FROM trade_records")
    fun getAll(): Flow<List<TradeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trade: TradeRecordEntity): Long

    @Update
    suspend fun update(trade: TradeRecordEntity)

    @Delete
    suspend fun delete(trade: TradeRecordEntity)
}
```

- [ ] **Step 5: Create AppDatabase**

`app/src/main/java/com/scanfolio/data/db/AppDatabase.kt`:
```kotlin
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
```

- [ ] **Step 6: Commit**

```
git add -A
git commit -m "feat: implement DAOs and AppDatabase"
```

---

### Task 4: Data Layer — Repositories

**Files:**
- Create: `app/src/main/java/com/scanfolio/data/repository/StockRepository.kt`
- Create: `app/src/main/java/com/scanfolio/data/repository/TradeRepository.kt`
- Create: `app/src/main/java/com/scanfolio/data/repository/SettingsRepository.kt`

- [ ] **Step 1: Create StockRepository**

`app/src/main/java/com/scanfolio/data/repository/StockRepository.kt`:
```kotlin
package com.scanfolio.data.repository

import com.scanfolio.data.db.dao.StockRecordDao
import com.scanfolio.data.db.entity.StockRecordEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

class StockRepository(private val dao: StockRecordDao) {
    private val gson = Gson()

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
            val existingData = parseDataColumns(existing.dataColumns)
            val merged = existingData + newData
            val updated = existing.copy(
                dataColumns = gson.toJson(merged),
                lastScreenshot = System.currentTimeMillis()
            )
            dao.update(updated)
            existing.id
        } else {
            val entity = StockRecordEntity(
                code = code,
                name = name,
                dataColumns = gson.toJson(newData)
            )
            dao.insert(entity)
        }
    }

    fun parseDataColumns(json: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return try { gson.fromJson(json, type) } catch (_: Exception) { emptyMap() }
    }
}
```

- [ ] **Step 2: Create TradeRepository**

`app/src/main/java/com/scanfolio/data/repository/TradeRepository.kt`:
```kotlin
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
```

- [ ] **Step 3: Create SettingsRepository**

`app/src/main/java/com/scanfolio/data/repository/SettingsRepository.kt`:
```kotlin
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
    suspend fun addColumn(column: ColumnDefinitionEntity) = columnDao.insert(column)
    suspend fun updateColumn(column: ColumnDefinitionEntity) = columnDao.update(column)
    suspend fun deleteColumn(id: Long) = columnDao.deleteById(id)
    suspend fun reorderColumn(id: Long, order: Int) = columnDao.updateSortOrder(id, order)

    fun getStrategies(): Flow<List<StrategyTypeEntity>> = strategyDao.getAll()
    suspend fun addStrategy(name: String) =
        strategyDao.insert(StrategyTypeEntity(name = name))
    suspend fun deleteStrategy(name: String) = strategyDao.deleteByName(name)
}
```

- [ ] **Step 4: Commit**

```
git add -A
git commit -m "feat: implement repositories for data access layer"
```

---

### Task 5: OCR Engine — Image Preprocessing and Tesseract Integration

**Files:**
- Create: `app/src/main/java/com/scanfolio/ocr/OcrResult.kt`
- Create: `app/src/main/java/com/scanfolio/ocr/ImagePreprocessor.kt`
- Create: `app/src/main/java/com/scanfolio/ocr/OcrEngine.kt`

- [ ] **Step 1: Create OcrResult data class**

`app/src/main/java/com/scanfolio/ocr/OcrResult.kt`:
```kotlin
package com.scanfolio.ocr

data class OcrResult(
    val header: List<String>,
    val rows: List<OcrRow>
)

data class OcrRow(
    val stockCode: String,
    val stockName: String,
    val data: Map<String, String>
)

data class OcrCell(
    val text: String,
    val confidence: Float
)
```

- [ ] **Step 2: Create ImagePreprocessor**

`app/src/main/java/com/scanfolio/ocr/ImagePreprocessor.kt`:
```kotlin
package com.scanfolio.ocr

import android.graphics.Bitmap
import android.graphics.Color

object ImagePreprocessor {

    fun toGrayscale(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        for (x in 0 until result.width) {
            for (y in 0 until result.height) {
                val pixel = result.getPixel(x, y)
                val gray = (Color.red(pixel) * 0.299 +
                        Color.green(pixel) * 0.587 +
                        Color.blue(pixel) * 0.114).toInt()
                result.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        return result
    }

    fun binarize(bitmap: Bitmap, threshold: Int = 128): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        for (x in 0 until result.width) {
            for (y in 0 until result.height) {
                val pixel = result.getPixel(x, y)
                val gray = Color.red(pixel)
                val binary = if (gray > threshold) Color.WHITE else Color.BLACK
                result.setPixel(x, y, binary)
            }
        }
        return result
    }

    /**
     * Detect approximate table row positions by scanning for horizontal
     * lines or large gaps between text rows. Returns Y coordinates of row boundaries.
     */
    fun detectRowBoundaries(bitmap: Bitmap): List<Int> {
        val boundaries = mutableListOf(0)
        val rowGapThreshold = (bitmap.height * 0.02).toInt().coerceAtLeast(4)
        var lastNonEmptyRow = 0
        var emptyRun = 0
        for (y in 1 until bitmap.height) {
            val isEmptyRow = isRowMostlyEmpty(bitmap, y)
            if (isEmptyRow) {
                emptyRun++
                if (emptyRun >= rowGapThreshold && lastNonEmptyRow > boundaries.last() + rowGapThreshold) {
                    boundaries.add(y - emptyRun / 2)
                    boundaries.add(y)
                }
            } else {
                emptyRun = 0
                lastNonEmptyRow = y
            }
        }
        boundaries.add(bitmap.height)
        return boundaries.distinct().filter { it < bitmap.height }.sorted()
    }

    private fun isRowMostlyEmpty(bitmap: Bitmap, y: Int, threshold: Float = 0.95f): Boolean {
        var emptyPixels = 0
        for (x in 0 until bitmap.width) {
            if (Color.red(bitmap.getPixel(x, y)) > 200) emptyPixels++
        }
        return emptyPixels.toFloat() / bitmap.width > threshold
    }
}
```

- [ ] **Step 3: Create OcrEngine (Tesseract wrapper)**

`app/src/main/java/com/scanfolio/ocr/OcrEngine.kt`:
```kotlin
package com.scanfolio.ocr

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

class OcrEngine(private val context: Context) {

    private val tessBaseAPI: TessBaseAPI by lazy {
        val datapath = context.filesDir.absolutePath + File.separator + "tesseract"
        val dir = File(datapath + File.separator + "tessdata")
        if (!dir.exists()) dir.mkdirs()
        copyTrainedDataIfNeeded(dir)

        TessBaseAPI().apply {
            init(datapath, "chi_sim")
            setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
        }
    }

    private fun copyTrainedDataIfNeeded(dir: File) {
        val targetFile = File(dir, "chi_sim.traineddata")
        if (!targetFile.exists()) {
            context.assets.open("tessdata/chi_sim.traineddata").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun recognizeText(bitmap: Bitmap): String {
        val processed = ImagePreprocessor.binarize(ImagePreprocessor.toGrayscale(bitmap))
        synchronized(tessBaseAPI) {
            tessBaseAPI.setImage(processed)
            return tessBaseAPI.utF8Text
        }
    }

    fun recognizeRegion(bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int): String {
        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
        return recognizeText(cropped)
    }

    fun release() {
        synchronized(tessBaseAPI) { tessBaseAPI.recycle() }
    }
}
```

- [ ] **Step 4: Commit**

```
git add -A
git commit -m "feat: implement OCR engine with Tesseract integration"
```

---

### Task 6: OCR Engine — Table Analysis and Data Matching

**Files:**
- Create: `app/src/main/java/com/scanfolio/ocr/HeaderRecognizer.kt`
- Create: `app/src/main/java/com/scanfolio/ocr/TableAnalyzer.kt`
- Modify: `app/src/main/java/com/scanfolio/ScanfolioApp.kt` (wire up OCR)

- [ ] **Step 1: Create HeaderRecognizer**

`app/src/main/java/com/scanfolio/ocr/HeaderRecognizer.kt`:
```kotlin
package com.scanfolio.ocr

object HeaderRecognizer {

    /**
     * Match recognized header text against known column definitions.
     * Returns column names in order of appearance.
     */
    fun matchHeaders(
        recognizedHeaders: List<String>,
        knownColumns: List<String>
    ): List<String> {
        return recognizedHeaders.map { header ->
            knownColumns.firstOrNull { known ->
                header.contains(known) || known.contains(header)
            } ?: header
        }
    }
}
```

- [ ] **Step 2: Create TableAnalyzer**

`app/src/main/java/com/scanfolio/ocr/TableAnalyzer.kt`:
```kotlin
package com.scanfolio.ocr

import android.graphics.Bitmap

class TableAnalyzer(private val ocrEngine: OcrEngine) {

    data class TableResult(
        val headers: List<String>,
        val rows: List<OcrRow>
    )

    fun analyze(bitmap: Bitmap): TableResult {
        val rowBoundaries = ImagePreprocessor.detectRowBoundaries(bitmap)
        if (rowBoundaries.size < 4) return TableResult(emptyList(), emptyList())

        val rowHeight = (bitmap.height.toFloat() / rowBoundaries.size).toInt().coerceAtLeast(20)

        // Extract header row
        val headerRow = rowBoundaries.getOrNull(0)?.let { top ->
            rowBoundaries.getOrNull(1)?.let { bottom ->
                val headerBmp = Bitmap.createBitmap(bitmap, 0, top, bitmap.width, bottom - top)
                ocrEngine.recognizeText(headerBmp)
            }
        } ?: ""

        val rawHeaders = parseLineToCells(headerRow)
        val columnCount = rawHeaders.size
        if (columnCount < 2) return TableResult(emptyList(), emptyList())

        val headers = rawHeaders.mapIndexed { index, h ->
            if (index == 0) "代码名称" else h.trim()
        }

        // Extract data rows
        val rows = mutableListOf<OcrRow>()
        for (i in 1 until rowBoundaries.size - 1) {
            val top = rowBoundaries[i]
            val bottom = rowBoundaries[i + 1]
            if (bottom - top < 10) continue

            val rowBmp = Bitmap.createBitmap(bitmap, 0, top, bitmap.width, bottom - top)
            val rowText = ocrEngine.recognizeText(rowBmp)
            val cells = parseLineToCells(rowText)
            if (cells.size < 2) continue

            val codeName = parseCodeAndName(cells[0])
            if (codeName == null) continue

            val dataMap = mutableMapOf<String, String>()
            for (j in 1 until cells.size.coerceAtMost(headers.size)) {
                dataMap[headers[j]] = cells[j].trim()
            }

            rows.add(OcrRow(
                stockCode = codeName.first,
                stockName = codeName.second,
                data = dataMap
            ))
        }

        return TableResult(headers = headers, rows = rows)
    }

    private fun parseLineToCells(text: String): List<String> {
        return text.split("\n")
            .firstOrNull()?.split(Regex("\\s{2,}"))
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    private fun parseCodeAndName(text: String): Pair<String, String>? {
        val clean = text.trim()
        val match = Regex("(\\d{6})\\s*(.+)").find(clean)
        return match?.let {
            Pair(it.groupValues[1], it.groupValues[2].trim())
        }
    }
}
```

- [ ] **Step 3: Update ScanfolioApp to expose OCR**

`app/src/main/java/com/scanfolio/ScanfolioApp.kt`:
```kotlin
package com.scanfolio

import android.app.Application
import com.scanfolio.data.db.AppDatabase
import com.scanfolio.data.repository.*
import com.scanfolio.ocr.*

class ScanfolioApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val stockRepository by lazy { StockRepository(database.stockRecordDao()) }
    val tradeRepository by lazy { TradeRepository(database.tradeRecordDao()) }
    val settingsRepository by lazy {
        SettingsRepository(database.columnDefinitionDao(), database.strategyTypeDao())
    }

    val ocrEngine by lazy { OcrEngine(this) }
    val tableAnalyzer by lazy { TableAnalyzer(ocrEngine) }
}
```

- [ ] **Step 6: Commit**

```
git add -A
git commit -m "feat: implement table analysis and header recognition for OCR"
```

---

### Task 7: Navigation and Utility Components

**Files:**
- Create: `app/src/main/java/com/scanfolio/util/JsonUtils.kt`
- Create: `app/src/main/java/com/scanfolio/util/DateUtils.kt`
- Create: `app/src/main/java/com/scanfolio/util/ExportImportManager.kt`
- Create: `app/src/main/java/com/scanfolio/ui/navigation/AppNavigation.kt`

- [ ] **Step 1: Create JsonUtils**

`app/src/main/java/com/scanfolio/util/JsonUtils.kt`:
```kotlin
package com.scanfolio.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonUtils {
    private val gson = Gson()

    fun toJson(obj: Any): String = gson.toJson(obj)

    inline fun <reified T> fromJson(json: String): T? =
        try { gson.fromJson(json, T::class.java) } catch (_: Exception) { null }

    fun mapToJson(map: Map<String, String>): String = gson.toJson(map)

    fun jsonToMap(json: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return try { gson.fromJson(json, type) } catch (_: Exception) { emptyMap() }
    }
}
```

- [ ] **Step 2: Create DateUtils**

`app/src/main/java/com/scanfolio/util/DateUtils.kt`:
```kotlin
package com.scanfolio.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun formatTimestamp(millis: Long): String = dateFormat.format(Date(millis))

    fun formatDateOnly(millis: Long): String = dateOnlyFormat.format(Date(millis))

    fun parseTimestamp(text: String): Long? {
        return try { dateFormat.parse(text)?.time } catch (_: Exception) { null }
    }

    fun formatDuration(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        return if (days > 0) "${days}天${hours}小时" else "${hours}小时"
    }
}
```

- [ ] **Step 3: Create ExportImportManager**

`app/src/main/java/com/scanfolio/util/ExportImportManager.kt`:
```kotlin
package com.scanfolio.util

import android.content.Context
import com.google.gson.Gson
import com.scanfolio.data.db.entity.*
import com.scanfolio.data.repository.*
import kotlinx.coroutines.flow.first

data class ExportData(
    val version: Int = 1,
    val stockRecords: List<StockRecordEntity>,
    val tradeRecords: List<TradeRecordEntity>,
    val columnDefinitions: List<ColumnDefinitionEntity>,
    val strategyTypes: List<StrategyTypeEntity>,
    val exportTime: Long = System.currentTimeMillis()
)

class ExportImportManager(
    private val context: Context,
    private val stockRepository: StockRepository,
    private val tradeRepository: TradeRepository,
    private val settingsRepository: SettingsRepository
) {
    private val gson = Gson()

    suspend fun exportToJson(): String {
        val stocks = stockRepository.getAll().let { flow ->
            val list = mutableListOf<StockRecordEntity>()
            flow.collect { list.addAll(it) }
            list
        }
        val data = ExportData(
            stockRecords = stocks,
            tradeRecords = tradeRepository.getAll().let { flow ->
                val list = mutableListOf<TradeRecordEntity>()
                flow.collect { list.addAll(it) }
                list
            },
            columnDefinitions = settingsRepository.getAllColumns().let { flow ->
                val list = mutableListOf<ColumnDefinitionEntity>()
                flow.collect { list.addAll(it) }
                list
            },
            strategyTypes = settingsRepository.getStrategies().let { flow ->
                val list = mutableListOf<StrategyTypeEntity>()
                flow.collect { list.addAll(it) }
                list
            }
        )
        return gson.toJson(data)
    }

    suspend fun importFromJson(json: String): Boolean = try {
        val data = gson.fromJson(json, ExportData::class.java) ?: return false
        data.columnDefinitions.forEach { settingsRepository.addColumn(it) }
        data.strategyTypes.forEach { settingsRepository.addStrategy(it.name) }
        data.stockRecords.forEach { stockRepository.insert(it) }
        data.tradeRecords.forEach { tradeRepository.insert(it) }
        true
    } catch (_: Exception) { false }
}
```

- [ ] **Step 4: Create AppNavigation**

`app/src/main/java/com/scanfolio/ui/navigation/AppNavigation.kt`:
```kotlin
package com.scanfolio.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scanfolio.ui.portfolio.PortfolioScreen
import com.scanfolio.ui.scan.ScanScreen
import com.scanfolio.ui.analysis.AnalysisScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Portfolio : Screen("portfolio", "持仓", Icons.Default.List)
    data object Scan : Screen("scan", "扫描", Icons.Default.CameraAlt)
    data object Analysis : Screen("analysis", "分析", Icons.Default.BarChart)
}

val bottomNavItems = listOf(Screen.Portfolio, Screen.Scan, Screen.Analysis)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Portfolio.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Portfolio.route) { PortfolioScreen(navController) }
            composable(Screen.Scan.route) { ScanScreen(navController) }
            composable(Screen.Analysis.route) { AnalysisScreen() }
        }
    }
}
```

- [ ] **Step 5: Commit**

```
git add -A
git commit -m "feat: implement navigation scaffold and utility components"
```

---

### Task 8: Portfolio Screen — Stock List and ViewModel

**Files:**
- Create: `app/src/main/java/com/scanfolio/ui/portfolio/PortfolioScreen.kt`
- Create: `app/src/main/java/com/scanfolio/ui/portfolio/PortfolioViewModel.kt`

- [ ] **Step 1: Create PortfolioViewModel**

`app/src/main/java/com/scanfolio/ui/portfolio/PortfolioViewModel.kt`:
```kotlin
package com.scanfolio.ui.portfolio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.db.entity.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp
    private val stockRepo = app.stockRepository
    private val tradeRepo = app.tradeRepository
    private val settingsRepo = app.settingsRepository

    val stocks: StateFlow<List<StockRecordEntity>> = stockRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledColumns: StateFlow<List<ColumnDefinitionEntity>> = settingsRepo.getEnabledColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteStock(stock: StockRecordEntity) {
        viewModelScope.launch { stockRepo.delete(stock) }
    }
}
```

- [ ] **Step 2: Create PortfolioScreen**

`app/src/main/java/com/scanfolio/ui/portfolio/PortfolioScreen.kt`:
```kotlin
package com.scanfolio.ui.portfolio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.scanfolio.data.db.entity.StockRecordEntity
import com.scanfolio.util.JsonUtils
import com.scanfolio.ScanfolioApp
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    navController: NavController,
    viewModel: PortfolioViewModel = viewModel()
) {
    val stocks by viewModel.stocks.collectAsState()
    val columns by viewModel.enabledColumns.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("持仓列表") },
                actions = {
                    TextButton(onClick = { /* navigate to settings */ }) {
                        Text("设置")
                    }
                }
            )
        }
    ) { padding ->
        if (stocks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无股票数据，请先截图扫描", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(stocks) { stock ->
                    StockListItem(stock = stock, columns = columns) {
                        navController.navigate("stock_detail/${stock.id}")
                    }
                }
            }
        }
    }
}

@Composable
private fun StockListItem(
    stock: StockRecordEntity,
    columns: List<com.scanfolio.data.db.entity.ColumnDefinitionEntity>,
    onClick: () -> Unit
) {
    val dataMap = remember(stock.dataColumns) {
        JsonUtils.jsonToMap(stock.dataColumns)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${stock.code} ${stock.name}",
                style = MaterialTheme.typography.titleMedium
            )
            if (columns.isNotEmpty()) {
                val previewColumns = columns.take(3)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    previewColumns.forEach { col ->
                        val value = dataMap[col.name] ?: "--"
                        Text(
                            text = "${col.name}: $value",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Add stock_detail navigation route to AppNavigation**

In `AppNavigation.kt`, inside the `NavHost` block, add:
```kotlin
composable("stock_detail/{stockId}") { backStackEntry ->
    val stockId = backStackEntry.arguments?.getString("stockId")?.toLongOrNull() ?: return@composable
    StockDetailScreen(stockId = stockId, navController = navController)
}
```

Add import for `StockDetailScreen`.

- [ ] **Step 4: Commit**

```
git add -A
git commit -m "feat: implement portfolio stock list screen"
```

---

### Task 9: Stock Detail and Trade Form

**Files:**
- Create: `app/src/main/java/com/scanfolio/ui/portfolio/StockDetailScreen.kt`
- Create: `app/src/main/java/com/scanfolio/ui/portfolio/TradeFormSheet.kt`

- [ ] **Step 1: Create StockDetailScreen**

`app/src/main/java/com/scanfolio/ui/portfolio/StockDetailScreen.kt`:
```kotlin
package com.scanfolio.ui.portfolio

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.reflect.TypeToken
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.db.entity.*
import com.scanfolio.ui.scan.ScanfolioTheme
import com.scanfolio.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StockDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp
    val stockRepo = app.stockRepository
    val tradeRepo = app.tradeRepository
    val settingsRepo = app.settingsRepository

    private val _stock = MutableStateFlow<StockRecordEntity?>(null)
    val stock: StateFlow<StockRecordEntity?> = _stock.asStateFlow()

    val trades = MutableStateFlow<List<TradeRecordEntity>>(emptyList())
    val columns = settingsRepo.getEnabledColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showTradeForm = MutableStateFlow(false)
    val showTradeForm: StateFlow<Boolean> = _showTradeForm.asStateFlow()

    fun loadStock(stockId: Long) {
        viewModelScope.launch {
            _stock.value = stockRepo.getById(stockId)
            tradeRepo.getByStockId(stockId).collect { trades.value = it }
        }
    }

    fun showTradeForm() { _showTradeForm.value = true }
    fun hideTradeForm() { _showTradeForm.value = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    stockId: Long,
    navController: NavController,
    viewModel: StockDetailViewModel = viewModel()
) {
    LaunchedEffect(stockId) { viewModel.loadStock(stockId) }

    val stock by viewModel.stock.collectAsState()
    val trades by viewModel.trades.collectAsState()
    val columns by viewModel.columns.collectAsState()
    val showForm by viewModel.showTradeForm.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stock?.let { "${it.code} ${it.name}" } ?: "股票详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showTradeForm() }) {
                        Icon(Icons.Default.Add, contentDescription = "添加交易")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Data columns section
            stock?.let { s ->
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("截图数据", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            val dataMap = JsonUtils.jsonToMap(s.dataColumns)
                            columns.forEach { col ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(col.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        dataMap[col.name] ?: "--",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Trade records section
            item {
                Text(
                    "交易记录",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            if (trades.isEmpty()) {
                item {
                    Text(
                        "暂无交易记录",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(trades) { trade ->
                    TradeListItem(trade = trade)
                }
            }
        }
    }

    if (showForm) {
        TradeFormSheet(
            stockId = stockId,
            viewModel = viewModel,
            onDismiss = { viewModel.hideTradeForm() }
        )
    }
}

@Composable
private fun TradeListItem(trade: TradeRecordEntity) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("买入: ${DateUtils.formatTimestamp(trade.buyTime)}")
                Text("￥%.2f".format(trade.buyPrice))
            }
            trade.sellTime?.let { sellTime ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("卖出: ${DateUtils.formatTimestamp(sellTime)}")
                    trade.profitAmount?.let { Text("￥%.2f".format(it)) }
                }
                trade.holdingDuration?.let {
                    Text("持仓: ${DateUtils.formatDuration(it)}")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("战法: ${trade.strategyName}")
                AssistChip(
                    onClick = {},
                    label = { Text(if (trade.isSuccess) "成功" else "失败") }
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create TradeFormSheet**

`app/src/main/java/com/scanfolio/ui/portfolio/TradeFormSheet.kt`:
```kotlin
package com.scanfolio.ui.portfolio

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.scanfolio.data.db.entity.TradeRecordEntity
import com.scanfolio.data.db.entity.StrategyTypeEntity
import com.scanfolio.util.DateUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeFormSheet(
    stockId: Long,
    viewModel: StockDetailViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val strategies by viewModel.settingsRepo.getStrategies()
        .collectAsState(initial = listOf(StrategyTypeEntity(name = "爆量")))

    var buyTime by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var sellTime by remember { mutableStateOf("") }
    var profitRatio by remember { mutableStateOf("") }
    var profitAmount by remember { mutableStateOf("") }
    var strategyName by remember { mutableStateOf(strategies.firstOrNull()?.name ?: "爆量") }
    var isSuccess by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("添加交易记录", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = buyTime,
                    onValueChange = { buyTime = it },
                    label = { Text("买入时间 (yyyy-MM-dd HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = buyPrice,
                    onValueChange = { buyPrice = it },
                    label = { Text("买入价格") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sellTime,
                    onValueChange = { sellTime = it },
                    label = { Text("卖出时间 (yyyy-MM-dd HH:mm, 可选)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = profitRatio,
                    onValueChange = { profitRatio = it },
                    label = { Text("获利比例 (%)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = profitAmount,
                    onValueChange = { profitAmount = it },
                    label = { Text("获利金额") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Strategy selector
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = strategyName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("战法") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        strategies.forEach { strategy ->
                            DropdownMenuItem(
                                text = { Text(strategy.name) },
                                onClick = {
                                    strategyName = strategy.name
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("战法成功")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = isSuccess, onCheckedChange = { isSuccess = it })
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        scope.launch {
                            val buyTimeMs = DateUtils.parseTimestamp(buyTime) ?: return@launch
                            val sellTimeMs = DateUtils.parseTimestamp(sellTime)
                            val duration = if (sellTimeMs != null) (sellTimeMs - buyTimeMs) / 1000 else null

                            viewModel.tradeRepo.insert(
                                TradeRecordEntity(
                                    stockRecordId = stockId,
                                    buyTime = buyTimeMs,
                                    buyPrice = buyPrice.toDoubleOrNull() ?: 0.0,
                                    sellTime = sellTimeMs,
                                    holdingDuration = duration,
                                    profitRatio = profitRatio.toDoubleOrNull(),
                                    profitAmount = profitAmount.toDoubleOrNull(),
                                    strategyName = strategyName,
                                    isSuccess = isSuccess
                                )
                            )
                            onDismiss()
                        }
                    }) { Text("保存") }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```
git add -A
git commit -m "feat: implement stock detail screen and trade form"
```

---

### Task 10: Scan Screen — Capture Screenshot and OCR Preview

**Files:**
- Create: `app/src/main/java/com/scanfolio/ui/scan/ScanScreen.kt`
- Create: `app/src/main/java/com/scanfolio/ui/scan/ScanViewModel.kt`
- Create: `app/src/main/java/com/scanfolio/ui/scan/ScanPreviewScreen.kt`
- Create: `app/src/main/java/com/scanfolio/ui/scan/ScanPreviewViewModel.kt`

- [ ] **Step 1: Create ScanViewModel**

`app/src/main/java/com/scanfolio/ui/scan/ScanViewModel.kt`:
```kotlin
package com.scanfolio.ui.scan

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.ocr.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp

    private val _ocrResult = MutableStateFlow<TableAnalyzer.TableResult?>(null)
    val ocrResult: StateFlow<TableAnalyzer.TableResult?> = _ocrResult.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun processImage(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            try {
                val result = app.tableAnalyzer.analyze(bitmap)
                if (result.rows.isEmpty()) {
                    _error.value = "未识别到有效表格数据，请确认是同花顺股票列表截图"
                }
                _ocrResult.value = result
            } catch (e: Exception) {
                _error.value = "识别失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun confirmImport() {
        viewModelScope.launch {
            val result = _ocrResult.value ?: return@launch
            for (row in result.rows) {
                app.stockRepository.mergeScreenshotData(
                    code = row.stockCode,
                    name = row.stockName,
                    newData = row.data
                )
            }
            _ocrResult.value = null
        }
    }

    fun clearError() { _error.value = null }
}
```

- [ ] **Step 2: Create ScanScreen**

`app/src/main/java/com/scanfolio/ui/scan/ScanScreen.kt`:
```kotlin
package com.scanfolio.ui.scan

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.scanfolio.ScanfolioApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    navController: NavController,
    viewModel: ScanViewModel = viewModel()
) {
    val context = LocalContext.current
    val isProcessing by viewModel.isProcessing.collectAsState()
    val result by viewModel.ocrResult.collectAsState()
    val error by viewModel.error.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.let { bm -> viewModel.processImage(bm) }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { viewModel.processImage(it) }
    }

    LaunchedEffect(result) {
        if (result != null) {
            viewModel.confirmImport()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("截图识别") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("正在识别截图...")
            } else {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "选择同花顺股票列表截图进行识别",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("拍照")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("从相册选择")
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            result?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("识别完成，共 ${it.rows.size} 只股票")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.confirmImport() }) {
                    Text("确认导入")
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```
git add -A
git commit -m "feat: implement screenshot capture and OCR scan screen"
```

---

### Task 11: Analysis Screen — Success/Failure Comparison

**Files:**
- Create: `app/src/main/java/com/scanfolio/ui/analysis/AnalysisScreen.kt`
- Create: `app/src/main/java/com/scanfolio/ui/analysis/AnalysisViewModel.kt`

- [ ] **Step 1: Create AnalysisViewModel**

`app/src/main/java/com/scanfolio/ui/analysis/AnalysisViewModel.kt`:
```kotlin
package com.scanfolio.ui.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.db.entity.*
import com.scanfolio.util.JsonUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AnalysisData(
    val columnName: String,
    val successAvg: Double?,
    val failAvg: Double?,
    val successValues: List<Double>,
    val failValues: List<Double>
)

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp
    private val stockRepo = app.stockRepository
    private val tradeRepo = app.tradeRepository
    private val settingsRepo = app.settingsRepository

    private val _selectedTab = MutableStateFlow(0) // 0=success, 1=fail, 2=compare
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val columns: StateFlow<List<ColumnDefinitionEntity>> = settingsRepo.getEnabledColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _analysisData = MutableStateFlow<List<AnalysisData>>(emptyList())
    val analysisData: StateFlow<List<AnalysisData>> = _analysisData.asStateFlow()

    init { loadAnalysisData() }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    private fun loadAnalysisData() {
        viewModelScope.launch {
            val allTrades = mutableListOf<TradeRecordEntity>()
            tradeRepo.getAll().collect { allTrades.clear(); allTrades.addAll(it) }

            val allStocks = mutableListOf<StockRecordEntity>()
            stockRepo.getAll().collect { allStocks.clear(); allStocks.addAll(it) }

            val stockMap = allStocks.associateBy { it.id }
            val cols = columns.value.ifEmpty { return@launch }

            val successTrades = allTrades.filter { it.isSuccess }
            val failTrades = allTrades.filter { !it.isSuccess }

            val analysis = cols.map { col ->
                val successValues = successTrades.mapNotNull { trade ->
                    stockMap[trade.stockRecordId]
                        ?.let { JsonUtils.jsonToMap(it.dataColumns)[col.name]?.toDoubleOrNull() }
                }
                val failValues = failTrades.mapNotNull { trade ->
                    stockMap[trade.stockRecordId]
                        ?.let { JsonUtils.jsonToMap(it.dataColumns)[col.name]?.toDoubleOrNull() }
                }
                AnalysisData(
                    columnName = col.name,
                    successAvg = successValues.average().takeIf { successValues.isNotEmpty() },
                    failAvg = failValues.average().takeIf { failValues.isNotEmpty() },
                    successValues = successValues,
                    failValues = failValues
                )
            }
            _analysisData.value = analysis
        }
    }
}
```

- [ ] **Step 2: Create AnalysisScreen**

`app/src/main/java/com/scanfolio/ui/analysis/AnalysisScreen.kt`:
```kotlin
package com.scanfolio.ui.analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel = viewModel()) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val analysisData by viewModel.analysisData.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("数据分析") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tab selector
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { viewModel.selectTab(0) }) {
                    Text("成功分析", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { viewModel.selectTab(1) }) {
                    Text("失败分析", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { viewModel.selectTab(2) }) {
                    Text("对比分析", modifier = Modifier.padding(12.dp))
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(analysisData) { data ->
                    AnalysisCard(
                        data = data,
                        mode = selectedTab
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisCard(data: AnalysisData, mode: Int) {
    Card(modifier = Modifier.fillMaxWidth().padding(12.dp, 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(data.columnName, style = MaterialTheme.typography.titleSmall)

            Spacer(modifier = Modifier.height(8.dp))

            when (mode) {
                0 -> {
                    Text("成功组样本数: ${data.successValues.size}")
                    data.successAvg?.let { Text("均值: %.2f".format(it)) }
                }
                1 -> {
                    Text("失败组样本数: ${data.failValues.size}")
                    data.failAvg?.let { Text("均值: %.2f".format(it)) }
                }
                2 -> {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("成功", color = MaterialTheme.colorScheme.primary)
                            Text("样本: ${data.successValues.size}")
                            data.successAvg?.let { Text("均值: %.2f".format(it)) }
                        }
                        Column {
                            Text("失败", color = MaterialTheme.colorScheme.error)
                            Text("样本: ${data.failValues.size}")
                            data.failAvg?.let { Text("均值: %.2f".format(it)) }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```
git add -A
git commit -m "feat: implement analysis screen with success/failure comparison"
```

---

### Task 12: Settings Screens — Column and Strategy Management

**Files:**
- Create: `app/src/main/java/com/scanfolio/ui/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/scanfolio/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/scanfolio/ui/settings/ColumnManageScreen.kt`
- Create: `app/src/main/java/com/scanfolio/ui/settings/StrategyManageScreen.kt`

- [ ] **Step 1: Create SettingsViewModel**

`app/src/main/java/com/scanfolio/ui/settings/SettingsViewModel.kt`:
```kotlin
package com.scanfolio.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanfolio.ScanfolioApp
import com.scanfolio.data.db.entity.*
import com.scanfolio.util.ExportImportManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ScanfolioApp
    val settingsRepo = app.settingsRepository
    val exportImportManager = ExportImportManager(
        app, app.stockRepository, app.tradeRepository, settingsRepo
    )

    val columns: StateFlow<List<ColumnDefinitionEntity>> = settingsRepo.getAllColumns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val strategies: StateFlow<List<StrategyTypeEntity>> = settingsRepo.getStrategies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exportJson = MutableStateFlow<String?>(null)
    val exportJson: StateFlow<String?> = _exportJson.asStateFlow()

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult.asStateFlow()

    fun exportData() {
        viewModelScope.launch {
            _exportJson.value = exportImportManager.exportToJson()
        }
    }

    fun importData(json: String) {
        viewModelScope.launch {
            val success = exportImportManager.importFromJson(json)
            _importResult.value = if (success) "导入成功" else "导入失败"
        }
    }

    fun clearExportResult() { _exportJson.value = null }
    fun clearImportResult() { _importResult.value = null }

    // Default column presets
    fun addDefaultColumns() {
        val defaults = listOf(
            "最新价" to "number", "涨跌幅" to "percentage", "成交量" to "number",
            "20日涨幅" to "percentage", "DDE散户数量" to "number",
            "竞价涨幅" to "percentage", "竞价金额" to "number",
            "个股热度排名" to "number", "人气数值" to "number",
            "涨停次数(年)" to "number", "所属概念" to "text", "所属同花顺行业" to "text"
        )
        viewModelScope.launch {
            defaults.forEachIndexed { index, (name, type) ->
                settingsRepo.addColumn(
                    ColumnDefinitionEntity(name = name, type = type, sortOrder = index)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create SettingsScreen**

`app/src/main/java/com/scanfolio/ui/settings/SettingsScreen.kt`:
```kotlin
package com.scanfolio.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val exportJson by viewModel.exportJson.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    var importText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Data columns
            Text("数据列管理", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = { navController.navigate("column_manage") }) {
                Text("管理数据列 →")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Strategies
            Text("战法管理", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = { navController.navigate("strategy_manage") }) {
                Text("管理战法 →")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data export/import
            Text("数据导入导出", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.exportData() }) {
                Text("导出数据 (JSON)")
            }

            exportJson?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("数据已生成，复制到剪贴板", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Scanfolio Export", it))
                }) {
                    Text("复制到剪贴板")
                }
                TextButton(onClick = { viewModel.clearExportResult() }) {
                    Text("关闭")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                label = { Text("粘贴导入的 JSON 数据") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = { viewModel.importData(importText) }) {
                Text("导入数据")
            }

            importResult?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it)
                TextButton(onClick = { viewModel.clearImportResult() }) {
                    Text("关闭")
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create ColumnManageScreen**

`app/src/main/java/com/scanfolio/ui/settings/ColumnManageScreen.kt`:
```kotlin
package com.scanfolio.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.scanfolio.data.db.entity.ColumnDefinitionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnManageScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val columns by viewModel.columns.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据列管理") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
                    }
                },
                actions = {
                    if (columns.isEmpty()) {
                        TextButton(onClick = { viewModel.addDefaultColumns() }) {
                            Text("添加默认列")
                        }
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(columns) { column ->
                ColumnListItem(column = column, viewModel = viewModel)
            }
        }
    }

    if (showAddDialog) {
        AddColumnDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type ->
                viewModel.settingsRepo.addColumn(
                    ColumnDefinitionEntity(
                        name = name, type = type,
                        sortOrder = columns.size
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ColumnListItem(
    column: ColumnDefinitionEntity,
    viewModel: SettingsViewModel
) {
    Card(modifier = Modifier.fillMaxWidth().padding(12.dp, 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(column.name, style = MaterialTheme.typography.bodyLarge)
                Text("类型: ${column.type}", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = column.enabled,
                onCheckedChange = {
                    viewModel.settingsRepo.updateColumn(column.copy(enabled = it))
                }
            )
        }
    }
}

@Composable
private fun AddColumnDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("number") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加数据列") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("列名") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = type, onValueChange = {},
                        readOnly = true, label = { Text("数据类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("number", "percentage", "text").forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = { type = t; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, type) }, enabled = name.isNotBlank()) {
                Text("添加")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
```

- [ ] **Step 4: Create StrategyManageScreen**

`app/src/main/java/com/scanfolio/ui/settings/StrategyManageScreen.kt`:
```kotlin
package com.scanfolio.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyManageScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val strategies by viewModel.strategies.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("战法管理") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加战法")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(strategies) { strategy ->
                Card(modifier = Modifier.fillMaxWidth().padding(12.dp, 4.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strategy.name, style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = {
                            viewModel.settingsRepo.deleteStrategy(strategy.name)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加战法") },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("战法名称") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.settingsRepo.addStrategy(name)
                    showAddDialog = false
                }, enabled = name.isNotBlank()) {
                    Text("添加")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }
}
```

- [ ] **Step 5: Add settings and sub-screens to navigation**

In `AppNavigation.kt`, add routes for settings and sub-screens:
```kotlin
composable("settings") { SettingsScreen(navController) }
composable("column_manage") { ColumnManageScreen() }
composable("strategy_manage") { StrategyManageScreen() }
```

- [ ] **Step 6: Commit**

```
git add -A
git commit -m "feat: implement settings screens with column and strategy management"
```

---

### Task 13: Tesseract Training Data Setup

**Files:**
- Create: `app/src/main/assets/tessdata/` (directory)

- [ ] **Step 1: Download and place Chinese Tesseract traineddata**

The `chi_sim.traineddata` file must be placed at `app/src/main/assets/tessdata/chi_sim.traineddata`.

Download from: https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata

```bash
# Download the traineddata file
curl -L -o app/src/main/assets/tessdata/chi_sim.traineddata \
  https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata
```

Alternatively, users can manually download the file and place it in the assets directory.

- [ ] **Step 2: Commit**

```
git add -A
git commit -m "feat: add Tesseract Chinese language traineddata"
```

---

## Spec Coverage Verification

| Spec Section | Tasks Implementing It |
|---|---|
| 截图识别模块 (OCR + table analysis + data matching) | Task 5, Task 6 |
| 持仓管理模块 (stock list + detail + trade form) | Task 8, Task 9 |
| 数据分析模块 (success/fail/compare) | Task 11 |
| 设置管理模块 (columns, strategies, export/import) | Task 12 |
| 数据模型 (Room entities, DAOs, Database) | Task 2, Task 3 |
| 数据层 (Repositories) | Task 4 |
| 项目脚手架 (Gradle, Manifest, Theme, Navigation) | Task 1, Task 7 |
| Tesseract 数据文件 | Task 13 |
