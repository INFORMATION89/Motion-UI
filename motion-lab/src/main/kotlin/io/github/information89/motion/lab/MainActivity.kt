package io.github.information89.motion.lab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.information89.motion.Dial
import io.github.information89.motion.DialLook
import io.github.information89.motion.DialScale
import io.github.information89.motion.EdgeBand
import io.github.information89.motion.Haptic
import io.github.information89.motion.Morph
import io.github.information89.motion.MorphHost
import io.github.information89.motion.Motion
import io.github.information89.motion.MotionSpring
import io.github.information89.motion.MotionTime
import io.github.information89.motion.PageOvershoot
import io.github.information89.motion.edgeBlur
import io.github.information89.motion.haptics
import io.github.information89.motion.morphAnchor
import io.github.information89.motion.morphSource
import io.github.information89.motion.motionDrag
import io.github.information89.motion.pageOvershoot
import io.github.information89.motion.pageSpringFling
import io.github.information89.motion.rememberDialState
import io.github.information89.motion.rememberMotionValue
import io.github.information89.motion.reveal
import io.github.information89.motion.springToPage
import io.github.information89.motion.ui.MotionUiMaterials
import io.github.information89.motion.ui.MotionUiTheme
import io.github.information89.motion.ui.MotionUiThemeTokens
import io.github.information89.motion.ui.MotionUiTokens
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MotionLabTheme {
                MotionLabApp()
            }
        }
    }
}

private enum class LabSection(val title: String) {
    COMPONENTS("Components"),
    INTERACTIONS("Interactions"),
}

