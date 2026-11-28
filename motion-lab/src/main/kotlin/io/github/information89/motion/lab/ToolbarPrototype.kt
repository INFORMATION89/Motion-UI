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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.information89.motion.Haptic
import io.github.information89.motion.Motion
import io.github.information89.motion.haptics
import io.github.information89.motion.rememberReducedMotion
import io.github.information89.motion.ui.MotionUiThemeTokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class ToolbarDestination(
    val id: String,
    val label: String,
    val symbol: String,
    val description: String,
)

internal enum class ToolbarVisualProfile(val title: String) {
    EXPRESSIVE("M3 Expressive"),
    CUSTOM("App-defined"),
}

private data class ToolbarAppearance(
    val container: Color,
    val indicator: Color,
    val selectedContent: Color,
    val unselectedContent: Color,
    val barShape: Shape,
    val indicatorShape: Shape,
    val border: Color?,
)

private val toolbarDestinations = listOf(
    ToolbarDestination("overview", "Overview", "⌂", "Overview destination"),
    ToolbarDestination("saved", "Saved items", "☆", "Saved items destination"),
    ToolbarDestination("discover", "Discover", "⌕", "Discover destination"),
    ToolbarDestination("settings", "Settings", "⚙", "Settings destination")
)

@Composable
internal fun ToolbarPrototype() {
    val pagerState = rememberPagerState(pageCount = { toolbarDestinations.size })
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()
    var profile by remember { mutableStateOf(ToolbarVisualProfile.EXPRESSIVE) }
    var showContextAction by remember { mutableStateOf(true) }
    var contextActionDetached by remember { mutableStateOf(false) }
    var hapticsEnabled by remember { mutableStateOf(false) }
    var actionActive by remember { mutableStateOf(false) }

    // The committed selection is the settled page; progress endpoints still follow currentPage.
    val selectedTabId = toolbarDestinations.getOrNull(pagerState.settledPage)?.id
        ?: toolbarDestinations.first().id
    val hostProgress = toolbarProgressForPager(
        destinations = toolbarDestinations,
        currentPage = pagerState.currentPage,
        pageOffset = pagerState.currentPageOffsetFraction
    )

    fun requestSelection(tabId: String) {
        val page = toolbarDestinations.indexOfFirst { it.id == tabId }
        if (page >= 0) {
            scope.launch {
                if (reducedMotion) {
                    pagerState.scrollToPage(page)
                } else {
                    pagerState.animateScrollToPage(page)
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("One interaction model, two visual profiles.", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolbarVisualProfile.entries.forEach { item ->
                FilterChip(
                    selected = profile == item,
                    onClick = { profile = item },
                    label = { Text(item.title) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = showContextAction,
                onClick = { showContextAction = !showContextAction },
                label = { Text("Context action") }
            )
            FilterChip(
                selected = contextActionDetached,
                onClick = { contextActionDetached = !contextActionDetached },
                label = { Text("Detach action") }
            )
            FilterChip(
                selected = hapticsEnabled,
                onClick = { hapticsEnabled = !hapticsEnabled },
                label = { Text("Haptics") }
            )
        }
        ToolbarPrototypeBar(
            selectedTabId = selectedTabId,
            hostProgress = hostProgress,
            profile = profile,
            showContextAction = showContextAction,
            contextActionDetached = contextActionDetached,
            actionActive = actionActive,
            hapticsEnabled = hapticsEnabled,
            reducedMotion = reducedMotion,
            onActionClick = { actionActive = !actionActive },
            onSelectionRequest = ::requestSelection,
            onSelectionInteractionStarted = {
                scope.launch { pagerState.stopScroll() }
            }
        )
        Text(
            "Long-press the selected tab and drag across the row. " +
                "A tap or keyboard action requests a destination; the pager remains the source of truth. " +
                "Use Detach action to inspect the connector retracting into a separate target.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(220.dp)
        ) { page ->
            val destination = toolbarDestinations[page]
            val pageColor = when (page % 3) {
                0 -> MaterialTheme.colorScheme.primaryContainer
                1 -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.tertiaryContainer
            }
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                color = pageColor
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(destination.symbol, style = MaterialTheme.typography.headlineLarge)
                    Text(destination.label, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Page ${page + 1} of ${toolbarDestinations.size}. Swipe here or use the toolbar above.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        Text(
            hostProgress?.let {
                "Host progress · ${it.fromTabId} → ${it.toTabId} · ${"%.2f".format(it.fraction)}"
            } ?: "Committed destination · $selectedTabId",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (reducedMotion) {
                "Reduced motion is active: destination requests use immediate pager changes."
            } else {
                "Reduced motion is inactive: destination requests use the pager spring animation."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun ToolbarPrototypeBar(
    selectedTabId: String,
    hostProgress: ToolbarHostProgress?,
    profile: ToolbarVisualProfile,
    showContextAction: Boolean,
    contextActionDetached: Boolean,
    actionActive: Boolean,
    hapticsEnabled: Boolean,
    reducedMotion: Boolean,
    onActionClick: () -> Unit,
    onSelectionRequest: (String) -> Unit,
    onSelectionInteractionStarted: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val tokens = MotionUiThemeTokens.current
    val appearance = when (profile) {
        ToolbarVisualProfile.EXPRESSIVE -> ToolbarAppearance(
            container = colors.surfaceContainerLow,
            indicator = colors.primaryContainer,
            selectedContent = colors.onPrimaryContainer,
            unselectedContent = colors.onSurfaceVariant,
            barShape = RoundedCornerShape(28.dp),
            indicatorShape = RoundedCornerShape(20.dp),
            border = null
        )
        ToolbarVisualProfile.CUSTOM -> ToolbarAppearance(
            container = tokens.materials.cardContainer,
            indicator = colors.tertiaryContainer,
            selectedContent = colors.onTertiaryContainer,
            unselectedContent = colors.onSurface,
            barShape = tokens.shapes.card,
            indicatorShape = RoundedCornerShape(12.dp),
            border = colors.outlineVariant
        )
    }
    val feedback = haptics()
    val detachmentTarget = if (contextActionDetached) 1f else 0f
    val detachmentProgress by animateFloatAsState(
        targetValue = detachmentTarget,
        animationSpec = if (reducedMotion) snap() else Motion.playful.spec(),
        label = "toolbar-action-detachment"
    )
    val connectorGap by animateDpAsState(
        targetValue = if (contextActionDetached) 8.dp else 0.dp,
        animationSpec = if (reducedMotion) snap() else Motion.control.spec(),
        label = "toolbar-action-gap"
    )
    val actionLeadingCorner by animateDpAsState(
        targetValue = if (contextActionDetached) 24.dp else 12.dp,
        animationSpec = if (reducedMotion) snap() else Motion.control.spec(),
        label = "toolbar-action-leading-corner"
    )
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val tabBounds = remember { mutableStateMapOf<String, ToolbarTabBounds>() }
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragPointerViewportX by remember { mutableFloatStateOf(0f) }
    var dragTargetId by remember { mutableStateOf<String?>(null) }
    var settlingTargetId by remember { mutableStateOf<String?>(null) }
    var focusedTabId by remember(selectedTabId) { mutableStateOf(selectedTabId) }
    var pendingFocusRequestId by remember { mutableStateOf<String?>(null) }
    var toolbarHasFocus by remember { mutableStateOf(false) }

    val activeSettlingTargetId = settlingTargetId?.takeUnless {
        it == selectedTabId || hostProgress != null
    }
    val committedProgress = if (dragging || activeSettlingTargetId != null) null else hostProgress
    val effectiveSelectedId = dragTargetId ?: activeSettlingTargetId ?: selectedTabId
    val ids = remember { toolbarDestinations.map { it.id } }
    val focusRequesters = remember(ids) { ids.associateWith { FocusRequester() } }
    val layoutDirection = LocalLayoutDirection.current
    val selectionFractions = toolbarSelectionFractions(
        tabIds = ids,
        selectedTabId = effectiveSelectedId,
        hostProgress = committedProgress
    )
    val hostIndicator = toolbarIndicatorBounds(
        tabIds = ids,
        boundsById = tabBounds,
        selectedTabId = effectiveSelectedId,
        hostProgress = committedProgress
    )
    val indicator = if (dragging || activeSettlingTargetId != null) {
        tabBounds[effectiveSelectedId] ?: hostIndicator
    } else {
        hostIndicator
    }
    val density = LocalDensity.current
    val latestOnSelectionRequest by rememberUpdatedState(onSelectionRequest)
    val latestOnSelectionInteractionStarted by rememberUpdatedState(onSelectionInteractionStarted)

    LaunchedEffect(selectedTabId, toolbarHasFocus, pendingFocusRequestId) {
        if (!toolbarHasFocus && pendingFocusRequestId == null) {
            focusedTabId = selectedTabId
        }
    }
    LaunchedEffect(pendingFocusRequestId) {
        val targetId = pendingFocusRequestId ?: return@LaunchedEffect
        withFrameNanos { }
        val requested = focusRequesters[targetId]?.requestFocus() == true
        if (!requested) {
            pendingFocusRequestId = null
            if (!toolbarHasFocus) focusedTabId = selectedTabId
        }
    }
    LaunchedEffect(selectedTabId, hostProgress != null, settlingTargetId) {
        if (
            settlingTargetId != null &&
            (settlingTargetId == selectedTabId || hostProgress != null)
        ) {
            settlingTargetId = null
        }
    }
    LaunchedEffect(
        selectedTabId,
        focusedTabId,
        toolbarHasFocus,
        hostProgress == null,
        dragging,
        activeSettlingTargetId,
        viewportWidthPx,
        tabBounds[if (toolbarHasFocus) focusedTabId else selectedTabId],
        scrollState.maxValue
    ) {
        if (hostProgress != null || dragging || activeSettlingTargetId != null) return@LaunchedEffect
        val targetId = if (toolbarHasFocus) focusedTabId else selectedTabId
        val targetBounds = tabBounds[targetId] ?: return@LaunchedEffect
        if (viewportWidthPx <= 0f) return@LaunchedEffect
        val target = (targetBounds.centerPx - viewportWidthPx / 2f)
            .roundToInt()
            .coerceIn(0, scrollState.maxValue)
        if (target != scrollState.value) {
            if (reducedMotion) scrollState.scrollTo(target) else scrollState.animateScrollTo(target)
        }
    }
    LaunchedEffect(dragging, layoutDirection, hapticsEnabled) {
        if (!dragging) return@LaunchedEffect
        val reverseDirection = layoutDirection == LayoutDirection.Rtl
        val edgeSize = with(density) { 48.dp.toPx() }
        val maxSpeed = with(density) { 900.dp.toPx() }
        var previousFrameNanos = 0L
        while (dragging) {
            val frameNanos = withFrameNanos { it }
            if (previousFrameNanos != 0L) {
                val frameSeconds = (
                    (frameNanos - previousFrameNanos) / 1_000_000_000f
                    ).coerceIn(0f, 0.05f)
                val delta = toolbarEdgeScrollDelta(
                    pointerViewportX = dragPointerViewportX,
                    viewportWidthPx = viewportWidthPx,
                    frameSeconds = frameSeconds,
                    reverseDirection = reverseDirection,
                    edgeSizePx = edgeSize,
                    maxSpeedPxPerSecond = maxSpeed
                )
                if (delta != 0f) {
                    scrollState.scrollBy(delta)
                    val contentX = toolbarContentPosition(
                        viewportPositionPx = dragPointerViewportX,
                        scrollOffsetPx = scrollState.value.toFloat(),
                        reverseDirection = reverseDirection
                    )
                    val next = nearestToolbarTabId(ids, tabBounds, contentX)
                    if (next != null && next != dragTargetId) {
                        dragTargetId = next
                        if (hapticsEnabled) feedback.perform(Haptic.detent)
                    }
                }
            }
            previousFrameNanos = frameNanos
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .clip(appearance.barShape)
                .background(appearance.container)
                .then(
                    appearance.border?.let { Modifier.border(BorderStroke(1.dp, it), appearance.barShape) }
                        ?: Modifier
                )
                .onGloballyPositioned { viewportWidthPx = it.size.width.toFloat() }
        ) {
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                Box(modifier = Modifier.height(64.dp)) {
                    indicator?.let { bounds ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset { IntOffset(bounds.leftPx.roundToInt(), 0) }
                                .width(with(density) { bounds.widthPx.toDp() })
                                .height(48.dp)
                                .clip(appearance.indicatorShape)
                                .background(appearance.indicator)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .selectableGroup()
                            .focusGroup()
                            .onFocusChanged { toolbarHasFocus = it.hasFocus },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        toolbarDestinations.forEach { destination ->
                            val fraction = selectionFractions[destination.id] ?: 0f
                            val labelStyle = MaterialTheme.typography.labelLarge
                            val labelLayout = textMeasurer.measure(
                                text = AnnotatedString(destination.label),
                                style = labelStyle
                            )
                            val labelWidth = with(density) { labelLayout.size.width.toDp() }
                            val isCommittedSelection = selectedTabId == destination.id
                            val isVisuallySelected = fraction >= 0.5f
                            val tabContentColor = if (isVisuallySelected) {
                                appearance.selectedContent
                            } else {
                                appearance.unselectedContent
                            }
                            Row(
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        // A transient zero-size layout is not a measurement; keep the last bounds.
                                        if (coordinates.size.width > 0) {
                                            val next = ToolbarTabBounds(
                                                leftPx = coordinates.positionInParent().x,
                                                widthPx = coordinates.size.width.toFloat()
                                            )
                                            if (tabBounds[destination.id] != next) {
                                                tabBounds[destination.id] = next
                                            }
                                        }
                                    }
                                    .clip(RoundedCornerShape(24.dp))
                                    .then(
                                        if (toolbarHasFocus && focusedTabId == destination.id) {
                                            Modifier.border(
                                                BorderStroke(2.dp, colors.primary),
                                                RoundedCornerShape(24.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .focusRequester(focusRequesters.getValue(destination.id))
                                    .focusProperties { canFocus = focusedTabId == destination.id }
                                    .onFocusChanged {
                                        if (it.isFocused) {
                                            focusedTabId = destination.id
                                            if (pendingFocusRequestId == destination.id) {
                                                pendingFocusRequestId = null
                                            }
                                        }
                                    }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type != KeyEventType.KeyDown) {
                                            return@onPreviewKeyEvent false
                                        }
                                        val forwardKey = if (layoutDirection == LayoutDirection.Ltr) {
                                            Key.DirectionRight
                                        } else {
                                            Key.DirectionLeft
                                        }
                                        val backwardKey = if (layoutDirection == LayoutDirection.Ltr) {
                                            Key.DirectionLeft
                                        } else {
                                            Key.DirectionRight
                                        }
                                        val targetId = when (event.key) {
                                            forwardKey -> toolbarAdjacentTabId(ids, destination.id, forward = true)
                                            backwardKey -> toolbarAdjacentTabId(ids, destination.id, forward = false)
                                            else -> null
                                        } ?: return@onPreviewKeyEvent false
                                        focusedTabId = targetId
                                        pendingFocusRequestId = targetId
                                        latestOnSelectionRequest(targetId)
                                        true
                                    }
                                    .semantics {
                                        role = Role.Tab
                                        selected = isCommittedSelection
                                        contentDescription = destination.description
                                    }
                                    .clickable(role = Role.Tab) {
                                        latestOnSelectionRequest(destination.id)
                                    }
                                    .pointerInput(destination.id, selectedTabId, hapticsEnabled, layoutDirection) {
                                        if (destination.id == selectedTabId) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { start ->
                                                    val measured = tabBounds[destination.id]
                                                    if (measured != null) {
                                                        dragging = true
                                                        dragTargetId = destination.id
                                                        settlingTargetId = null
                                                        latestOnSelectionInteractionStarted()
                                                        if (hapticsEnabled) feedback.perform(Haptic.press)
                                                        dragPointerViewportX = toolbarViewportPosition(
                                                            contentPositionPx = measured.leftPx + start.x,
                                                            scrollOffsetPx = scrollState.value.toFloat(),
                                                            reverseDirection = layoutDirection == LayoutDirection.Rtl
                                                        )
                                                    }
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    if (dragging) {
                                                        dragPointerViewportX += amount.x
                                                        val contentX = toolbarContentPosition(
                                                            viewportPositionPx = dragPointerViewportX,
                                                            scrollOffsetPx = scrollState.value.toFloat(),
                                                            reverseDirection = layoutDirection == LayoutDirection.Rtl
                                                        )
                                                        val next = nearestToolbarTabId(ids, tabBounds, contentX)
                                                        if (next != null && next != dragTargetId) {
                                                            dragTargetId = next
                                                            if (hapticsEnabled) feedback.perform(Haptic.detent)
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    val target = dragTargetId
                                                    dragging = false
                                                    dragTargetId = null
                                                    if (target != null) {
                                                        settlingTargetId = target
                                                        latestOnSelectionRequest(target)
                                                        if (hapticsEnabled) feedback.perform(Haptic.release)
                                                    }
                                                },
                                                onDragCancel = {
                                                    dragging = false
                                                    dragTargetId = null
                                                }
                                            )
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    destination.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = tabContentColor
                                )
                                Spacer(Modifier.width(8.dp * fraction))
                                Box(
                                    modifier = Modifier
                                        .width(labelWidth * fraction)
                                        .clipToBounds()
                                ) {
                                    Text(
                                        destination.label,
                                        modifier = Modifier.width(labelWidth),
                                        maxLines = 1,
                                        softWrap = false,
                                        style = labelStyle,
                                        color = appearance.selectedContent
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showContextAction,
            enter = if (reducedMotion) {
                EnterTransition.None
            } else {
                expandHorizontally(animationSpec = Motion.container.spec()) +
                    fadeIn(animationSpec = Motion.content.spec())
            },
            exit = if (reducedMotion) {
                ExitTransition.None
            } else {
                shrinkHorizontally(animationSpec = Motion.dismiss.spec()) +
                    fadeOut(animationSpec = Motion.content.spec())
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(
                    modifier = Modifier
                        .width(24.dp)
                        .height(48.dp)
                ) {
                    val centerY = size.height / 2f
                    val outerHalfHeight = size.height * 0.38f
                    val waistHalfHeight = outerHalfHeight * (1f - detachmentProgress)
                    val connector = Path().apply {
                        moveTo(0f, centerY - outerHalfHeight)
                        cubicTo(
                            size.width * 0.3f,
                            centerY - outerHalfHeight,
                            size.width * 0.3f,
                            centerY - waistHalfHeight,
                            size.width * 0.5f,
                            centerY - waistHalfHeight
                        )
                        cubicTo(
                            size.width * 0.7f,
                            centerY - waistHalfHeight,
                            size.width * 0.7f,
                            centerY - outerHalfHeight,
                            size.width,
                            centerY - outerHalfHeight
                        )
                        lineTo(size.width, centerY + outerHalfHeight)
                        cubicTo(
                            size.width * 0.7f,
                            centerY + outerHalfHeight,
                            size.width * 0.7f,
                            centerY + waistHalfHeight,
                            size.width * 0.5f,
                            centerY + waistHalfHeight
                        )
                        cubicTo(
                            size.width * 0.3f,
                            centerY + waistHalfHeight,
                            size.width * 0.3f,
                            centerY + outerHalfHeight,
                            0f,
                            centerY + outerHalfHeight
                        )
                        close()
                    }
                    drawPath(
                        path = connector,
                        color = appearance.indicator.copy(
                            alpha = (1f - detachmentProgress).coerceIn(0f, 1f)
                        )
                    )
                }
                Spacer(Modifier.width(connectorGap))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier
                        .height(48.dp)
                        .semantics {
                            stateDescription = if (actionActive) "Pinned" else "Not pinned"
                        },
                    shape = RoundedCornerShape(
                        topStart = actionLeadingCorner,
                        topEnd = 24.dp,
                        bottomEnd = 24.dp,
                        bottomStart = actionLeadingCorner
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appearance.indicator,
                        contentColor = appearance.selectedContent
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (actionActive) "✓" else "+")
                        Spacer(Modifier.width(8.dp))
                        Text(if (actionActive) "Unpin" else "Pin")
                    }
                }
            }
        }
    }
}

private fun toolbarProgressForPager(
    destinations: List<ToolbarDestination>,
    currentPage: Int,
    pageOffset: Float,
): ToolbarHostProgress? {
    if (!pageOffset.isFinite() || pageOffset == 0f || destinations.isEmpty()) return null
    return when {
        pageOffset > 0f && currentPage < destinations.lastIndex -> ToolbarHostProgress(
            fromTabId = destinations[currentPage].id,
            toTabId = destinations[currentPage + 1].id,
            fraction = pageOffset
        )
        pageOffset < 0f && currentPage > 0 -> ToolbarHostProgress(
            fromTabId = destinations[currentPage - 1].id,
            toTabId = destinations[currentPage].id,
            fraction = 1f + pageOffset
        )
        else -> null
    }
}
