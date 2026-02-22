package fi.protonode.ElectricitySpotPrice

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for Hilt dependency injection.
 *
 * - @HiltAndroidApp boots Hilt when the app starts and generates the app-level dependency container
 * (a graph of how to build classes).
 * - With this in place, Android components (Activity/Service) annotated with
 * @AndroidEntryPoint can receive injected dependencies, and classes with
 * @Inject constructors can be created by Hilt.
 */
@HiltAndroidApp class ElectricitySpotPriceApp : Application()
