package com.dartmoortors.di

import android.content.Context
import androidx.room.Room
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
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideVisitedTorDao(database: DartmoorDatabase): VisitedTorDao {
        return database.visitedTorDao()
    }
}
