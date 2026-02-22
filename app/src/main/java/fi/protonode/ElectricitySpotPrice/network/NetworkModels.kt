package fi.protonode.ElectricitySpotPrice.network

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object (DTO) representing one price sample from the API.
 *
 * - @SerializedName maps JSON fields (PascalCase from API) to Kotlin properties.
 * - This class is intentionally simple so we can cache the raw response as-is.
 */
data class SpotPriceDto(
        // The rank of the price compared to other prices that day (1 = lowest, 24 = highest).
        @SerializedName("Rank") val rank: Int,

        // The date and time of the price in ISO 8601 format.
        @SerializedName("DateTime") val dateTime: String,

        // Price in EUR/kWh without tax.
        @SerializedName("PriceNoTax") val priceNoTax: Double,

        // Price in EUR/kWh with tax.
        @SerializedName("PriceWithTax") val priceWithTax: Double
)
