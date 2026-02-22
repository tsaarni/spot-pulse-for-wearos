package fi.protonode.ElectricitySpotPrice.ui.screens

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import fi.protonode.ElectricitySpotPrice.R
import fi.protonode.ElectricitySpotPrice.repo.DayPrices
import fi.protonode.ElectricitySpotPrice.ui.PriceViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Wear OS UI built with Jetpack Compose.
 *
 * Compose primer:
 * - @Composable marks a function that describes UI.
 * - remember { mutableStateOf(...) } holds state that survives recomposition; changing it causes
 * the UI to recompose.
 * - LaunchedEffect(Unit) runs a coroutine when the composable first enters the composition (good
 * place to call suspend functions).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentPriceScreen(viewModel: PriceViewModel = hiltViewModel()) {
        // Local state for UI data
        var currentPrice by remember { mutableStateOf<Double?>(null) }
        var todayPrices by remember { mutableStateOf<DayPrices?>(null) }
        var tomorrowPrices by remember { mutableStateOf<DayPrices?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        // Pull-to-refresh state
        var isRefreshing by remember { mutableStateOf(false) }
        var rotaryScrollAccumulator by remember { mutableStateOf(0f) }
        val state = rememberPullToRefreshState()

        // Format numbers according to the device locale (e.g., decimal separators)
        val numberFormat =
                NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                        minimumFractionDigits = 2
                        maximumFractionDigits = 2
                }
        val minMaxStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 26.sp)
        val timeStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp)

        // Wear OS Scrolling Setup:
        // ScrollState tracks the current scroll position and provides scrollBy() method
        val scrollState = rememberScrollState()

        // FocusRequester is needed to claim focus for this composable so it can
        // receive rotary input events from the watch crown/bezel
        val focusRequester = remember { FocusRequester() }

        // CoroutineScope for launching scroll animations from rotary events
        val coroutineScope = rememberCoroutineScope()

        // Get the lifecycle owner to observe lifecycle events
        val lifecycleOwner = LocalLifecycleOwner.current

        // LaunchedEffect that responds to lifecycle events
        // This ensures data is refreshed when the screen comes back into view
        // (e.g., when returning from the tile or when the app resumes)
        LaunchedEffect(lifecycleOwner) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        // This block runs every time the lifecycle reaches STARTED state
                        // (when the screen becomes visible to the user)
                        isLoading = true
                        currentPrice = viewModel.getCurrentPriceCents()
                        todayPrices = viewModel.getTodayMinMaxCents()
                        tomorrowPrices = viewModel.getTomorrowMinMaxCents()
                        isLoading = false
                }
        }

        // Handle refresh trigger
        LaunchedEffect(isRefreshing) {
                if (isRefreshing) {
                        val (newCurrent, newToday, newTomorrow) = viewModel.refreshPrices()
                        currentPrice = newCurrent
                        todayPrices = newToday
                        tomorrowPrices = newTomorrow
                        isRefreshing = false
                }
        }

        // Request rotary focus only after the scrollable content is on screen
        LaunchedEffect(isLoading) {
                if (!isLoading) {
                        withFrameNanos { /* wait for the UI to attach */}
                        focusRequester.requestFocus()
                }
        }

        // Show loading state while fetching
        if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        } else {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .pullToRefresh(
                                                state = state,
                                                isRefreshing = isRefreshing,
                                                onRefresh = { isRefreshing = true },
                                                threshold = 40.dp
                                        )
                ) {
                        Column(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .onRotaryScrollEvent {
                                                        coroutineScope.launch {
                                                                val delta = it.verticalScrollPixels
                                                                if (!isRefreshing &&
                                                                                delta < 0 &&
                                                                                scrollState.value ==
                                                                                        0
                                                                ) {
                                                                        rotaryScrollAccumulator -=
                                                                                delta
                                                                        if (rotaryScrollAccumulator >
                                                                                        300f
                                                                        ) {
                                                                                isRefreshing = true
                                                                                rotaryScrollAccumulator =
                                                                                        0f
                                                                        }
                                                                } else {
                                                                        rotaryScrollAccumulator = 0f
                                                                        scrollState.scrollBy(delta)
                                                                }
                                                        }
                                                        true
                                                }
                                                .focusRequester(focusRequester)
                                                .focusable()
                                                .verticalScroll(scrollState)
                                                .padding(horizontal = 8.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                        ) {
                                // --- Current Price Section ---
                                Text(
                                        text = stringResource(R.string.tile_current_price),
                                        style =
                                                MaterialTheme.typography.labelMedium.copy(
                                                        fontSize = 16.sp
                                                ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (currentPrice != null) {
                                        Text(
                                                text = "${numberFormat.format(currentPrice)}",
                                                style =
                                                        MaterialTheme.typography.displayMedium.copy(
                                                                fontSize = 50.sp
                                                        ),
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                                text = "c/kWh",
                                                style =
                                                        MaterialTheme.typography.titleMedium.copy(
                                                                fontSize = 20.sp
                                                        ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                } else {
                                        Text(
                                                text = "--",
                                                style = MaterialTheme.typography.displayMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // --- Today's Min/Max Section ---
                                MinMaxSection(
                                        title = stringResource(R.string.tile_today),
                                        dayPrices = todayPrices,
                                        numberFormat = numberFormat,
                                        minMaxStyle = minMaxStyle,
                                        timeStyle = timeStyle
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // --- Tomorrow's Min/Max Section ---
                                MinMaxSection(
                                        title = stringResource(R.string.tile_tomorrow),
                                        dayPrices = tomorrowPrices,
                                        numberFormat = numberFormat,
                                        minMaxStyle = minMaxStyle,
                                        timeStyle = timeStyle
                                )

                                Spacer(modifier = Modifier.height(32.dp))
                        }

                        PullToRefreshDefaults.Indicator(
                                state = state,
                                isRefreshing = isRefreshing,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopCenter),
                        )
                }
        }
}

@Composable
private fun MinMaxSection(
        title: String,
        dayPrices: DayPrices?,
        numberFormat: NumberFormat,
        minMaxStyle: androidx.compose.ui.text.TextStyle,
        timeStyle: androidx.compose.ui.text.TextStyle
) {
        Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (dayPrices != null) {
                Row(
                        modifier = Modifier.width(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                        text = "Min",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                                        color = Color.Green
                                )
                                Text(
                                        text = numberFormat.format(dayPrices.minPrice),
                                        style = minMaxStyle,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                        text = "${dayPrices.minHour}:00",
                                        style = timeStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                        text = "Max",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                                        color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                        text = numberFormat.format(dayPrices.maxPrice),
                                        style = minMaxStyle,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                        text = "${dayPrices.maxHour}:00",
                                        style = timeStyle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                        }
                }
        } else {
                Text(
                        text = "--",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }
}
