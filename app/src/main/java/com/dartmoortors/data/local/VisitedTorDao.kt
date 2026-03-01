package com.dartmoortors.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow
import com.dartmoortors.data.model.VisitedTor

@Dao
interface VisitedTorDao {
    @Query("SELECT * FROM visited_tors WHERE checklistId = :checklistId")
    fun getVisitedTors(checklistId: String = "default"): Flow<List<VisitedTor>>
    
    @Query("SELECT * FROM visited_tors WHERE torId = :torId LIMIT 1")
    suspend fun getVisitedTor(torId: String): VisitedTor?
    
    @Query("SELECT * FROM visited_tors WHERE torId = :torId LIMIT 1")
    fun observeVisitedTor(torId: String): Flow<VisitedTor?>
    
    @Query("SELECT torId FROM visited_tors WHERE checklistId = :checklistId")
    fun getVisitedTorIds(checklistId: String = "default"): Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitedTor(visitedTor: VisitedTor)
    
    @Delete
    suspend fun deleteVisitedTor(visitedTor: VisitedTor)
    
    @Query("DELETE FROM visited_tors WHERE torId = :torId")
    suspend fun deleteByTorId(torId: String)
    
    @Query("UPDATE visited_tors SET visitedDate = :date WHERE torId = :torId")
    suspend fun updateVisitedDate(torId: String, date: Long)
    
    @Query("UPDATE visited_tors SET photoUri = :photoUri WHERE torId = :torId")
    suspend fun updatePhotoUri(torId: String, photoUri: String?)
    
    @Query("SELECT COUNT(*) FROM visited_tors WHERE checklistId = :checklistId")
    fun getVisitedCount(checklistId: String = "default"): Flow<Int>
}
