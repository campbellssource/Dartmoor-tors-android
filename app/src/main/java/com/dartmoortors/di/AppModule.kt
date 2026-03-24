package com.dartmoortors.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.dartmoortors.data.local.DartmoorDatabase
import com.dartmoortors.data.local.VisitedTorDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Migration from version 1 to 2: Add indices for better query performance.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create indices on visited_tors table for faster queries
            // torId index must be UNIQUE to match the entity definition
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_visited_tors_torId` ON `visited_tors` (`torId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_visited_tors_checklistId` ON `visited_tors` (`checklistId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_visited_tors_checklistId_torId` ON `visited_tors` (`checklistId`, `torId`)")
        }
    }
    
    /**
     * Migration from version 2 to 3: Fix torId index to be unique.
     * Version 2 incorrectly created a non-unique index.
     */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Drop the incorrect non-unique index and create unique one
            database.execSQL("DROP INDEX IF EXISTS `index_visited_tors_torId`")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_visited_tors_torId` ON `visited_tors` (`torId`)")
        }
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): DartmoorDatabase {
        return Room.databaseBuilder(
            context,
            DartmoorDatabase::class.java,
            DartmoorDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideVisitedTorDao(database: DartmoorDatabase): VisitedTorDao {
        return database.visitedTorDao()
    }
}
