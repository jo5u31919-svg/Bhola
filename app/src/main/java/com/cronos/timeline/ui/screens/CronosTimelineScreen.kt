package com.cronos.timeline.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cronos.timeline.model.TimelineMath
import com.cronos.timeline.model.TimelineTickLevel
import com.cronos.timeline.ui.components.BlockEditorSheet
import com.cronos.timeline.ui.components.CategoryFilterBar
import com.cronos.timeline.ui.components.CountdownHeaderBar
import com.cronos.timeline.ui.components.HorizontalTimelineCanvas
import com.cronos.timeline.ui.theme.CyanChronos
import com.cronos.timeline.ui.theme.HarmonyGlassCard
import com.cronos.timeline.ui.theme.ObsidianVoid
import com.cronos.timeline.ui.theme.TextPrimary
import com.cronos.timeline.ui.theme.TextSecondary
import com.cronos.timeline.viewmodel.TimelineViewModel
import com.example.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun CronosTimelineScreen(
    viewModel: TimelineViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var scaleX by rememberSaveable { mutableFloatStateOf(1.0f) }
    var scrollOffsetPx by rememberSaveable { mutableFloatStateOf(0f) }
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }
    var topOverlayHeightPx by remember { mutableIntStateOf(0) }
    var hasInitializedScroll by rememberSaveable { mutableStateOf(false) }
    var activeScrollJob by remember { mutableStateOf<Job?>(null) }

    val manualExpansionOverrides = remember { mutableStateMapOf<Long, Boolean>() }
    val isZoomExpanded = scaleX >= TimelineMath.DETAIL_EXPANSION_SCALE_THRESHOLD
    var previousZoomExpanded by remember { mutableStateOf(isZoomExpanded) }

    LaunchedEffect(isZoomExpanded) {
        if (isZoomExpanded != previousZoomExpanded) {
            manualExpansionOverrides.clear()
            previousZoomExpanded = isZoomExpanded
        }
    }

    val tickLevel = remember(scaleX) { TimelineTickLevel.fromScale(scaleX) }
    val topOverlayHeightDp = with(density) { topOverlayHeightPx.toDp() }

    fun computeCenteredScrollForMinute(minute: Float, targetScale: Float): Float {
        if (viewportWidthPx <= 0f) return 0f
        val hourPx = with(density) { (TimelineMath.BASE_HOUR_WIDTH_DP * targetScale).dp.toPx() }
        val totalPx = hourPx * 24f
        val maxScroll = (totalPx - viewportWidthPx).coerceAtLeast(0f)
        val targetX = (minute / 60f) * hourPx - (viewportWidthPx / 2f)
        return targetX.coerceIn(0f, maxScroll)
    }

    LaunchedEffect(viewportWidthPx) {
        if (viewportWidthPx > 0f && !hasInitializedScroll) {
            scrollOffsetPx = computeCenteredScrollForMinute(
                minute = uiState.currentSecondsOfDay / 60f,
                targetScale = scaleX
            )
            hasInitializedScroll = true
        }
    }

    fun animateZoomTo(targetScaleRaw: Float) {
        val targetScale = targetScaleRaw.coerceIn(TimelineMath.MIN_SCALE_X, TimelineMath.MAX_SCALE_X)
        val oldScale = scaleX
        val crossesExpansion =
            (oldScale < TimelineMath.DETAIL_EXPANSION_SCALE_THRESHOLD && targetScale >= TimelineMath.DETAIL_EXPANSION_SCALE_THRESHOLD) ||
                (oldScale >= TimelineMath.DETAIL_EXPANSION_SCALE_THRESHOLD && targetScale < TimelineMath.DETAIL_EXPANSION_SCALE_THRESHOLD)
        haptic.performHapticFeedback(
            if (crossesExpansion) HapticFeedbackType.LongPress
            else HapticFeedbackType.TextHandleMove
        )
        val centerWorldX = scrollOffsetPx + (viewportWidthPx / 2f)
        val ratio = targetScale / oldScale
        val newHourPx = with(density) { (TimelineMath.BASE_HOUR_WIDTH_DP * targetScale).dp.toPx() }
        val maxScroll = (newHourPx * 24f - viewportWidthPx).coerceAtLeast(0f)
        val targetScroll = (centerWorldX * ratio - (viewportWidthPx / 2f)).coerceIn(0f, maxScroll)

        activeScrollJob?.cancel()
        activeScrollJob = scope.launch {
            val scaleAnim = Animatable(oldScale)
            val scrollAnim = Animatable(scrollOffsetPx)
            launch {
                scaleAnim.animateTo(targetScale, tween(220)) {
                    scaleX = value
                }
            }
            launch {
                scrollAnim.animateTo(targetScroll, tween(220)) {
                    scrollOffsetPx = value
                }
            }
        }
    }

    fun animateJumpToNow() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val targetScroll = computeCenteredScrollForMinute(
            minute = uiState.currentSecondsOfDay / 60f,
            targetScale = scaleX
        )
        activeScrollJob?.cancel()
        activeScrollJob = scope.launch {
            val anim = Animatable(scrollOffsetPx)
            anim.animateTo(targetScroll, tween(320)) {
                scrollOffsetPx = value
            }
        }
    }

    Scaffold(
        containerColor = ObsidianVoid,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateSheet() },
                shape = RoundedCornerShape(18.dp),
                containerColor = CyanChronos,
                contentColor = ObsidianVoid,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("create_block_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.cd_create_block),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Full-bleed timeline canvas so grid lines & glow gently glide behind translucent top HUD
            HorizontalTimelineCanvas(
                blocks = uiState.blocks,
                currentSecondsOfDay = uiState.currentSecondsOfDay,
                scaleX = scaleX,
                scrollOffsetPx = scrollOffsetPx,
                manualExpansionOverrides = manualExpansionOverrides,
                selectedCategoryFilter = uiState.selectedCategoryFilter,
                onClearCategoryFilter = { viewModel.selectCategoryFilter(null) },
                onTransformGesture = { newScale, newScroll ->
                    activeScrollJob?.cancel()
                    scaleX = newScale
                    scrollOffsetPx = newScroll
                },
                onToggleBlockExpand = { block ->
                    val currentlyExpanded = manualExpansionOverrides[block.id] ?: isZoomExpanded
                    val nextExpanded = !currentlyExpanded
                    if (nextExpanded == isZoomExpanded) {
                        manualExpansionOverrides.remove(block.id)
                    } else {
                        manualExpansionOverrides[block.id] = nextExpanded
                    }
                },
                onEditBlockClick = { block -> viewModel.openEditSheet(block) },
                onEmptySlotLongPress = { minute -> viewModel.openCreateSheet(minute) },
                onViewportWidthChanged = { widthPx -> viewportWidthPx = widthPx },
                topContentPaddingDp = topOverlayHeightDp
            )

            // Floating Translucent HarmonyOS Top HUD (Countdown + Progress + Category Filter Chips)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { size -> topOverlayHeightPx = size.height }
            ) {
                CountdownHeaderBar(
                    countdownState = uiState.countdownState,
                    currentSecondsOfDay = uiState.currentSecondsOfDay,
                    scaleX = scaleX,
                    tickLevel = tickLevel,
                    onJumpToNow = { animateJumpToNow() }
                )

                CategoryFilterBar(
                    selectedCategory = uiState.selectedCategoryFilter,
                    totalCount = uiState.totalBlocksCount,
                    categoryCounts = uiState.categoryCounts,
                    onSelectCategory = { category -> viewModel.selectCategoryFilter(category) }
                )
            }

            // Bottom-left Translucent Zoom Controls Pill
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = HarmonyGlassCard.copy(alpha = 0.74f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = { animateZoomTo(scaleX / 1.6f) },
                            modifier = Modifier.testTag("zoom_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomOut,
                                contentDescription = stringResource(R.string.cd_zoom_out),
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        TextButton(
                            onClick = { animateZoomTo(1.0f) },
                            modifier = Modifier.testTag("reset_zoom_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_reset_zoom),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanChronos
                            )
                        }
                        IconButton(
                            onClick = { animateZoomTo(scaleX * 1.6f) },
                            modifier = Modifier.testTag("zoom_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomIn,
                                contentDescription = stringResource(R.string.cd_zoom_in),
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (!isZoomExpanded && uiState.blocks.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.timeline_pinch_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }

    uiState.editorDraft?.let { draft ->
        BlockEditorSheet(
            draft = draft,
            validationErrorResId = uiState.validationErrorResId,
            onDraftChange = { viewModel.updateDraft(it) },
            onVoiceTranscript = { field, transcript ->
                viewModel.appendVoiceTranscript(field, transcript)
            },
            onSave = {
                viewModel.saveDraft { isUpdate ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(
                                if (isUpdate) R.string.msg_block_updated else R.string.msg_block_saved
                            )
                        )
                    }
                }
            },
            onDelete = {
                viewModel.deleteEditingBlock {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_block_deleted))
                    }
                }
            },
            onDismiss = { viewModel.dismissEditorSheet() }
        )
    }
}
