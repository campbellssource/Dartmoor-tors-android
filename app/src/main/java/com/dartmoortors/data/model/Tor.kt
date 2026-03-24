package com.dartmoortors.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a tor from the bundled JSON data.
 */
data class Tor(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("heightMeters")
    val heightMeters: Int,
    @SerializedName("osGridRef")
    val osGridRef: String,
    val classification: String,
    val access: String,
    val parish: String?,
    val rockType: String?,
    @SerializedName("torsOfDartmoorURL")
    val torsOfDartmoorURL: String?,
    @SerializedName("wikipediaURL")
    val wikipediaURL: String?,
    val collections: List<String> = emptyList()
) {
    // Cached enum values - computed once per access, stored in transient fields
    // Note: Can't use `by lazy` with Gson deserialization
    @Transient
    private var _classificationEnum: Classification? = null
    
    @Transient
    private var _accessEnum: Access? = null
    
    /**
     * Returns the classification enum value (cached for performance).
     */
    val classificationEnum: Classification
        get() = _classificationEnum ?: Classification.fromString(classification).also { _classificationEnum = it }
    
    /**
     * Returns the access enum value (cached for performance).
     */
    val accessEnum: Access
        get() = _accessEnum ?: Access.fromString(access).also { _accessEnum = it }
    
    /**
     * Returns true if this tor is accessible (can be legally visited).
     */
    val isAccessible: Boolean
        get() = accessEnum.isAccessible
    
    /**
     * Returns true if this tor belongs to the specified collection.
     */
    fun isInCollection(collectionId: String): Boolean {
        return collections.contains(collectionId)
    }
}

/**
 * Tor classifications from torsofdartmoor.co.uk
 */
enum class Classification(val displayName: String, val defaultEnabled: Boolean) {
    SUMMIT("Summit", true),
    SUMMIT_AVENUE("Summit Avenue", true),
    VALLEY_SIDE("Valley Side", true),
    SPUR("Spur", false),
    EMERGENT("Emergent", false),
    SMALL("Small", false),
    RUINED("Ruined", false),
    CLITTER("Clitter", false),
    GORGE("Gorge", false),
    GULLY("Gully", false),
    ARTIFICIAL("Artificial", false),
    BOULDER("Boulder", false),
    GLACIAL_REMAINS("Glacial Remains", false),
    UNKNOWN("Unknown", false);
    
    companion object {
        fun fromString(value: String): Classification {
            return entries.find { 
                it.displayName.equals(value, ignoreCase = true) ||
                it.name.equals(value.replace(" ", "_"), ignoreCase = true)
            } ?: UNKNOWN
        }
    }
}

/**
 * Access levels for tors.
 */
enum class Access(val displayName: String, val isAccessible: Boolean) {
    PUBLIC("Public", true),
    PUBLIC_PART_PRIVATE("Public (part private)", true),
    PRIVATE_ACCESSIBLE("Private (but accessible)", true),
    PRIVATE_FEE("Private (fee required)", true),
    PRIVATE_VISIBLE("Private (visible only)", false),
    PRIVATE_PERMISSION("Private (seek permission)", false),
    PRIVATE_NO_ACCESS("Private (no access)", false),
    UNKNOWN("Unknown", false);
    
    companion object {
        fun fromString(value: String): Access {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
