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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.information89.motion.ui.MotionUiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ToolbarPrototypeUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabTapRequestsDestinationWithoutChangingHostSelection() {
        val requested = AtomicReference<String?>(null)
        setToolbar(onSelectionRequest = requested::set)

        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
                hasContentDescription("Saved items destination")
        ).performClick()
        composeRule.waitForIdle()

        assertEquals("saved", requested.get())
        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
                hasContentDescription("Overview destination")
        ).assertIsSelected()
        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
                hasContentDescription("Saved items destination")
        ).assertIsNotSelected()
    }

    @Test
    fun toolbarExposesFourTabsAndExactlyOneCommittedSelection() {
        setToolbar()

        composeRule.waitForIdle()

        composeRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).assertCountEquals(4)
        composeRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
                isSelected()
        ).assertCountEquals(1)
        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
                hasContentDescription("Overview destination")
        ).assertIsSelected()
    }

    @Test
    fun hostProgressChangesVisualPreviewWithoutCommittingTabSelection() {
        val requested = AtomicReference<String?>(null)
        setToolbar(
            hostProgress = ToolbarHostProgress("overview", "saved", 0.75f),
            onSelectionRequest = requested::set
        )

        composeRule.waitForIdle()

        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
                hasContentDescription("Overview destination")
        ).assertIsSelected()
        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
                hasContentDescription("Saved items destination")
        ).assertIsNotSelected()
        composeRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab) and
                isSelected()
        ).assertCountEquals(1)
        assertEquals(null, requested.get())
    }

    @Test
    fun longPressDragRequestsTargetOnReleaseWithoutCommittingSelection() {
        val requested = AtomicReference<String?>(null)
        val requestCount = AtomicInteger(0)
        val interactionStarted = AtomicInteger(0)
        setToolbar(
            onSelectionRequest = {
                requestCount.incrementAndGet()
                requested.set(it)
            },
            onSelectionInteractionStarted = { interactionStarted.incrementAndGet() }
        )

        composeRule.waitForIdle()
        dragSelectedOverviewToSaved(cancelGesture = false)

        assertEquals(1, interactionStarted.get())
        assertEquals("saved", requested.get())
        assertEquals(1, requestCount.get())
        composeRule.onNodeWithContentDescription("Overview destination").assertIsSelected()
        composeRule.onNodeWithContentDescription("Saved items destination").assertIsNotSelected()
    }

    @Test
    fun cancelledLongPressDragDoesNotRequestTarget() {
        val requested = AtomicReference<String?>(null)
        val requestCount = AtomicInteger(0)
        val interactionStarted = AtomicInteger(0)
        setToolbar(
            onSelectionRequest = {
                requestCount.incrementAndGet()
                requested.set(it)
            },
            onSelectionInteractionStarted = { interactionStarted.incrementAndGet() }
        )

        composeRule.waitForIdle()
        dragSelectedOverviewToSaved(cancelGesture = true)

        assertEquals(1, interactionStarted.get())
        assertEquals(null, requested.get())
        assertEquals(0, requestCount.get())
        composeRule.onNodeWithContentDescription("Overview destination").assertIsSelected()
        composeRule.onNodeWithContentDescription("Saved items destination").assertIsNotSelected()
    }

    @Test
    fun arrowLeftWrapsFromFirstTabToLastInLtr() {
        val requested = AtomicReference<String?>(null)
        setToolbar(onSelectionRequest = requested::set)

        val overview = composeRule.onNodeWithContentDescription("Overview destination")
        overview.performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()
        overview.performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Settings destination").assertIsFocused()
        assertEquals("settings", requested.get())
    }

    @Test
    fun arrowRightWrapsFromLastTabToFirstInLtr() {
        val requested = AtomicReference<String?>(null)
        setToolbar(selectedTabId = "settings", onSelectionRequest = requested::set)

        val settings = composeRule.onNodeWithContentDescription("Settings destination")
        settings.performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()
        settings.performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Overview destination").assertIsFocused()
        assertEquals("overview", requested.get())
    }

    @Test
    fun attachedContextActionHasOneSemanticButtonTarget() {
        assertContextActionHasOneTarget(detached = false)
    }

    @Test
    fun detachedContextActionKeepsTheSameSingleSemanticButtonTarget() {
        assertContextActionHasOneTarget(detached = true)
    }

    @Test
    fun hiddenContextActionHasNoSemanticOrClickableTarget() {
        setToolbar(showContextAction = false)

        composeRule.waitForIdle()
        composeRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).assertCountEquals(0)
    }

    @Test
    fun arrowRightMovesTheSingleFocusStopForwardInLtr() {
        val requested = AtomicReference<String?>(null)
        setToolbar(onSelectionRequest = requested::set)

        val overview = composeRule.onNodeWithContentDescription("Overview destination")
        overview.performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()
        overview.assertIsFocused()

        overview.performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Saved items destination").assertIsFocused()
        overview.assertIsNotFocused()
        assertEquals("saved", requested.get())
    }

    @Test
    fun arrowLeftMovesTheSingleFocusStopForwardInRtl() {
        val requested = AtomicReference<String?>(null)
        setToolbar(
            layoutDirection = LayoutDirection.Rtl,
            onSelectionRequest = requested::set
        )

        val overview = composeRule.onNodeWithContentDescription("Overview destination")
        overview.performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()
        overview.assertIsFocused()

        overview.performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Saved items destination").assertIsFocused()
        overview.assertIsNotFocused()
        assertEquals("saved", requested.get())
    }

    @Test
    fun arrowRightMovesTheSingleFocusStopBackwardInRtl() {
        val requested = AtomicReference<String?>(null)
        setToolbar(
            layoutDirection = LayoutDirection.Rtl,
            selectedTabId = "saved",
            onSelectionRequest = requested::set
        )

        val saved = composeRule.onNodeWithContentDescription("Saved items destination")
        saved.performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()
        saved.performKeyInput { pressKey(Key.DirectionRight) }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Overview destination").assertIsFocused()
        saved.assertIsNotFocused()
        assertEquals("overview", requested.get())
    }

    @Test
    fun arrowLeftWrapsFromLastTabToFirstInRtl() {
        val requested = AtomicReference<String?>(null)
        setToolbar(
            layoutDirection = LayoutDirection.Rtl,
            selectedTabId = "settings",
            onSelectionRequest = requested::set
        )

        val settings = composeRule.onNodeWithContentDescription("Settings destination")
        settings.performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()
        settings.performKeyInput { pressKey(Key.DirectionLeft) }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Overview destination").assertIsFocused()
        assertEquals("overview", requested.get())
    }

    @Test
    fun attachedContextActionDoesNotInterceptAdjacentTabClick() {
        assertAdjacentTabRemainsInteractive(
            contextActionDetached = false,
            profile = ToolbarVisualProfile.EXPRESSIVE
        )
    }

    @Test
    fun detachedContextActionDoesNotInterceptAdjacentTabClick() {
        assertAdjacentTabRemainsInteractive(
            contextActionDetached = true,
            profile = ToolbarVisualProfile.CUSTOM
        )
    }

    private fun assertAdjacentTabRemainsInteractive(
        contextActionDetached: Boolean,
        profile: ToolbarVisualProfile,
    ) {
        val requested = AtomicReference<String?>(null)
        val actionClicks = AtomicInteger(0)
        setToolbar(
            selectedTabId = "settings",
            profile = profile,
            contextActionDetached = contextActionDetached,
            onActionClick = { actionClicks.incrementAndGet() },
            onSelectionRequest = requested::set
        )

        composeRule.waitForIdle()

        composeRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        ).assertCountEquals(1)

        composeRule.onNodeWithContentDescription("Settings destination").performTouchInput {
            click(
                Offset(
                    x = width.toFloat() - 8f,
                    y = height.toFloat() / 2f
                )
            )
        }
        composeRule.waitForIdle()

        assertEquals("settings", requested.get())
        assertEquals(0, actionClicks.get())

        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        ).performTouchInput {
            click()
        }
        composeRule.waitForIdle()

        assertEquals("settings", requested.get())
        assertEquals(1, actionClicks.get())
    }

    @Test
    fun contextActionExposesToggleStateAndKeepsOneTarget() {
        val active = mutableStateOf(false)
        setToolbar(
            actionActive = { active.value },
            onActionClick = { active.value = !active.value }
        )

        composeRule.waitForIdle()
        val button = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        composeRule.onNode(button).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not pinned")
        )

        composeRule.onNode(button).performClick()
        composeRule.waitForIdle()

        composeRule.onNode(button).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Pinned")
        )
        composeRule.onAllNodes(button).assertCountEquals(1)
    }

    private fun assertContextActionHasOneTarget(detached: Boolean) {
        val clicks = AtomicInteger(0)
        setToolbar(
            contextActionDetached = detached,
            onActionClick = { clicks.incrementAndGet() }
        )

        composeRule.waitForIdle()
        composeRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).assertCountEquals(1)
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).performClick()
        composeRule.waitForIdle()

        assertEquals(1, clicks.get())
        composeRule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).assertCountEquals(1)
    }

    private fun dragSelectedOverviewToSaved(cancelGesture: Boolean) {
        val source = composeRule.onNodeWithContentDescription("Overview destination")
            .fetchSemanticsNode()
            .boundsInRoot
            .center
        val target = composeRule.onNodeWithContentDescription("Saved items destination")
            .fetchSemanticsNode()
            .boundsInRoot
            .center

        composeRule.onRoot().performTouchInput {
            down(source)
            advanceEventTime(700)
            moveTo(target, delayMillis = 16)
            if (cancelGesture) {
                cancel()
            } else {
                up()
            }
        }
        composeRule.waitForIdle()
    }

    private fun setToolbar(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        selectedTabId: String = "overview",
        profile: ToolbarVisualProfile = ToolbarVisualProfile.EXPRESSIVE,
        hostProgress: ToolbarHostProgress? = null,
        showContextAction: Boolean = true,
        contextActionDetached: Boolean = false,
        actionActive: () -> Boolean = { false },
        reducedMotion: Boolean = true,
        onActionClick: () -> Unit = {},
        onSelectionRequest: (String) -> Unit = {},
        onSelectionInteractionStarted: () -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MotionUiTheme {
                    Box(Modifier.width(380.dp)) {
                        ToolbarPrototypeBar(
                            selectedTabId = selectedTabId,
                            hostProgress = hostProgress,
                            profile = profile,
                            showContextAction = showContextAction,
                            contextActionDetached = contextActionDetached,
                            actionActive = actionActive(),
                            reducedMotion = reducedMotion,
                            hapticsEnabled = false,
                            onActionClick = onActionClick,
                            onSelectionRequest = onSelectionRequest,
                            onSelectionInteractionStarted = onSelectionInteractionStarted
                        )
                    }
                }
            }
        }
    }
}
