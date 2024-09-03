package com.example.windowstatecallbackind

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display.DEFAULT_DISPLAY
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.registerWindowLayoutInfoChangeCallback
import kotlinx.coroutines.flow.StateFlow

/** Activity to show display configuration from different system callbacks. */
class WindowStateCallbackActivity : ComponentActivity() {
    private val viewModel: WindowStateViewModel by viewModels()

    /**
     * [DisplayManager]s from `Activity` and `Application` are updated from different resource
     * configurations, so we listen on them separately.
     */
    private lateinit var applicationDisplayManager: DisplayManager
    private lateinit var activityDisplayManager: DisplayManager
    private lateinit var windowMetricsCalculator: WindowMetricsCalculator
    private lateinit var handler: Handler
    private var winLayoutStateFlow: StateFlow<WindowLayoutInfo>? = null

    /**
     * Runnable to poll configuration every 500ms. To always provide an up-to-date configuration so
     * it can be used to verify the configurations from other callbacks.
     */
    private val updateWindowStateIfChanged =
        object : Runnable {
            override fun run() {
                provideLatestWindowState()
                handler.postDelayed(this, 500)
            }
        }

    /** [DisplayListener] on `Application`. */
    private val applicationDisplayListener =
        object : DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}

            override fun onDisplayRemoved(displayId: Int) {}

            override fun onDisplayChanged(displayId: Int) {
                if (displayId != DEFAULT_DISPLAY) {
                    return
                }
                onWindowStateCallbackInvoked(R.string.application_display_listener_title, displayId)
            }
        }

    /** [DisplayListener] on `Activity`. */
    private val activityDisplayListener =
        object : DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}

            override fun onDisplayRemoved(displayId: Int) {}

            override fun onDisplayChanged(displayId: Int) {
                if (displayId != DEFAULT_DISPLAY) {
                    return
                }
                onWindowStateCallbackInvoked(R.string.activity_display_listener_title, displayId)
            }
        }

    /** [onConfigurationChanged] on `Application`. */
    private val applicationComponentCallback =
        object : ComponentCallbacks {
            override fun onConfigurationChanged(configuration: Configuration) {
                onWindowStateCallbackInvoked(
                    R.string.application_configuration_title,
                    configuration
                )
            }

            @Deprecated(
                "Since API level 34 this is never called. Apps targeting API level 34 " +
                    "and above may provide an empty implementation."
            )
            override fun onLowMemory() {}
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { DemoTheme { WindowStateScreen() } }

        applicationDisplayManager = application.getSystemService(DisplayManager::class.java)
        activityDisplayManager = getSystemService(DisplayManager::class.java)
        windowMetricsCalculator = WindowMetricsCalculator.getOrCreate()
        handler = Handler(Looper.getMainLooper())
        onWindowStateCallbackInvoked(R.string.activity_create_begin_title)

        applicationDisplayManager.registerDisplayListener(applicationDisplayListener, handler)
        activityDisplayManager.registerDisplayListener(activityDisplayListener, handler)
        application.registerComponentCallbacks(applicationComponentCallback)

        // Collect windowInfo immediately and may repeat from ON_START to ON_STOP.
        winLayoutStateFlow = registerWindowLayoutInfoChangeCallback { info ->
            onWindowStateCallbackInvoked(R.string.display_feature_title, info)
        }
        if (winLayoutStateFlow !=null) {
            onWindowStateCallbackInvoked(
                R.string.display_feature_stateflow_title,
                winLayoutStateFlow!!.value,
            )
        }

        onWindowStateCallbackInvoked(R.string.activity_create_end_title)
    }

    override fun onDestroy() {
        super.onDestroy()
        applicationDisplayManager.unregisterDisplayListener(applicationDisplayListener)
        activityDisplayManager.unregisterDisplayListener(activityDisplayListener)
        application.unregisterComponentCallbacks(applicationComponentCallback)
    }

    override fun onResume() {
        super.onResume()
        // Start polling the configuration every 500ms.
        updateWindowStateIfChanged.run()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateWindowStateIfChanged)
    }

    /** [onConfigurationChanged] on `Activity`. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onWindowStateCallbackInvoked(R.string.activity_configuration_title, newConfig)
    }

    /** Called when the corresponding system callback is invoked. */
    private fun onWindowStateCallbackInvoked(resId: Int, details: Any? = "") {
        viewModel.onWindowStateCallback(queryWindowState(resId, details = "$details"))
    }

    private fun provideLatestWindowState() {
        viewModel.updateLatestWindowState(
            queryWindowState(
                R.string.latest_configuration_title,
                "poll configuration every 500ms",
            )
        )
    }

    private fun queryWindowState(resId: Int, details: String): WindowState {
        fun DisplayManager.defaultDisplayRotation() = getDisplay(DEFAULT_DISPLAY).rotation
        fun Context.displayBounds() =
            windowMetricsCalculator.computeMaximumWindowMetrics(this).bounds

        return WindowState(
            name = getString(resId),
            applicationDisplayRotation = applicationDisplayManager.defaultDisplayRotation(),
            activityDisplayRotation = activityDisplayManager.defaultDisplayRotation(),
            applicationDisplayBounds = applicationContext.displayBounds(),
            activityDisplayBounds = this@WindowStateCallbackActivity.displayBounds(),
            callbackDetails = details,
        )
    }
}
