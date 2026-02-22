package fi.protonode.ElectricitySpotPrice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import fi.protonode.ElectricitySpotPrice.ui.screens.CurrentPriceScreen

/**
 * Marks this Activity as a Hilt entry point so it can receive injected dependencies (e.g.,
 * ViewModels via hiltViewModel() inside Compose). Without @AndroidEntryPoint, Hilt cannot inject
 * into this Activity.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent { CurrentPriceScreen() }
        }
}
