package com.dartmoortors.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a visited tor record stored in the local database.
 */
@Entity(
    tableName = "visited_tors",
    indices = [
        Index(value = ["torId"], unique = true),
        Index(value = ["checklistId"]),
        Index(value = ["checklistId", "torId"])
    ]
)
data class VisitedTor(
    @PrimaryKey
    val torId: String,
    val visitedDate: Long, // Timestamp in milliseconds
    val photoUri: String? = null,
    val photoCloudId: String? = null, // For cross-device sync
    val checklistId: String = "default"
)

/**
 * Represents a checklist for grouping visited tors.
 */
@Entity(tableName = "checklists")
data class Checklist(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
