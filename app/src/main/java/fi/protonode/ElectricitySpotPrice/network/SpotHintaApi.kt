package fi.protonode.ElectricitySpotPrice.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface describing HTTP endpoints.
 *
 * - @GET maps a Kotlin function to an HTTP GET request.
 * - suspend means this runs asynchronously using Kotlin coroutines.
 * - @Query appends a query parameter to the URL (e.g., ?priceResolution=60).
 */
interface SpotHintaApi {
    @GET("TodayAndDayForward")
    suspend fun getPrices(@Query("priceResolution") priceResolution: Int = 60): List<SpotPriceDto>
}