private enum class LabPage(val title: String, val description: String, val section: LabSection) {
    MORPH("Morph", "Grow a surface from its source, then catch or drag it back.", LabSection.COMPONENTS),
    PAGES("Page movement", "Swipe between pages or compare Motion with the Compose default.", LabSection.COMPONENTS),
    EDGE_BLUR(
        "Scroll edge blur",
        "Explore progressive blur and fade at the edges of a horizontal list.",
        LabSection.COMPONENTS
    ),
    TOOLBAR(
        "Animated toolbar",
        "Explore host-driven selection, variable-width tabs, drag switching, and two visual profiles.",
        LabSection.INTERACTIONS
    ),
    SPRINGS(
        "Springs and release",
        "Compare Motion spring tokens and try gesture-to-spring release.",
        LabSection.INTERACTIONS
    ),
    DIAL("Dial", "Drag, flick, and snap a reusable dial with haptic feedback.", LabSection.INTERACTIONS),
    HAPTICS("Haptics", "Try semantic haptic tokens.", LabSection.INTERACTIONS),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotionLabApp() {
    var page by rememberSaveable { mutableStateOf<LabPage?>(null) }
    var section by rememberSaveable { mutableStateOf(LabSection.COMPONENTS) }
    var slowMotion by rememberSaveable { mutableFloatStateOf(1f) }
    BackHandler(enabled = page != null) { page = null }

    DisposableEffect(Unit) {
        onDispose { MotionTime.scale = 1f }
    }
    LaunchedEffect(slowMotion) {
        MotionTime.scale = slowMotion
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(page?.title ?: "Motion Lab") },
                    navigationIcon = {
                        if (page != null) {
                            IconButton(
                                onClick = { page = null },
                                modifier = Modifier.semantics { contentDescription = "Navigate back" }
                            ) {
                                Text("‹", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                )
                if (page == null) {
                    TabRow(selectedTabIndex = LabSection.entries.indexOf(section)) {
                        LabSection.entries.forEach { item ->
                            Tab(
                                selected = section == item,
                                onClick = { section = item },
                                text = { Text(item.title) }
                            )
                        }
                    }
                }
            }
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (page == null) {
                Text(
                    "Motion is the interaction layer beneath your UI. Pick a pattern to explore how it behaves.",
                    style = MaterialTheme.typography.bodyLarge
                )
                SlowMotionControl(slowMotion) { slowMotion = it }
                MotionUiTokensPreview()
                LabPage.entries.filter { it.section == section }.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { page = item },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleLarge)
                            Text(item.description, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Explore →",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                Text(
                    "Standalone sample app · Android / Jetpack Compose",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(page!!.description, style = MaterialTheme.typography.bodyLarge)
                SlowMotionControl(slowMotion) { slowMotion = it }
                when (page) {
                    LabPage.TOOLBAR -> ToolbarPrototype()
                    LabPage.SPRINGS -> SpringsDemo()
                    LabPage.MORPH -> MorphDemo()
                    LabPage.PAGES -> PageDemo()
                    LabPage.DIAL -> DialDemo()
                    LabPage.HAPTICS -> HapticsDemo()
                    LabPage.EDGE_BLUR -> EdgeBlurDemo()
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun MotionUiTokensPreview() {
    val tokens = MotionUiThemeTokens.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = tokens.shapes.card,
        colors = CardDefaults.cardColors(containerColor = tokens.materials.cardContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = tokens.materials.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.compact)
        ) {
            Text("Motion UI theme", style = MaterialTheme.typography.titleMedium)
            Text(
                "This preview reads the app-provided shape, spacing, surface, and elevation tokens.",
                style = MaterialTheme.typography.bodyMedium
            )
            Surface(
                color = tokens.materials.glassContainer,
                shape = RoundedCornerShape(tokens.spacing.compact)
            ) {
                Text(
                    "Configurable surface role",
                    modifier = Modifier.padding(tokens.spacing.compact),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun SlowMotionControl(value: Float, onChange: (Float) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Slow motion · ${"%.1f".format(value)}×", style = MaterialTheme.typography.titleSmall)
            Slider(value = value, onValueChange = onChange, valueRange = 1f..5f, steps = 3)
            Text(
                "Applies to springs started after the setting changes. It resets when Motion Lab closes.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private val springTokens: List<Pair<String, MotionSpring>> = listOf(
    "smooth" to Motion.smooth,
    "container" to Motion.container,
    "dismiss" to Motion.dismiss,
    "playful" to Motion.playful,
    "momentum" to Motion.momentum
)

@Composable
private fun SpringsDemo() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val position = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Choose a spring token, then play it in both directions.", style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            springTokens.forEachIndexed { index, item ->
                FilterChip(
                    selected = selected == index,
                    onClick = { selected = index },
                    label = { Text(item.first) }
                )
            }
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            val travel = with(androidx.compose.ui.platform.LocalDensity.current) { (maxWidth - 36.dp).toPx() }
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .size(36.dp)
                    .graphicsLayer { translationX = travel * position.value }
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        Text(
            "${springTokens[selected].first} · response ${"%.2f".format(
                springTokens[selected].second.response
            )} s · damping ${"%.2f".format(springTokens[selected].second.damping)}",
            style = MaterialTheme.typography.bodySmall
        )
        Button(onClick = {
            scope.launch {
                position.animateTo(
                    if (position.targetValue < 0.5f) 1f else 0f,
                    springTokens[selected].second.spec()
                )
            }
        }) { Text("Play spring") }

        Spacer(Modifier.height(4.dp))
        Text("Gesture release", style = MaterialTheme.typography.titleMedium)
        val value = rememberMotionValue(initial = 0f, overMin = 0.12f, overMax = 0.12f)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val travelDp = maxWidth - 36.dp
            val travelPx = with(androidx.compose.ui.platform.LocalDensity.current) { travelDp.toPx() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .motionDrag(
                        value,
                        androidx.compose.foundation.gestures.Orientation.Horizontal,
                        travelDp
                    )
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .size(36.dp)
                        .graphicsLayer { translationX = travelPx * value.value }
                        .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch { value.animateTo(if (value.target < 0.5f) 1f else 0f) }
            }) {
                Text(if (value.target < 0.5f) "Animate release" else "Return to start")
            }
        }
        Text(
            "position ${"%.2f".format(value.value)} · target ${"%.1f".format(value.target)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun MorphDemo() {
    var compare by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = compare,
            onClick = { compare = !compare },
            label = { Text("Compare with Compose") }
        )
        if (compare) {
            MorphComparisonDemo()
        } else {
            MorphMotionDemo()
        }
    }
}

@Composable
private fun MorphMotionDemo() {
    val motion = rememberMotionValue(initial = 0f, overMin = 0.06f, overMax = 0.08f)
    val visible by remember(motion) {
        derivedStateOf { motion.value > 0.001f || motion.isRunning }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MorphHost(
            Modifier
                .fillMaxWidth()
                .height(340.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                Spacer(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(230.dp)
                        .morphAnchor("lab-target", 28.dp)
                )
                Button(
                    onClick = { motion.animateTo(if (motion.target < 0.5f) 1f else 0f) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .morphAnchor("lab-source", 100.dp)
                        .morphSource("lab-source", 100.dp)
                ) {
                    Text(if (motion.target < 0.5f) "Open surface" else "Close surface")
                }
                if (visible) {
                    Morph(
                        from = "lab-source",
                        to = "lab-target",
                        value = motion,
                        modifier = Modifier
                            .fillMaxSize()
                            .motionDrag(
                                motion,
                                androidx.compose.foundation.gestures.Orientation.Vertical,
                                230.dp,
                                reverseDirection = true
                            ),
                        surface = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(22.dp)
                                .reveal(motion, 0.35f, 0.7f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("The source became this surface", style = MaterialTheme.typography.titleMedium)
                            Text("Drag down to scrub the morph back to its source. Tap the action below to close.")
                            Button(onClick = { motion.animateTo(0f) }) { Text("Close") }
                        }
                    }
                }
            }
        }
        Text("The morph uses measured source and target anchors. Its content is revealed as the surface grows.")
    }
}

@Composable
private fun MorphComparisonDemo() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val motion = rememberMotionValue(initial = 0f, overMin = 0.04f, overMax = 0.04f)
    val visible by remember(motion) {
        derivedStateOf { motion.value > 0.001f || motion.isRunning }
    }
    LaunchedEffect(expanded) {
        motion.animateTo(if (expanded) 1f else 0f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Close both" else "Open both")
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Motion · measured container morph", style = MaterialTheme.typography.titleMedium)
                MorphHost(
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .morphAnchor("compare-source", 24.dp)
                                .morphSource("compare-source", 24.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) {
                                Text("Profile", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(154.dp)
                                .morphAnchor("compare-target", 28.dp)
                        )
                        if (visible) {
                            Morph(
                                from = "compare-source",
                                to = "compare-target",
                                value = motion,
                                modifier = Modifier.fillMaxSize(),
                                surface = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(18.dp)
                                        .reveal(motion, 0.3f, 0.7f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Profile details", style = MaterialTheme.typography.titleMedium)
                                    Text("A surface travels from its measured source into its destination.")
                                    Text("The content appears as the container grows.")
                                }
                            }
                        }
                    }
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Compose · default AnimatedVisibility", style = MaterialTheme.typography.titleMedium)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) {
                        Text("Profile", style = MaterialTheme.typography.titleMedium)
                    }
                }
                AnimatedVisibility(visible = expanded) {
                    Surface(
                        Modifier.fillMaxWidth().height(154.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Profile details", style = MaterialTheme.typography.titleMedium)
                            Text("Compose reveals a separate destination below the source.")
                            Text("This uses AnimatedVisibility's default enter and exit.")
                        }
                    }
                }
            }
        }
        Text(
            "One control opens and closes both previews. Motion morphs between measured anchors; Compose uses its default AnimatedVisibility transition.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PageDemo() {
    val pager = rememberPagerState(initialPage = 0) { 3 }
    val overshoot = remember { PageOvershoot() }
    var springIndex by rememberSaveable { mutableIntStateOf(0) }
    var compare by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val springs = listOf("momentum" to Motion.momentum, "gentle" to Motion.gentle, "playful" to Motion.playful)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            springs.forEachIndexed { index, item ->
                FilterChip(
                    selected = springIndex == index,
                    onClick = { springIndex = index },
                    label = { Text(item.first) }
                )
            }
        }
        FilterChip(
            selected = compare,
            onClick = { compare = !compare },
            label = { Text("Compare with Compose") }
        )
        if (compare) {
            PageComparisonDemo(springs[springIndex].second)
        } else {
            HorizontalPager(
                state = pager,
                flingBehavior = remember(pager, springIndex) {
                    pageSpringFling(pager, overshoot) { springs[springIndex].second }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .pageOvershoot(overshoot)
            ) { index ->
                Surface(
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer
                    )[index]
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Page ${index + 1}", style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Button(onClick = {
                        scope.launch { pager.springToPage(index, overshoot, springs[springIndex].second) }
                    }) {
                        Text("${index + 1}")
                    }
                }
            }
            Text(
                "Swipe, or tap a page number. The pages use the selected Motion spring.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PageComparisonDemo(spring: MotionSpring) {
    val motionPager = rememberPagerState(initialPage = 0) { 3 }
    val composePager = rememberPagerState(initialPage = 0) { 3 }
    val overshoot = remember { PageOvershoot() }
    val scope = rememberCoroutineScope()
    val pageColors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Both previews receive the same destination at the same time.",
            style = MaterialTheme.typography.bodyMedium
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Motion · springToPage", style = MaterialTheme.typography.titleMedium)
                HorizontalPager(
                    state = motionPager,
                    userScrollEnabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .pageOvershoot(overshoot)
                ) { index ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = pageColors[index]
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Page ${index + 1}", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Compose · default animateScrollToPage", style = MaterialTheme.typography.titleMedium)
                HorizontalPager(
                    state = composePager,
                    userScrollEnabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) { index ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = pageColors[index]
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Page ${index + 1}", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { index ->
                Button(onClick = {
                    scope.launch { motionPager.springToPage(index, overshoot, spring) }
                    scope.launch { composePager.animateScrollToPage(index) }
                }) {
                    Text("Page ${index + 1}")
                }
            }
        }
        Text(
            "Tap a page number to drive both previews together. Motion uses the selected spring; Compose uses its default pager animation. Swiping is disabled here so the input stays identical.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun HapticsDemo() {
    val feedback = haptics()
    val tokens = remember { Haptic.all }
    var intensity by remember { mutableFloatStateOf(Haptic.intensity) }

    DisposableEffect(Unit) {
        onDispose { Haptic.intensity = 1f }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Choose a token to play its semantic feedback. The token list is shared with Motion components.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text("Intensity · " + (intensity * 100).toInt() + "%", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = intensity,
            onValueChange = {
                intensity = it
                Haptic.intensity = it
            },
            valueRange = 0f..1f,
            steps = 3
        )
        tokens.chunked(2).forEach { rowTokens ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTokens.forEach { token ->
                    Button(
                        onClick = { feedback.perform(token) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(token.name)
                    }
                }
                if (rowTokens.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Text(
            "Output depends on device hardware and the system touch-feedback setting. Unsupported primitives fall back to Android view feedback. Intensity resets to 100% when leaving this page.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DialDemo() {
    val scale = remember { DialScale(min = -14f, max = 14f, step = 1f, majorEvery = 4, visibleSpan = 8f) }
    val state = rememberDialState(scale, initial = 0f)
    val ink = MaterialTheme.colorScheme.onSurface
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Value: ${state.value.target.toInt()}", style = MaterialTheme.typography.titleLarge)
        Dial(
            state = state,
            look = DialLook(
                ink = ink,
                marker = MaterialTheme.colorScheme.primary,
                labelStyle = MaterialTheme.typography.labelSmall
            ),
            label = { it.toInt().toString() },
            describe = { "Position ${it.toInt()}" },
            onDoubleTap = { it.value.animateTo(0f) },
            modifier = Modifier.fillMaxWidth().height(88.dp)
        )
        Button(onClick = { state.value.animateTo(0f) }) {
            Text("Reset dial")
        }
        Text(
            "Double-tap the dial to return to zero. Drag or flick to move between steps; use Reset dial as a button alternative."
        )
    }
}

@Composable
private fun EdgeBlurDemo() {
    val items = remember { (1..18).map { "Item $it" } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .edgeBlur(
                    start = EdgeBand(size = 54.dp, blur = 16.dp, fadeOut = true),
                    end = EdgeBand(size = 54.dp, blur = 16.dp, fadeOut = true)
                ),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier.width(112.dp).fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(item, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Text("Scroll horizontally. On Android 12 and later, the edge bands also blur nearby content.")
    }
}

@Composable
private fun MotionLabTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFFB5CBE0),
            secondary = Color(0xFFB7D0C2),
            tertiary = Color(0xFFE0B99F)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF465D72),
            secondary = Color(0xFF526B61),
            tertiary = Color(0xFF805B45)
        )
    }
    MotionUiTheme(
        colorScheme = colors,
        tokens = MotionUiTokens(
            materials = MotionUiMaterials(
                cardContainer = colors.surfaceContainer,
                glassContainer = colors.surface.copy(alpha = 0.86f)
            )
        ),
        content = content
    )
}
