package com.dartmoortors.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a collection of tors from the bundled collections.json data.
 */
data class TorCollection(
    val id: String,
    val name: String,
    val description: String,
    val introduction: String,
    @SerializedName("imageAsset")
    val imageAsset: String?,
    val url: String?,
    @SerializedName("hasSubFilters")
    val hasSubFilters: Boolean,
    @SerializedName("sortOrder")
    val sortOrder: Int
) {
    companion object {
        /**
         * The default collection ID (OS Map).
         */
        const val DEFAULT_COLLECTION_ID = "os-map"
        
        /**
         * The Tors of Dartmoor collection ID - the only one with sub-filters.
         */
        const val TORS_OF_DARTMOOR_ID = "tors-of-dartmoor"
    }
}
