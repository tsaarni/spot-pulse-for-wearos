package fi.protonode.ElectricitySpotPrice.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.protonode.ElectricitySpotPrice.network.SpotPriceDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

// Extension property to create a DataStore instance for storing price data.
// The "by" keyword is a Kotlin delegation pattern - it creates the DataStore lazily.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "price_cache")

/**
 * Simple cache container holding the raw API response data.
 *
 * @param prices List of price DTOs directly from the API (today + tomorrow if available)
 * @param fetchDate The date when this data was fetched, in "yyyy-MM-dd" format
 */
data class ApiCache(val prices: List<SpotPriceDto>)

/**
 * Data store layer for caching electricity price data.
 *
 * Uses Android DataStore (key-value storage) to persist API responses as JSON. All operations are
 * suspend functions - they can only be called from coroutines or other suspend functions, ensuring
 * safe background execution.
 */
// @Singleton creates one shared instance for the whole app.
// @Inject constructor allows Hilt to provide this wherever needed.
@Singleton
class PriceDataStore @Inject constructor(@ApplicationContext private val context: Context) {
    // Gson is a library that converts Kotlin objects to/from JSON strings
    private val gson = Gson()

    // Key used to store/retrieve the cache from DataStore
    private val PRICE_CACHE_KEY = stringPreferencesKey("price_cache")

    /**
     * Load the cached price data.
     *
     * Returns null if no cache exists or if the JSON is corrupted. The "suspend" keyword means this
     * function must be called from a coroutine.
     */
    suspend fun loadCache(): ApiCache? {
        // Read the current preferences snapshot from DataStore.
        // .first() retrieves one value and then stops (no streaming).
        val preferences = context.dataStore.data.first()

        // The "?." is the safe call operator - if left side is null, the whole expression is null
        return preferences[PRICE_CACHE_KEY]?.let { json ->
            try {
                // TypeToken helps Gson understand generic types like List<SpotPriceDto>
                val type = object : TypeToken<ApiCache>() {}.type
                gson.fromJson<ApiCache>(json, type)
            } catch (e: Exception) {
                // If JSON is corrupted, return null
                null
            }
        }
    }

    /**
     * Save price data to cache.
     *
     * Serializes the cache to JSON and stores it in DataStore. The suspend keyword means this runs
     * on a background thread.
     */
    suspend fun saveCache(cache: ApiCache) {
        // edit {} is a transaction - all changes happen atomically
        context.dataStore.edit { preferences -> preferences[PRICE_CACHE_KEY] = gson.toJson(cache) }
    }

    /**
     * Clear all cached data.
     *
     * Used when the cache is stale or on API errors.
     */
    suspend fun clearCache() {
        context.dataStore.edit { preferences -> preferences.remove(PRICE_CACHE_KEY) }
    }
}
