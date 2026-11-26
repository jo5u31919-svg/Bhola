package com.cronos.timeline.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pinch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cronos.timeline.data.local.TimelineBlockEntity
import com.cronos.timeline.model.BlockCategory
import com.cronos.timeline.model.PlacedTimelineBlock
import com.cronos.timeline.model.TimelineMath
import com.cronos.timeline.model.TimelineTickLevel
import com.cronos.timeline.ui.theme.AmberChronos
import com.cronos.timeline.ui.theme.CyanChronos
import com.cronos.timeline.ui.theme.HarmonyAmbientGlow
import com.cronos.timeline.ui.theme.HarmonyAmbientViolet
import com.cronos.timeline.ui.theme.HarmonyGlassCard
import com.cronos.timeline.ui.theme.ObsidianVoid
import com.cronos.timeline.ui.theme.SlateBorder
import com.cronos.timeline.ui.theme.SlateGridLine
import com.cronos.timeline.ui.theme.TextMuted
import com.cronos.timeline.ui.theme.TextPrimary
import com.cronos.timeline.ui.theme.TextSecondary
import com.example.R
import kotlin.math.roundToInt

@Composable
fun HorizontalTimelineCanvas(
    blocks: List<TimelineBlockEntity>,
    currentSecondsOfDay: Int,
    scaleX: Float,
    scrollOffsetPx: Float,
    manualExpansionOverrides: Map<Long, Boolean>,
    selectedCategoryFilter: BlockCategory?,
    onClearCategoryFilter: () -> Unit,
    onTransformGesture: (newScale: Float, newScrollOffsetPx: Float) -> Unit,
    onToggleBlockExpand: (TimelineBlockEntity) -> Unit,
    onEditBlockClick: (TimelineBlockEntity) -> Unit,
    onEmptySlotLongPress: (minuteOfDay: Int) -> Unit,
    onViewportWidthChanged: (Float) -> Unit,
    topContentPaddingDp: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val tickLevel = remember(scaleX) { TimelineTickLevel.fromScale(scaleX) }
    val hourWidthDp = TimelineMath.BASE_HOUR_WIDTH_DP * scaleX
    val minBlockVisualWidthDp = 84f
    val minVisualMinutes = ((minBlockVisualWidthDp / hourWidthDp) * 60f).roundToInt().coerceAtLeast(1)

    val (placedBlocks, laneCount) = remember(blocks, minVisualMinutes) {
        val sorted = blocks.sortedWith(
            compareBy<TimelineBlockEntity> { it.startMinute }.thenBy { it.endMinute }
        )
        val laneEnds = mutableListOf<Int>()
        val result = sorted.map { block ->
            val effectiveEnd = maxOf(block.endMinute, block.startMinute + minVisualMinutes)
            var lane = -1
            for (i in laneEnds.indices) {
                if (laneEnds[i] <= block.startMinute) {
                    lane = i
                    laneEnds[i] = effectiveEnd
                    break
                }
            }
            if (lane == -1) {
                lane = laneEnds.size
                laneEnds.add(effectiveEnd)
            }
            PlacedTimelineBlock(block = block, laneIndex = lane)
        }
        result to laneEnds.size.coerceAtLeast(1)
    }

    val isZoomExpanded = scaleX >= TimelineMath.DETAIL_EXPANSION_SCALE_THRESHOLD
    val hasAnyExpandedBlock = isZoomExpanded || manualExpansionOverrides.values.any { it }

    val laneSlotHeightDp by animateDpAsState(
        targetValue = if (hasAnyExpandedBlock) 154.dp else 88.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "lane_slot_height"
    )
    val laneSpacingDp = 12f
    val rulerHeightDp = 46f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianVoid, HarmonyAmbientGlow.copy(alpha = 0.55f), ObsidianVoid)
                )
            )
            .clipToBounds()
            .testTag("horizontal_timeline_canvas")
    ) {
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        onViewportWidthChanged(viewportWidthPx)

        val currentScale by rememberUpdatedState(scaleX)
        val currentScroll by rememberUpdatedState(scrollOffsetPx)
        val currentViewportPx by rememberUpdatedState(viewportWidthPx)
        val haptic = LocalHapticFeedback.current

        val hourWidthPx = with(density) { hourWidthDp.dp.toPx() }

        val gestureModifier = Modifier
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = currentScale
                    val newScale = (oldScale * zoom).coerceIn(
                        TimelineMath.MIN_SCALE_X,
                        TimelineMath.MAX_SCALE_X
                    )
                    if (zoom != 1f) {
                        val oldTickLevel = TimelineTickLevel.fromScale(oldScale)
                        val newTickLevel = TimelineTickLevel.fromScale(newScale)
                        val wasZoomExpanded = oldScale >= TimelineMath.DETAIL_EXPANSION_SCALE_THRESHOLD
                        val isNowZoomExpanded = newScale >= TimelineMath.DETAIL_EXPANSION_SCALE_THRESHOLD
                        val hitMinBound = newScale == TimelineMath.MIN_SCALE_X && oldScale > TimelineMath.MIN_SCALE_X
                        val hitMaxBound = newScale == TimelineMath.MAX_SCALE_X && oldScale < TimelineMath.MAX_SCALE_X

                        when {
                            isNowZoomExpanded != wasZoomExpanded -> {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            newTickLevel != oldTickLevel -> {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            hitMinBound || hitMaxBound -> {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }

                    val ratio = newScale / oldScale
                    val newHourWidthPx = with(density) {
                        (TimelineMath.BASE_HOUR_WIDTH_DP * newScale).dp.toPx()
                    }
                    val newTotalWidthPx = newHourWidthPx * 24f
                    val maxScroll = (newTotalWidthPx - currentViewportPx).coerceAtLeast(0f)
                    val rawScroll = (currentScroll + centroid.x) * ratio - centroid.x - pan.x
                    onTransformGesture(newScale, rawScroll.coerceIn(0f, maxScroll))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val worldX = currentScroll + offset.x
                        val currentHourPx = with(density) {
                            (TimelineMath.BASE_HOUR_WIDTH_DP * currentScale).dp.toPx()
                        }
                        val tappedMinutes = ((worldX / currentHourPx) * 60f)
                            .roundToInt()
                            .coerceIn(0, TimelineMath.MINUTES_IN_DAY - 15)
                        val snapped = (tappedMinutes / 5) * 5
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEmptySlotLongPress(snapped)
                    }
                )
            }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier)
        ) {
            val contentVerticalScroll = rememberScrollState()
            val totalLanesHeightDp =
                laneCount * laneSlotHeightDp.value + (laneCount - 1).coerceAtLeast(0) * laneSpacingDp + 140f

            // 1. Background Ambient Glow & Grid Lines extending subtly behind the translucent top HUD
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyanChronos.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.25f, size.height * 0.18f),
                        radius = size.width * 0.7f
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.25f, size.height * 0.18f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HarmonyAmbientViolet.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.55f),
                        radius = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width * 0.85f, size.height * 0.55f)
                )

                val topPadPx = topContentPaddingDp.toPx()
                val rulerHeightPx = topPadPx + rulerHeightDp.dp.toPx()
                val stepMinutes = tickLevel.intervalMinutes
                val startVisibleMin = ((scrollOffsetPx / hourWidthPx) * 60f).toInt()
                    .coerceAtLeast(0) / stepMinutes * stepMinutes
                val endVisibleMin = (((scrollOffsetPx + size.width) / hourWidthPx) * 60f).toInt()
                    .coerceAtMost(TimelineMath.MINUTES_IN_DAY)

                drawLine(
                    color = Color.White.copy(alpha = 0.14f),
                    start = Offset(0f, rulerHeightPx),
                    end = Offset(size.width, rulerHeightPx),
                    strokeWidth = 1.dp.toPx()
                )

                var minute = startVisibleMin
                while (minute <= endVisibleMin) {
                    val x = (minute / 60f) * hourWidthPx - scrollOffsetPx
                    if (x in -20f..(size.width + 20f)) {
                        val isHour = minute % 60 == 0
                        val isHalfHour = minute % 30 == 0
                        val isQuarterHour = minute % 15 == 0

                        val tickHeight = when {
                            isHour -> 14.dp.toPx()
                            isHalfHour -> 10.dp.toPx()
                            isQuarterHour -> 7.dp.toPx()
                            else -> 5.dp.toPx()
                        }

                        val gridColor = when {
                            isHour -> SlateBorder.copy(alpha = 0.85f)
                            isHalfHour -> SlateGridLine.copy(alpha = 0.85f)
                            isQuarterHour -> SlateGridLine.copy(alpha = 0.55f)
                            else -> SlateGridLine.copy(alpha = 0.35f)
                        }

                        drawLine(
                            color = if (isHour) CyanChronos.copy(alpha = 0.85f) else TextMuted,
                            start = Offset(x, rulerHeightPx - tickHeight),
                            end = Offset(x, rulerHeightPx),
                            strokeWidth = if (isHour) 1.5.dp.toPx() else 1.dp.toPx()
                        )

                        // Full-height vertical grid line gliding behind translucent header & cards
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = if (isHour) 1.dp.toPx() else 0.75.dp.toPx(),
                            pathEffect = if (isHour) null else PathEffect.dashPathEffect(
                                floatArrayOf(6f, 6f),
                                0f
                            )
                        )
                    }
                    minute += stepMinutes
                }
            }

            // 2. Ruler Labels (00:00 .. 23:59 + progressive 30m, 15m, 5m labels)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topContentPaddingDp)
                    .height(rulerHeightDp.dp)
            ) {
                val stepMinutes = tickLevel.intervalMinutes
                val startVisibleMin = ((scrollOffsetPx / hourWidthPx) * 60f).toInt()
                    .coerceAtLeast(0) / stepMinutes * stepMinutes
                val endVisibleMin = (((scrollOffsetPx + viewportWidthPx) / hourWidthPx) * 60f).toInt()
                    .coerceAtMost(TimelineMath.MINUTES_IN_DAY)

                var m = startVisibleMin
                while (m <= endVisibleMin) {
                    val xPx = (m / 60f) * hourWidthPx - scrollOffsetPx
                    val isHour = m % 60 == 0
                    val label = if (m == TimelineMath.MINUTES_IN_DAY) "23:59" else TimelineMath.formatMinuteOfDay(m)
                    val labelWidthPx = with(density) { 44.dp.toPx() }

                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isHour) FontWeight.Bold else FontWeight.Normal,
                        color = if (isHour) TextPrimary else TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(44.dp)
                            .offset {
                                IntOffset(
                                    x = (xPx - labelWidthPx / 2f).roundToInt(),
                                    y = with(density) { 8.dp.toPx().roundToInt() }
                                )
                            }
                    )
                    m += stepMinutes
                }
            }

            // 3. Timeline Blocks Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topContentPaddingDp + rulerHeightDp.dp)
                    .verticalScroll(contentVerticalScroll)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalLanesHeightDp.dp)
                ) {
                    placedBlocks.forEach { placed ->
                        val block = placed.block
                        val startXPx = (block.startMinute / 60f) * hourWidthPx - scrollOffsetPx
                        val endXPx = (block.endMinute / 60f) * hourWidthPx - scrollOffsetPx
                        val rawWidthDp = with(density) { (endXPx - startXPx).toDp() }
                        val baseWidthDp = maxOf(rawWidthDp, minBlockVisualWidthDp.dp)

                        val isExpanded = manualExpansionOverrides[block.id] ?: isZoomExpanded
                        val effectiveRenderWidthPx = with(density) {
                            (if (isExpanded) maxOf(baseWidthDp, 340.dp) else baseWidthDp).toPx()
                        }

                        // Generous offscreen culling so expanding cards animate smoothly
                        val blockRightPx = startXPx + effectiveRenderWidthPx
                        if (blockRightPx >= -120f && startXPx <= viewportWidthPx + 120f) {
                            val topOffsetDp = 16f + placed.laneIndex * (laneSlotHeightDp.value + laneSpacingDp)
                            val nowMin = currentSecondsOfDay / 60
                            val isActiveNow = nowMin in block.startMinute until block.endMinute

                            TimelineBlockCard(
                                block = block,
                                baseWidthDp = baseWidthDp,
                                isExpanded = isExpanded,
                                isActiveNow = isActiveNow,
                                onToggleExpand = onToggleBlockExpand,
                                onEditClick = onEditBlockClick,
                                modifier = Modifier
                                    .zIndex(if (isExpanded) 5f else 1f)
                                    .offset {
                                        IntOffset(
                                            x = startXPx.roundToInt(),
                                            y = with(density) { topOffsetDp.dp.toPx().roundToInt() }
                                        )
                                    }
                            )
                        }
                    }
                }
            }

            // 4. Empty State Overlay
            if (blocks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = HarmonyGlassCard.copy(alpha = 0.76f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = topContentPaddingDp)
                        .padding(horizontal = 28.dp)
                        .testTag("timeline_empty_state")
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedCategoryFilter != null) {
                            Text(
                                text = stringResource(
                                    R.string.filter_empty_title,
                                    stringResource(selectedCategoryFilter.labelResId)
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            androidx.compose.material3.TextButton(
                                onClick = onClearCategoryFilter,
                                modifier = Modifier.testTag("clear_category_filter_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.filter_empty_action),
                                    color = selectedCategoryFilter.accentColor
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Pinch,
                                contentDescription = null,
                                tint = CyanChronos,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = stringResource(R.string.timeline_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.timeline_empty_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 5. Real-Time Current Time Indicator Line ("Ahora" Needle)
            val nowMinutesFloat = currentSecondsOfDay / 60f
            val nowXPx = (nowMinutesFloat / 60f) * hourWidthPx - scrollOffsetPx
            if (nowXPx in -40f..(viewportWidthPx + 40f)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val rulerY = topContentPaddingDp.toPx() + rulerHeightDp.dp.toPx()
                    drawLine(
                        color = AmberChronos.copy(alpha = 0.25f),
                        start = Offset(nowXPx, 0f),
                        end = Offset(nowXPx, size.height),
                        strokeWidth = 6.dp.toPx()
                    )
                    drawLine(
                        color = AmberChronos,
                        start = Offset(nowXPx, 0f),
                        end = Offset(nowXPx, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(
                        color = AmberChronos,
                        radius = 5.dp.toPx(),
                        center = Offset(nowXPx, rulerY)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberChronos,
                    modifier = Modifier
                        .offset {
                            val badgeWidthPx = with(density) { 64.dp.toPx() }
                            val badgeY = with(density) {
                                (topContentPaddingDp + (rulerHeightDp - 20f).dp).toPx()
                            }
                            IntOffset(
                                x = (nowXPx - badgeWidthPx / 2f).roundToInt(),
                                y = badgeY.roundToInt()
                            )
                        }
                        .testTag("now_needle_badge")
                ) {
                    Text(
                        text = TimelineMath.formatHms(currentSecondsOfDay),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianVoid,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(64.dp)
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
