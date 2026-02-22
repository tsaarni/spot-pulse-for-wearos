package fi.protonode.ElectricitySpotPrice.ui

import fi.protonode.ElectricitySpotPrice.repo.DayPrices
import fi.protonode.ElectricitySpotPrice.repo.PriceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PriceViewModelTest {

    private lateinit var repository: PriceRepository
    private lateinit var viewModel: PriceViewModel

    @Before
    fun setup() {
        repository = mockk()
        viewModel = PriceViewModel(repository)
    }

    @Test
    fun `getCurrentPriceCents returns value from repository`() = runTest {
        val expectedPrice = 5.0
        coEvery { repository.getCurrentPriceCents() } returns expectedPrice

        val actualPrice = viewModel.getCurrentPriceCents()

        assertEquals(expectedPrice, actualPrice)
        coVerify(exactly = 1) { repository.getCurrentPriceCents() }
    }

    @Test
    fun `getCurrentPriceCents returns null when repository returns null`() = runTest {
        coEvery { repository.getCurrentPriceCents() } returns null

        val actualPrice = viewModel.getCurrentPriceCents()

        assertNull(actualPrice)
        coVerify(exactly = 1) { repository.getCurrentPriceCents() }
    }

    @Test
    fun `getTodayMinMaxCents returns value from repository`() = runTest {
        val expectedPrices = DayPrices(1.0, 10.0, 2, 20)
        coEvery { repository.getTodayMinMaxCents() } returns expectedPrices

        val actualPrices = viewModel.getTodayMinMaxCents()

        assertEquals(expectedPrices, actualPrices)
        coVerify(exactly = 1) { repository.getTodayMinMaxCents() }
    }

    @Test
    fun `getTomorrowMinMaxCents returns value from repository`() = runTest {
        val expectedPrices = DayPrices(2.0, 15.0, 3, 21)
        coEvery { repository.getTomorrowMinMaxCents() } returns expectedPrices

        val actualPrices = viewModel.getTomorrowMinMaxCents()

        assertEquals(expectedPrices, actualPrices)
        coVerify(exactly = 1) { repository.getTomorrowMinMaxCents() }
    }

    @Test
    fun `refreshPrices forces refresh and returns all values`() = runTest {
        val currentPrice = 7.5
        val todayPrices = DayPrices(1.0, 10.0, 2, 20)
        val tomorrowPrices = DayPrices(2.0, 15.0, 3, 21)

        coEvery { repository.forceRefresh() } returns Unit
        coEvery { repository.getCurrentPriceCents() } returns currentPrice
        coEvery { repository.getTodayMinMaxCents() } returns todayPrices
        coEvery { repository.getTomorrowMinMaxCents() } returns tomorrowPrices

        val result = viewModel.refreshPrices()

        assertEquals(Triple(currentPrice, todayPrices, tomorrowPrices), result)

        coVerify(exactly = 1) { repository.forceRefresh() }
        coVerify(exactly = 1) { repository.getCurrentPriceCents() }
        coVerify(exactly = 1) { repository.getTodayMinMaxCents() }
        coVerify(exactly = 1) { repository.getTomorrowMinMaxCents() }
    }
}
