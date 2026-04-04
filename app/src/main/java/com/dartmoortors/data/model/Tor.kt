package com.dartmoortors.data.model

import com.google.gson.annotations.SerializedName

/**
 * Compendium edition - 1st or 2nd edition of the Dartmoor Compendium.
 */
enum class CompendiumEdition(val displayName: String, val jsonValue: String) {
    FIRST("1st Edition", "1st"),
    SECOND("2nd Edition", "2nd");

    companion object {
        fun fromString(value: String): CompendiumEdition? {
            return entries.find { it.jsonValue == value }
        }
    }
}

/**
 * Information about a tor's inclusion in the Dartmoor Compendium.
 */
data class CompendiumInfo(
    val editions: List<String>,
    val name: String? = null,
    val firstEditionName: String? = null,
    val secondEditionName: String? = null
) {
    /**
     * Returns true if this tor is in the specified edition.
     */
    fun isInEdition(edition: CompendiumEdition): Boolean {
        return editions.contains(edition.jsonValue)
    }

    /**
     * Returns the display name for a specific edition.
     */
    fun nameFor(edition: CompendiumEdition): String? {
        return when (edition) {
            CompendiumEdition.FIRST -> firstEditionName ?: name
            CompendiumEdition.SECOND -> secondEditionName ?: name
        }
    }
}

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
    val collections: List<String> = emptyList(),
    val compendiumInfo: CompendiumInfo? = null
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

    /**
     * Returns true if this tor is in the specified compendium edition.
     */
    fun isInCompendiumEdition(edition: CompendiumEdition): Boolean {
        return compendiumInfo?.isInEdition(edition) ?: false
    }
}

/**
 * Tor classifications from torsofdartmoor.co.uk
 */
enum class Classification(val displayName: String, val defaultEnabled: Boolean) {
    // All types enabled by default on first selection of Tors of Dartmoor collection
    SUMMIT("Summit", true),
    SUMMIT_AVENUE("Summit Avenue", true),
    VALLEY_SIDE("Valley Side", true),
    SPUR("Spur", true),
    EMERGENT("Emergent", true),
    SMALL("Small", true),
    RUINED("Ruined", true),
    CLITTER("Clitter", true),
    GORGE("Gorge", true),
    GULLY("Gully", true),
    ARTIFICIAL("Artificial", true),
    BOULDER("Boulder", true),
    GLACIAL_REMAINS("Glacial Remains", true),
    UNKNOWN("Unknown", true);
    
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
