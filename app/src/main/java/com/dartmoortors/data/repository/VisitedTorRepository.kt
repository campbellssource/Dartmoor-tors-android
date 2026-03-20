package com.dartmoortors.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.dartmoortors.data.local.VisitedTorDao
import com.dartmoortors.data.model.VisitedTor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing visited tor data.
 */
@Singleton
class VisitedTorRepository @Inject constructor(
    private val visitedTorDao: VisitedTorDao
) {
    /**
     * Get all visited tors as a Flow.
     */
    fun getVisitedTors(checklistId: String = "default"): Flow<List<VisitedTor>> {
        return visitedTorDao.getVisitedTors(checklistId)
    }
    
    /**
     * Get visited tor IDs as a Flow.
     */
    fun getVisitedTorIds(checklistId: String = "default"): Flow<Set<String>> {
        return visitedTorDao.getVisitedTorIds(checklistId).map { it.toSet() }
    }
    
    /**
     * Get a visited tor by ID.
     */
    suspend fun getVisitedTor(torId: String): VisitedTor? {
        return visitedTorDao.getVisitedTor(torId)
    }
    
    /**
     * Observe a visited tor by ID.
     */
    fun observeVisitedTor(torId: String): Flow<VisitedTor?> {
        return visitedTorDao.observeVisitedTor(torId)
    }
    
    /**
     * Mark a tor as visited.
     */
    suspend fun markAsVisited(torId: String, date: Long = System.currentTimeMillis(), checklistId: String = "default") {
        visitedTorDao.insertVisitedTor(
            VisitedTor(
                torId = torId,
                visitedDate = date,
                checklistId = checklistId
            )
        )
    }
    
    /**
     * Unmark a tor as visited.
     */
    suspend fun unmarkAsVisited(torId: String) {
        visitedTorDao.deleteByTorId(torId)
    }
    
    /**
     * Update the visited date for a tor.
     */
    suspend fun updateVisitedDate(torId: String, date: Long) {
        visitedTorDao.updateVisitedDate(torId, date)
    }
    
    /**
     * Associate a photo with a visited tor.
     */
    suspend fun setPhoto(torId: String, photoUri: String?) {
        visitedTorDao.updatePhotoUri(torId, photoUri)
    }
    
    /**
     * Get all visited tors as a one-time list (not Flow).
     */
    suspend fun getVisitedTorsOnce(checklistId: String = "default"): List<VisitedTor> {
        return visitedTorDao.getVisitedTorsOnce(checklistId)
    }
    
    /**
     * Get the count of visited tors.
     */
    fun getVisitedCount(checklistId: String = "default"): Flow<Int> {
        return visitedTorDao.getVisitedCount(checklistId)
    }
}
