package fi.protonode.ElectricitySpotPrice.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import fi.protonode.ElectricitySpotPrice.repo.DayPrices
import fi.protonode.ElectricitySpotPrice.repo.PriceRepository
import fi.protonode.ElectricitySpotPrice.ui.PriceViewModel
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class CurrentPriceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysCurrentPriceAndMinMaxSuccessfully() {
        // Setup mock repository and viewmodel
        val repository = mockk<PriceRepository>(relaxed = true)
        val viewModel = PriceViewModel(repository)

        // Given
        coEvery { repository.getCurrentPriceCents() } returns 12.34
        coEvery { repository.getTodayMinMaxCents() } returns DayPrices(minPrice = 5.0, maxPrice = 25.0, minHour = 2, maxHour = 18)
        coEvery { repository.getTomorrowMinMaxCents() } returns DayPrices(minPrice = 4.0, maxPrice = 30.0, minHour = 3, maxHour = 19)

        // When
        composeTestRule.setContent {
            CurrentPriceScreen(viewModel = viewModel)
        }

        // Allow LaunchedEffect to run and populate data
        composeTestRule.waitForIdle()

        // Then verify Current Price components
        composeTestRule.onNodeWithText("12.34").assertIsDisplayed()
        composeTestRule.onNodeWithText("c/kWh").assertIsDisplayed()

        // Verify Today Min/Max prices and hours (formatted to 2 decimal places e.g. "5.00")
        composeTestRule.onNodeWithText("5.00", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("2:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("25.00", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("18:00").assertIsDisplayed()

        // Verify Tomorrow Min/Max prices and hours
        composeTestRule.onNodeWithText("4.00", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("3:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("30.00", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("19:00").assertIsDisplayed()
    }
}
