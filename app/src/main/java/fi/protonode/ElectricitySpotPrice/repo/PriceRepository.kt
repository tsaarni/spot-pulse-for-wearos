package fi.protonode.ElectricitySpotPrice.repo

import fi.protonode.ElectricitySpotPrice.network.SpotHintaApi
import fi.protonode.ElectricitySpotPrice.storage.ApiCache
import fi.protonode.ElectricitySpotPrice.storage.PriceDataStore
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Container for min/max price statistics for a day. All prices are in euro cents per kWh. */
data class DayPrices(
        val minPrice: Double, // Lowest price in euro cents/kWh
        val maxPrice: Double, // Highest price in euro cents/kWh
        val minHour: Int, // Hour (0-23) when minimum occurs
        val maxHour: Int // Hour (0-23) when maximum occurs
)

/**
 * Repository for electricity spot price data.
 *
 * Handles lazy loading with automatic cache expiry. The cache is fetched on first access and
 * refreshed when the date changes (device timezone).
 *
 * Thread-safe using Mutex to prevent concurrent API calls.
 */
// @Singleton tells Hilt to create a single shared instance of this class.
// @Inject on the constructor lets Hilt know how to build it.
@Singleton
class PriceRepository
@Inject
constructor(private val api: SpotHintaApi, private val priceDataStore: PriceDataStore) {
    // Mutex ensures only one coroutine can fetch at a time (thread safety)
    private val refreshMutex = Mutex()

    /**
     * Ensures cache is fresh and available.
     *
     * How it works (Kotlin primer):
     * - suspend: this function can pause without blocking a thread while it does I/O.
     * - Mutex.withLock: only one coroutine runs the block at a time (prevents duplicate API calls).
     * - We check the cached DTO timestamps. If none belong to "today" (device timezone), we fetch.
     */
    private suspend fun ensureFreshCache() {
        refreshMutex.withLock {
            try {
                val cached = priceDataStore.loadCache()

                // Determine today's date in device timezone
                val today = LocalDate.now()

                // If cache exists and contains at least one item for today, it's fresh
                val hasTodayData =
                        cached?.prices?.any { dto ->
                            val priceTime =
                                    OffsetDateTime.parse(dto.dateTime)
                                            .atZoneSameInstant(ZoneId.systemDefault())
                            priceTime.toLocalDate() == today
                        }
                                ?: false

                if (hasTodayData) {
                    return
                }

                // Cache missing or stale (no today's entries) - fetch from API
                val prices = api.getPrices()
                if (prices.isEmpty()) {
                    // API returned empty list - clear cache and return
                    priceDataStore.clearCache()
                    return
                }

                // Save new cache with raw DTOs
                val newCache = ApiCache(prices = prices)
                priceDataStore.saveCache(newCache)
            } catch (e: Exception) {
                // On any error (network, parsing, etc), clear cache
                // Next access will retry the fetch
                e.printStackTrace()
                priceDataStore.clearCache()
            }
        }
    }

    /**
     * Get current hour's electricity price in euro cents per kWh.
     *
     * Returns null if no data available or on error. The "?:" operator provides a fallback - if
     * left side is null, use right side.
     */
    suspend fun getCurrentPriceCents(): Double? {
        ensureFreshCache()

        val cached = priceDataStore.loadCache() ?: return null

        // Get current hour in device timezone
        val zone = ZoneId.systemDefault() // Device timezone
        val now = OffsetDateTime.now(zone) // Current date+time with offset
        val today = now.toLocalDate()
        val currentHour = now.hour

        // Find price matching current hour
        // The API's dateTime field is parsed to extract the hour
        val currentPrice =
                cached.prices.find { dto ->
                    val priceTime = OffsetDateTime.parse(dto.dateTime).atZoneSameInstant(zone)
                    priceTime.toLocalDate() == today && priceTime.hour == currentHour
                }

        // Convert from EUR/kWh to cents/kWh (multiply by 100)
        // The ?. operator (safe call) handles the case where no match is found
        return currentPrice?.priceWithTax?.let { it * 100.0 }
    }

    /**
     * Helper to get min/max prices for a specific local date in euro cents per kWh.
     */
    private suspend fun getMinMaxForDate(targetDate: LocalDate): DayPrices? {
        ensureFreshCache()

        val cached = priceDataStore.loadCache() ?: return null

        // Filter prices that belong to the target date
        val pricesForDate =
                cached.prices.filter { dto ->
                    val priceTime =
                            OffsetDateTime.parse(dto.dateTime)
                                    .atZoneSameInstant(ZoneId.systemDefault())
                    priceTime.toLocalDate() == targetDate
                }

        if (pricesForDate.isEmpty()) return null

        // minByOrNull/maxByOrNull return null if the list is empty (safe operations)
        val minPrice = pricesForDate.minByOrNull { it.priceWithTax } ?: return null
        val maxPrice = pricesForDate.maxByOrNull { it.priceWithTax } ?: return null

        // Extract hour from datetime for each
        val minHour =
                OffsetDateTime.parse(minPrice.dateTime)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .hour
        val maxHour =
                OffsetDateTime.parse(maxPrice.dateTime)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .hour

        return DayPrices(
                minPrice = minPrice.priceWithTax * 100.0, // Convert to cents
                maxPrice = maxPrice.priceWithTax * 100.0, // Convert to cents
                minHour = minHour,
                maxHour = maxHour
        )
    }

    /**
     * Get min/max prices for today in euro cents per kWh.
     *
     * Returns null if no data available. "Today" is determined by device timezone.
     */
    suspend fun getTodayMinMaxCents(): DayPrices? {
        return getMinMaxForDate(LocalDate.now())
    }

    /**
     * Get min/max prices for tomorrow in euro cents per kWh.
     *
     * Returns null if tomorrow's data not yet available (API provides after 14:00). "Tomorrow" is
     * determined by device timezone.
     */
    suspend fun getTomorrowMinMaxCents(): DayPrices? {
        return getMinMaxForDate(LocalDate.now().plusDays(1))
    }

    /**
     * Get the timestamp of the next price data point after the current time.
     *
     * Returns null if no future data available in cache.
     */
    suspend fun getNextPriceTimestamp(): OffsetDateTime? {
        ensureFreshCache()

        val cached = priceDataStore.loadCache() ?: return null
        val zone = ZoneId.systemDefault()
        val now = OffsetDateTime.now(zone)

        // Find the next price entry after current time
        return cached.prices
                .map { dto ->
                    OffsetDateTime.parse(dto.dateTime).atZoneSameInstant(zone).toOffsetDateTime()
                }
                .filter { it.isAfter(now) }
                .minOrNull()
    }

    /**
     * Force a refresh of price data from the API, ignoring cache.
     *
     * Clears the cache and fetches fresh data from the API. Used when the user manually triggers a
     * refresh via pull-to-refresh gesture.
     */
    suspend fun forceRefresh() {
        refreshMutex.withLock {
            try {
                // Clear existing cache to force fresh API call
                priceDataStore.clearCache()

                // Fetch fresh data from API
                val prices = api.getPrices()
                if (prices.isEmpty()) {
                    // API returned empty list - stay with cleared cache
                    return
                }

                // Save new cache with raw DTOs
                val newCache = ApiCache(prices = prices)
                priceDataStore.saveCache(newCache)
            } catch (e: Exception) {
                // On any error (network, parsing, etc), log it
                // The cache remains cleared, next attempt will retry
                e.printStackTrace()
            }
        }
    }
}
