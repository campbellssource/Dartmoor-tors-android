package com.dartmoortors.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dartmoortors.data.model.VisitedTor
import com.dartmoortors.data.model.Checklist

@Database(
    entities = [VisitedTor::class, Checklist::class],
    version = 1,
    exportSchema = false
)
abstract class DartmoorDatabase : RoomDatabase() {
    abstract fun visitedTorDao(): VisitedTorDao
    
    companion object {
        const val DATABASE_NAME = "dartmoor_tors.db"
    }
}
