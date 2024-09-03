/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.window.layout

import androidx.annotation.MainThread
import androidx.core.app.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@MainThread
fun ComponentActivity.registerWindowLayoutInfoChangeCallback(
    collector: FlowCollector<WindowLayoutInfo>,
): StateFlow<WindowLayoutInfo>? {
    val windowInfoFlow = WindowInfoTracker.getOrCreate(this).windowLayoutInfo(this)
    var currentStateFlow: StateFlow<WindowLayoutInfo>? = null
    lifecycleScope.launch {
        val stateFlow = windowInfoFlow.stateIn(lifecycleScope)
        currentStateFlow = stateFlow
        stateFlow.collect {}
    }
    // Collect windowInfo when STARTED and stop when STOPPED.
    lifecycleScope.launch(Dispatchers.Main) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            currentStateFlow!!.collect(collector)
        }
    }
    return currentStateFlow
}
