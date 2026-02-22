package fi.protonode.ElectricitySpotPrice.repo

import fi.protonode.ElectricitySpotPrice.network.SpotHintaApi
import fi.protonode.ElectricitySpotPrice.network.SpotPriceDto
import fi.protonode.ElectricitySpotPrice.storage.ApiCache
import fi.protonode.ElectricitySpotPrice.storage.PriceDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

class PriceRepositoryTest {

    private lateinit var api: SpotHintaApi
    private lateinit var dataStore: PriceDataStore
    private lateinit var repository: PriceRepository

    @Before
    fun setup() {
        api = mockk()
        dataStore = mockk(relaxed = true)
        repository = PriceRepository(api, dataStore)
    }

    private fun createDto(hour: Int, date: LocalDate, priceWithTax: Double): SpotPriceDto {
        val dateTimeString = date.atTime(hour, 0).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()
        return SpotPriceDto(
            rank = 1,
            dateTime = dateTimeString,
            priceNoTax = priceWithTax / 1.24,
            priceWithTax = priceWithTax
        )
    }

    @Test
    fun `getTodayMinMaxCents returns correct min and max when cache is fresh`() = runTest {
        val today = LocalDate.now()
        val d1 = createDto(2, today, 0.10) // min
        val d2 = createDto(18, today, 0.50) // max
        val d3 = createDto(12, today, 0.25)

        val cache = ApiCache(listOf(d1, d2, d3))
        coEvery { dataStore.loadCache() } returns cache

        val result = repository.getTodayMinMaxCents()

        assertNotNull(result)
        assertEquals(10.0, result!!.minPrice, 0.01)
        assertEquals(50.0, result.maxPrice, 0.01)
        assertEquals(2, result.minHour)
        assertEquals(18, result.maxHour)
    }

    @Test
    fun `getTomorrowMinMaxCents returns correct min and max`() = runTest {
        val today = LocalDate.now() // to establish "freshness"
        val d0 = createDto(1, today, 0.20)
        val tomorrow = LocalDate.now().plusDays(1)
        val d1 = createDto(3, tomorrow, 0.05) // min
        val d2 = createDto(19, tomorrow, 0.60) // max
        val d3 = createDto(12, tomorrow, 0.30)

        val cache = ApiCache(listOf(d0, d1, d2, d3))
        coEvery { dataStore.loadCache() } returns cache

        val result = repository.getTomorrowMinMaxCents()

        assertNotNull(result)
        assertEquals(5.0, result!!.minPrice, 0.01)
        assertEquals(60.0, result.maxPrice, 0.01)
        assertEquals(3, result.minHour)
        assertEquals(19, result.maxHour)
    }

    @Test
    fun `forceRefresh clears cache and calls API`() = runTest {
        val today = LocalDate.now()
        val d1 = createDto(2, today, 0.10)
        coEvery { api.getPrices(any()) } returns listOf(d1)

        repository.forceRefresh()

        coVerify { dataStore.clearCache() }
        coVerify { api.getPrices(any()) }
        coVerify { dataStore.saveCache(any()) }
    }
}
