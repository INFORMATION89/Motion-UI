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

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import io.kotest.matchers.ints.shouldBeBetween
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.coroutines.resume

private const val HZ_60_NANOS = 16_666_667L
private const val HZ_120_NANOS = 8_333_333L

@OptIn(ExperimentalCoroutinesApi::class)
class AmbientClockTickerTest {
    private fun intervalNanos(fps: Int) = 1_000_000_000L / fps

    /** `snapshotFlow` only sees writes once apply notifications are sent (the UI does this per frame). */
    private fun <T> androidx.compose.runtime.MutableState<T>.set(value: T) {
        this.value = value
        Snapshot.sendApplyNotifications()
    }

    @Test
    fun inactiveTickerDoesNotRequestFrames() = runTest {
        val frameClock = ManualFrameClock()
        val job = launch(frameClock) {
            runAmbientClockTicker(intervalNanos(30), running = { false }, onTick = {})
        }

        runCurrent()
        frameClock.pendingFrames shouldBe 0

        job.cancel()
    }

    @Test
    fun activationByStateChangeRequestsAFrameAndTicks() = runTest {
        val running = mutableStateOf(false)
        val frameClock = ManualFrameClock()
        var ticks = 0
        val job = launch(frameClock) {
            runAmbientClockTicker(intervalNanos(30), running = { running.value }, onTick = { ticks++ })
        }

        runCurrent()
        frameClock.pendingFrames shouldBe 0

        running.set(true)
        runCurrent()
        frameClock.pendingFrames shouldBe 1

        frameClock.sendFrame(1_000_000_000L)
        runCurrent()
        ticks shouldBe 1

        job.cancel()
        runCurrent()
        frameClock.pendingFrames shouldBe 0
    }

    @Test
    fun deactivatingTickerCancelsPendingFrameRequest() = runTest {
        val running = mutableStateOf(true)
        val frameClock = ManualFrameClock()
        var ticks = 0
        val job = launch(frameClock) {
            runAmbientClockTicker(intervalNanos(30), running = { running.value }, onTick = { ticks++ })
        }

        runCurrent()
        frameClock.pendingFrames shouldBe 1

        running.set(false)
        runCurrent()
        frameClock.pendingFrames shouldBe 0
        frameClock.sendFrame(2_000_000_000L)
        runCurrent()
        ticks shouldBe 0

        job.cancel()
    }

    @Test
    fun thirtyFpsOn60HzDisplayTicksAboutThirtyTimesPerSecond() = runTest {
        ticksPerVirtualSecond(fps = 30, framePeriodNanos = HZ_60_NANOS).shouldBeBetween(29, 31)
    }

    @Test
    fun thirtyFpsOn120HzDisplayTicksAboutThirtyTimesPerSecond() = runTest {
        ticksPerVirtualSecond(fps = 30, framePeriodNanos = HZ_120_NANOS).shouldBeBetween(29, 31)
    }

    @Test
    fun sixtyFpsOn60HzDisplayTicksAboutSixtyTimesPerSecond() = runTest {
        ticksPerVirtualSecond(fps = 60, framePeriodNanos = HZ_60_NANOS).shouldBeBetween(59, 61)
    }

    @Test
    fun rapidTogglingNeverTicksFasterThanTheCap() = runTest {
        val fps = 30
        val running = mutableStateOf(true)
        val frameClock = VsyncFrameClock(this, HZ_60_NANOS)
        val tickTimes = mutableListOf<Long>()
        val job = launch(frameClock) {
            runAmbientClockTicker(
                intervalNanos(fps),
                running = { running.value },
                onTick = { tickTimes += frameClock.lastFrameNanos }
            )
        }

        repeat(120) {
            running.set(false)
            runCurrent()
            running.set(true)
            advanceTimeBy(5)
        }
        job.cancel()

        (tickTimes.size > 2) shouldBe true
        // Allow the documented one-millisecond gate tolerance; a frame-gated clock never beats it.
        tickTimes.zipWithNext { a, b ->
            b - a
        }.forEach { it shouldBeGreaterThanOrEqual intervalNanos(fps) - 1_000_000L }
    }

    @Test
    fun replacedRunningLambdaIsHonored() = runTest {
        val frameClock = ManualFrameClock()
        val lambda = mutableStateOf<() -> Boolean>({ false })
        var ticks = 0
        val job = launch(frameClock) {
            // Mirrors rememberAmbientClock, which reads the latest lambda via rememberUpdatedState.
            runAmbientClockTicker(intervalNanos(30), running = { lambda.value() }, onTick = { ticks++ })
        }

        runCurrent()
        frameClock.pendingFrames shouldBe 0

        lambda.set { true }
        runCurrent()
        frameClock.pendingFrames shouldBe 1

        lambda.set { false }
        runCurrent()
        frameClock.pendingFrames shouldBe 0
        ticks shouldBe 0

        job.cancel()
    }

    private suspend fun TestScope.ticksPerVirtualSecond(fps: Int, framePeriodNanos: Long): Int {
        val frameClock = VsyncFrameClock(this, framePeriodNanos)
        var ticks = 0
        val job = launch(frameClock) {
            runAmbientClockTicker(intervalNanos(fps), running = { true }, onTick = { ticks++ })
        }
        // One virtual second. The first tick fires immediately and is counted; the +/-1 tolerance absorbs it.
        advanceTimeBy(1_000)
        runCurrent()
        job.cancel()
        return ticks
    }
}

/** A frame clock that completes only when a test sends a frame. */
private class ManualFrameClock : MonotonicFrameClock {
    private val waiting = mutableListOf<(Long) -> Unit>()
    val pendingFrames: Int
        get() = waiting.size

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
        suspendCancellableCoroutine { continuation: CancellableContinuation<R> ->
            val request: (Long) -> Unit = { frameNanos ->
                if (continuation.isActive) continuation.resume(onFrame(frameNanos))
            }
            waiting += request
            continuation.invokeOnCancellation { waiting.remove(request) }
        }

    fun sendFrame(frameNanos: Long) {
        val current = waiting.toList()
        waiting.clear()
        current.forEach { it(frameNanos) }
    }
}

/**
 * A display that vsyncs every [periodNanos] of the test's virtual time. A request completes at the first
 * vsync strictly after the previous one delivered, with the vsync's exact timestamp.
 */
private class VsyncFrameClock(
    private val scope: TestScope,
    private val periodNanos: Long,
) : MonotonicFrameClock {
    var lastFrameNanos = 0L
        private set

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
        val nowNanos = scope.currentTime * 1_000_000L
        val next = maxOf(nowNanos / periodNanos + 1, lastFrameNanos / periodNanos + 1) * periodNanos
        delay((next - nowNanos + 999_999L) / 1_000_000L)
        lastFrameNanos = next
        return onFrame(next)
    }
}
