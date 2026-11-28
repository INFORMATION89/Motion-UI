/*
 * Copyright 2026 INFORMATION89
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.information89.motion

import android.animation.ValueAnimator
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * The clock for ambient animation (F4, agreed 2026-11-21): a slow drift, twinkling, or background
 * ambience—anything that keeps moving rather than responding to a gesture. Any app's ambient animation
 * needs the same rules, so they live here:
 * - **capped frames per second** ([fps], clamped to 1–120): ambient motion doesn't need the display's
 *   full rate, and every frame of a full-screen shader costs battery;
 * - **lifecycle-aware:** it only ticks while the screen is resumed;
 * - **paused while unseen:** while [running] is false no frames are requested at all;
 * - its time is **wall-clock based**, so the drift carries on where it was after a pause.
 *
 * [running] is **observed, not polled**: it must read Compose snapshot state (for example a
 * `mutableStateOf` or a `State` the caller owns), and the clock reacts when that state changes. A lambda
 * that reads a plain `var` or another non-snapshot source is evaluated only when it is first observed
 * and never again, so it can neither pause nor resume the clock. The lambda itself may be replaced on
 * recomposition.
 *
 * Use it with [rememberAmbientMotionAllowed]: when that's false (battery saver, animations off),
 * draw a still frame instead.
 */
@Composable
fun rememberAmbientClock(fps: Int = 30, running: () -> Boolean = { true }): State<Float> {
    val seconds = remember { mutableFloatStateOf(ambientSeconds()) }
    val isRunning = rememberUpdatedState(running)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val frameIntervalNanos = 1_000_000_000L / fps.coerceIn(1, 120)
    LaunchedEffect(lifecycle, frameIntervalNanos) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            runAmbientClockTicker(
                frameIntervalNanos = frameIntervalNanos,
                running = { isRunning.value() },
                onTick = { seconds.floatValue = ambientSeconds() }
            )
        }
    }
    return seconds
}

/** Frame timestamps this close to the interval still count as due, absorbing vsync jitter. */
private const val TICK_TOLERANCE_NANOS = 1_000_000L

/** Wake this much before the frame that should tick, so the timer never pushes a tick past its vsync. */
private const val WAKE_SLACK_NANOS = 2_000_000L

/** Initial guess for the display's frame period (60 Hz) until two consecutive frames have been seen. */
private const val INITIAL_FRAME_PERIOD_NANOS = 16_666_667L

/**
 * Ticks at [frameIntervalNanos] while [running] is true, and requests no frames while it is false.
 *
 * Each tick runs in a frame callback and the cadence is gated on frame timestamps (a tick is due once
 * `now - last >= interval - tolerance`), so the rate follows the requested fps to within one display
 * frame. The timer only avoids waking for frames that would be skipped: after each frame it sleeps for
 * the time left until the next tick minus one observed frame period and a little slack, floored at 0.
 * It never adds to the period. `last` is kept across deactivation, so toggling [running] rapidly cannot
 * exceed the cap. [running] is observed with `snapshotFlow`; see [rememberAmbientClock].
 */
internal suspend fun runAmbientClockTicker(
    frameIntervalNanos: Long,
    running: () -> Boolean,
    onTick: () -> Unit,
) {
    var lastTickNanos = Long.MIN_VALUE
    var lastFrameNanos = Long.MIN_VALUE
    var framePeriodNanos = INITIAL_FRAME_PERIOD_NANOS
    snapshotFlow { running() }.collectLatest { active ->
        if (!active) return@collectLatest
        while (true) {
            val untilNextTickNanos = withFrameNanos { now ->
                // The smallest gap between consecutive frames is the display's frame period.
                if (lastFrameNanos != Long.MIN_VALUE && now > lastFrameNanos) {
                    framePeriodNanos = minOf(framePeriodNanos, now - lastFrameNanos)
                }
                lastFrameNanos = now
                if (lastTickNanos == Long.MIN_VALUE ||
                    now - lastTickNanos >= frameIntervalNanos - TICK_TOLERANCE_NANOS
                ) {
                    lastTickNanos = now
                    onTick()
                }
                frameIntervalNanos - (now - lastTickNanos)
            }
            val sleepMillis = (untilNextTickNanos - framePeriodNanos - WAKE_SLACK_NANOS) / 1_000_000L
            if (sleepMillis > 0L) delay(sleepMillis)
        }
    }
}

/** Whether ambient motion should run: not in battery saver, not with the system's animations off. */
@Composable
fun rememberAmbientMotionAllowed(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val powerSave = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
        ValueAnimator.areAnimatorsEnabled() && !powerSave
    }
}

/** Seconds within the current hour, from the wall clock (small enough for a shader's float). */
private fun ambientSeconds() = (System.currentTimeMillis() % 3_600_000L) / 1000f
