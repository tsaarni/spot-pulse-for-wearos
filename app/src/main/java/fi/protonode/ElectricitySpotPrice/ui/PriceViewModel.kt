package fi.protonode.ElectricitySpotPrice.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import fi.protonode.ElectricitySpotPrice.repo.DayPrices
import fi.protonode.ElectricitySpotPrice.repo.PriceRepository
import javax.inject.Inject

/**
 * ViewModel for the price screen.
 *
 * - @HiltViewModel lets Hilt create this ViewModel and satisfy its constructor dependency
 * (PriceRepository). In Compose, you obtain it with hiltViewModel() without writing a factory.
 * - We expose simple suspend functions the UI calls on demand instead of using flows. This keeps
 * the data flow explicit and easy to follow.
 */
@HiltViewModel
class PriceViewModel @Inject constructor(private val repository: PriceRepository) : ViewModel() {

    /**
     * Get current electricity price in euro cents per kWh.
     *
     * Returns null if data unavailable or on error. This is a suspend function - it can only be
     * called from coroutines (like LaunchedEffect in Compose).
     */
    suspend fun getCurrentPriceCents(): Double? {
        return repository.getCurrentPriceCents()
    }

    /**
     * Get today's min/max prices in euro cents per kWh.
     *
     * Returns null if data unavailable.
     */
    suspend fun getTodayMinMaxCents(): DayPrices? {
        return repository.getTodayMinMaxCents()
    }

    /**
     * Get tomorrow's min/max prices in euro cents per kWh.
     *
     * Returns null if tomorrow's data not yet available.
     */
    suspend fun getTomorrowMinMaxCents(): DayPrices? {
        return repository.getTomorrowMinMaxCents()
    }

    /**
     * Manually refresh price data from the REST API.
     *
     * Clears the cache and fetches fresh data. Returns a triple of (currentPrice, todayPrices,
     * tomorrowPrices). This is called when the user triggers a pull-to-refresh gesture.
     */
    suspend fun refreshPrices(): Triple<Double?, DayPrices?, DayPrices?> {
        repository.forceRefresh()
        val currentPrice = repository.getCurrentPriceCents()
        val todayPrices = repository.getTodayMinMaxCents()
        val tomorrowPrices = repository.getTomorrowMinMaxCents()
        return Triple(currentPrice, todayPrices, tomorrowPrices)
    }
}
