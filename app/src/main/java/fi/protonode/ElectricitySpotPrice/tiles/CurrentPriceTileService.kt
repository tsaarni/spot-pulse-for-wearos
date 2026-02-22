package fi.protonode.ElectricitySpotPrice.tiles

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.compactButton
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import fi.protonode.ElectricitySpotPrice.R
import fi.protonode.ElectricitySpotPrice.repo.PriceRepository
import java.text.NumberFormat
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future

@AndroidEntryPoint
class CurrentPriceTileService : TileService() {

        @Inject
        lateinit var repository: PriceRepository
        private val serviceScope = CoroutineScope(Dispatchers.IO)

        override fun onTileRequest(
                requestParams: RequestBuilders.TileRequest
        ): ListenableFuture<TileBuilders.Tile> {
                return serviceScope.future {
                        val currentPriceCents = repository.getCurrentPriceCents()
                        val deviceConfig = requestParams.deviceConfiguration
                        val context = this@CurrentPriceTileService

                        val content =
                                materialScope(context, deviceConfig) {
                                        if (currentPriceCents != null) {
                                                val priceText =
                                                        priceNumberFormat()
                                                                .format(currentPriceCents)
                                                buildPriceLayout(priceText)
                                        } else {
                                                buildUnavailableLayout()
                                        }
                                }

                        val millisUntilNextPrice =
                                computeFreshnessIntervalMillis(repository.getNextPriceTimestamp())

                        TileBuilders.Tile.Builder()
                                .setResourcesVersion("1")
                                .setTileTimeline(
                                        TimelineBuilders.Timeline.Builder()
                                                .addTimelineEntry(
                                                        TimelineBuilders.TimelineEntry.Builder()
                                                                .setLayout(
                                                                        LayoutElementBuilders.Layout
                                                                                .Builder()
                                                                                .setRoot(content)
                                                                                .build()
                                                                )
                                                                .build()
                                                )
                                                .build()
                                )
                                .setFreshnessIntervalMillis(millisUntilNextPrice)
                                .build()
                }
        }

        override fun onTileResourcesRequest(
                requestParams: RequestBuilders.ResourcesRequest
        ): ListenableFuture<ResourceBuilders.Resources> {
                return Futures.immediateFuture(
                        ResourceBuilders.Resources.Builder().setVersion("1").build()
                )
        }

        private fun MaterialScope.buildPriceLayout(
                priceText: String
        ): LayoutElementBuilders.LayoutElement {
                val clickable = buildLaunchClickable()

                val row = LayoutElementBuilders.Row.Builder()
                for (char in priceText) {
                        val charElem = LayoutElementBuilders.Text.Builder()
                                .setText(char.toString())
                                .setFontStyle(
                                        LayoutElementBuilders.FontStyle.Builder()
                                                .setSize(DimensionBuilders.sp(50f))
                                                .setColor(colorScheme.primary.prop)
                                                .setVariant(
                                                        LayoutElementBuilders.FontVariantProp.Builder()
                                                                .setValue(LayoutElementBuilders.FONT_VARIANT_BODY)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()

                        if (char == '1') {
                                // Trim tabular space for '1'
                                row.addContent(
                                        LayoutElementBuilders.Box.Builder()
                                                .addContent(charElem)
                                                .setWidth(DimensionBuilders.dp(20f))
                                                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                                .build()
                                )
                        } else if (char == ',' || char == '.') {
                                // Trim tabular space for decimal separator
                                row.addContent(
                                        LayoutElementBuilders.Box.Builder()
                                                .addContent(charElem)
                                                .setWidth(DimensionBuilders.dp(12f))
                                                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                                                .build()
                                )
                        } else {
                                row.addContent(charElem)
                        }
                }

                val priceContent =
                        LayoutElementBuilders.Column.Builder()
                                .addContent(
                                        row.setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                                                .build()
                                )
                                .addContent(
                                        text(
                                                text = "c/kWh".layoutString,
                                                typography = Typography.TITLE_MEDIUM,
                                                color = colorScheme.onSurfaceVariant
                                        )
                                )
                                .setHorizontalAlignment(
                                        LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
                                )
                                .build()

                return primaryLayout(
                        mainSlot = { priceContent },
                        titleSlot = {
                                text(
                                        text = getString(R.string.tile_current_price).layoutString,
                                        typography = Typography.LABEL_MEDIUM,
                                        color = colorScheme.onSurfaceVariant
                                )
                        },
                        onClick = clickable
                )
        }

        private fun MaterialScope.buildUnavailableLayout(): LayoutElementBuilders.LayoutElement {
                return primaryLayout(
                        mainSlot = {
                                text(
                                        text = "-".layoutString,
                                        typography = Typography.DISPLAY_MEDIUM,
                                        color = colorScheme.onSurfaceVariant
                                )
                        }
                )
        }

        private fun buildLaunchClickable(): ModifiersBuilders.Clickable =
                ModifiersBuilders.Clickable.Builder()
                        .setOnClick(
                                ActionBuilders.LaunchAction.Builder()
                                        .setAndroidActivity(
                                                ActionBuilders.AndroidActivity.Builder()
                                                        .setPackageName(
                                                                "fi.protonode.ElectricitySpotPrice"
                                                        )
                                                        .setClassName(
                                                                "fi.protonode.ElectricitySpotPrice.MainActivity"
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                        .build()

        private fun priceNumberFormat(): NumberFormat =
                NumberFormat.getNumberInstance(Locale.getDefault()).apply {
                        minimumFractionDigits = 2
                        maximumFractionDigits = 2
                }

        private fun computeFreshnessIntervalMillis(nextPriceTime: OffsetDateTime?): Long {
                val now = OffsetDateTime.now(ZoneId.systemDefault())
                return if (nextPriceTime != null) {
                        Duration.between(now, nextPriceTime).toMillis().coerceAtLeast(60_000)
                } else {
                        Duration.ofHours(1).toMillis()
                }
        }
}
