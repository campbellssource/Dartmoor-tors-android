package com.dartmoortors.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.dartmoortors.data.model.Classification
import com.dartmoortors.data.model.CompendiumEdition
import com.dartmoortors.data.model.TorCollection
import com.dartmoortors.data.model.TorSortOption
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for managing user preferences/settings.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val ENABLED_CLASSIFICATIONS = stringSetPreferencesKey("enabled_classifications")
        val ACCESSIBLE_ONLY = booleanPreferencesKey("accessible_only")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val MAP_TYPE = intPreferencesKey("map_type")
        val SHOW_PHOTOS_LAYER = booleanPreferencesKey("show_photos_layer")
        val SELECTED_COLLECTION_ID = stringPreferencesKey("selected_collection_id")
        val SELECTED_COMPENDIUM_EDITION = stringPreferencesKey("selected_compendium_edition")
    }
    
    /**
     * Get the currently selected collection ID.
     */
    val selectedCollectionId: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_COLLECTION_ID] ?: TorCollection.DEFAULT_COLLECTION_ID
        }

    /**
     * Get the selected compendium edition (defaults to 2nd edition).
     */
    val selectedCompendiumEdition: Flow<CompendiumEdition> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val value = preferences[PreferencesKeys.SELECTED_COMPENDIUM_EDITION]
            if (value != null) {
                CompendiumEdition.fromString(value) ?: CompendiumEdition.SECOND
            } else {
                CompendiumEdition.SECOND
            }
        }
    
    /**
     * Get enabled classifications.
     */
    val enabledClassifications: Flow<Set<Classification>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val names = preferences[PreferencesKeys.ENABLED_CLASSIFICATIONS]
            if (names == null) {
                // First launch - use default enabled classifications
                Classification.entries.filter { it.defaultEnabled }.toSet()
            } else {
                // User has made a choice - honor it (even if empty)
                names.mapNotNull { name ->
                    Classification.entries.find { it.name == name }
                }.toSet()
            }
        }
    
    /**
     * Get accessible only filter state.
     */
    val accessibleOnly: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.ACCESSIBLE_ONLY] ?: true
        }

    /**
     * Get the current sort option.
     */
    val sortOption: Flow<TorSortOption> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val name = preferences[PreferencesKeys.SORT_OPTION]
            if (name != null) {
                TorSortOption.entries.find { it.name == name } ?: TorSortOption.HEIGHT_DESC
            } else {
                TorSortOption.HEIGHT_DESC
            }
        }
    
    /**
     * Get the map type (0=Normal, 1=Satellite, 2=Terrain, 3=Hybrid).
     */
    val mapType: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.MAP_TYPE] ?: 2  // Default to Terrain
        }
    
    /**
     * Get whether to show photos layer on map.
     */
    val showPhotosLayer: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.SHOW_PHOTOS_LAYER] ?: false
        }
    
    /**
     * Update enabled classifications.
     */
    suspend fun setEnabledClassifications(classifications: Set<Classification>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENABLED_CLASSIFICATIONS] = classifications.map { it.name }.toSet()
        }
    }
    
    /**
     * Toggle a classification.
     */
    suspend fun toggleClassification(classification: Classification, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.ENABLED_CLASSIFICATIONS]?.toMutableSet()
                ?: Classification.entries.filter { it.defaultEnabled }.map { it.name }.toMutableSet()
            
            if (enabled) {
                current.add(classification.name)
            } else {
                current.remove(classification.name)
            }
            preferences[PreferencesKeys.ENABLED_CLASSIFICATIONS] = current
        }
    }
    
    /**
     * Update accessible only filter.
     */
    suspend fun setAccessibleOnly(accessibleOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCESSIBLE_ONLY] = accessibleOnly
        }
    }

    /**
     * Update sort option.
     */
    suspend fun setSortOption(sortOption: TorSortOption) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_OPTION] = sortOption.name
        }
    }
    
    /**
     * Update map type.
     */
    suspend fun setMapType(mapType: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAP_TYPE] = mapType
        }
    }
    
    /**
     * Update show photos layer.
     */
    suspend fun setShowPhotosLayer(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_PHOTOS_LAYER] = show
        }
    }
    
    /**
     * Update selected collection ID.
     */
    suspend fun setSelectedCollectionId(collectionId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_COLLECTION_ID] = collectionId
        }
    }

    /**
     * Update selected compendium edition.
     */
    suspend fun setSelectedCompendiumEdition(edition: CompendiumEdition) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_COMPENDIUM_EDITION] = edition.jsonValue
        }
    }
}
