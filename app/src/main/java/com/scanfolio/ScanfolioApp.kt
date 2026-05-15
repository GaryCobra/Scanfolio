package com.scanfolio

import android.app.Application
import com.scanfolio.data.db.AppDatabase

class ScanfolioApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
