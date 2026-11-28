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

package io.github.information89.motion.lab

import kotlin.math.abs

internal data class ToolbarHostProgress(
    val fromTabId: String,
    val toTabId: String,
    val fraction: Float,
)

internal data class ToolbarTabBounds(
    val leftPx: Float,
    val widthPx: Float,
) {
    init {
        require(leftPx.isFinite() && leftPx >= 0f) { "leftPx must be finite and non-negative" }
        require(widthPx.isFinite() && widthPx > 0f) { "widthPx must be finite and positive" }
    }

    val centerPx: Float get() = leftPx + widthPx / 2f
}

private fun ToolbarHostProgress.isValidFor(tabIds: List<String>): Boolean {
    val fromIndex = tabIds.indexOf(fromTabId)
    val toIndex = tabIds.indexOf(toTabId)
    return fraction.isFinite() &&
        fraction in 0f..1f &&
        fromIndex >= 0 &&
        toIndex >= 0 &&
        abs(fromIndex - toIndex) == 1
}

internal fun toolbarSelectionFractions(
    tabIds: List<String>,
    selectedTabId: String,
    hostProgress: ToolbarHostProgress?,
): Map<String, Float> {
    require(tabIds.distinct().size == tabIds.size) { "tab IDs must be unique" }

    val fractions = tabIds.associateWith { if (it == selectedTabId) 1f else 0f }.toMutableMap()
    val progress = hostProgress ?: return fractions
    if (!progress.isValidFor(tabIds)) return fractions

    fractions[progress.fromTabId] = 1f - progress.fraction
    fractions[progress.toTabId] = progress.fraction
    return fractions
}

internal fun interpolateToolbarBounds(
    from: ToolbarTabBounds,
    to: ToolbarTabBounds,
    fraction: Float,
): ToolbarTabBounds? {
    if (!fraction.isFinite() || fraction !in 0f..1f) return null
    return ToolbarTabBounds(
        leftPx = from.leftPx + (to.leftPx - from.leftPx) * fraction,
        widthPx = from.widthPx + (to.widthPx - from.widthPx) * fraction
    )
}

internal fun toolbarIndicatorBounds(
    tabIds: List<String>,
    boundsById: Map<String, ToolbarTabBounds>,
    selectedTabId: String,
    hostProgress: ToolbarHostProgress?,
): ToolbarTabBounds? {
    require(tabIds.distinct().size == tabIds.size) { "tab IDs must be unique" }
    val progress = hostProgress ?: return boundsById[selectedTabId]
    if (!progress.isValidFor(tabIds)) return boundsById[selectedTabId]
    val from = boundsById[progress.fromTabId] ?: return boundsById[selectedTabId]
    val to = boundsById[progress.toTabId] ?: return boundsById[selectedTabId]
    return interpolateToolbarBounds(from, to, progress.fraction) ?: boundsById[selectedTabId]
}

internal fun toolbarAdjacentTabId(
    tabIds: List<String>,
    currentTabId: String,
    forward: Boolean,
): String? {
    require(tabIds.distinct().size == tabIds.size) { "tab IDs must be unique" }
    if (tabIds.size < 2) return null

    val currentIndex = tabIds.indexOf(currentTabId)
    if (currentIndex < 0) return null
    val delta = if (forward) 1 else -1
    val targetIndex = (currentIndex + delta + tabIds.size) % tabIds.size
    return tabIds[targetIndex]
}

internal fun toolbarContentPosition(
    viewportPositionPx: Float,
    scrollOffsetPx: Float,
    reverseDirection: Boolean,
): Float {
    val direction = if (reverseDirection) -1f else 1f
    return viewportPositionPx + scrollOffsetPx * direction
}

internal fun toolbarViewportPosition(
    contentPositionPx: Float,
    scrollOffsetPx: Float,
    reverseDirection: Boolean,
): Float {
    val direction = if (reverseDirection) -1f else 1f
    return contentPositionPx - scrollOffsetPx * direction
}

internal fun toolbarEdgeScrollDelta(
    pointerViewportX: Float,
    viewportWidthPx: Float,
    frameSeconds: Float,
    reverseDirection: Boolean,
    edgeSizePx: Float,
    maxSpeedPxPerSecond: Float,
): Float {
    if (
        !pointerViewportX.isFinite() ||
        !viewportWidthPx.isFinite() ||
        !frameSeconds.isFinite() ||
        !edgeSizePx.isFinite() ||
        !maxSpeedPxPerSecond.isFinite() ||
        viewportWidthPx <= 0f ||
        frameSeconds <= 0f ||
        edgeSizePx <= 0f ||
        maxSpeedPxPerSecond <= 0f
    ) {
        return 0f
    }

    val edge = minOf(edgeSizePx, viewportWidthPx / 2f)
    val leftStrength = ((edge - pointerViewportX) / edge).coerceIn(0f, 1f)
    val rightStrength = (
        (pointerViewportX - (viewportWidthPx - edge)) / edge
        ).coerceIn(0f, 1f)
    val velocity = (rightStrength * rightStrength - leftStrength * leftStrength) *
        maxSpeedPxPerSecond
    val direction = if (reverseDirection) -1f else 1f
    return velocity * frameSeconds * direction
}

internal fun nearestToolbarTabId(
    tabIds: List<String>,
    boundsById: Map<String, ToolbarTabBounds>,
    positionPx: Float,
): String? {
    if (!positionPx.isFinite()) return null
    return tabIds
        .mapNotNull { id -> boundsById[id]?.let { id to abs(it.centerPx - positionPx) } }
        .minByOrNull { it.second }
        ?.first
}
