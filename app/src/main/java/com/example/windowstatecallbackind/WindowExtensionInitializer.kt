package com.example.windowstatecallbackind

import android.content.Context
import androidx.startup.Initializer
import androidx.window.layout.WindowInfoTracker

/** Initializes WindowInfoTracker. */
class WindowExtensionInitializer : Initializer<WindowInfoTracker> {

    override fun create(context: Context) = WindowInfoTracker.getOrCreate(context)

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}