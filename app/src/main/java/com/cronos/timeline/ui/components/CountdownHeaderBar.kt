package com.cronos.timeline.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cronos.timeline.model.BlockCategory
import com.cronos.timeline.model.CountdownState
import com.cronos.timeline.model.TimelineMath
import com.cronos.timeline.model.TimelineTickLevel
import com.cronos.timeline.ui.theme.AmberChronos
import com.cronos.timeline.ui.theme.CyanChronos
import com.cronos.timeline.ui.theme.HarmonyGlassBase
import com.cronos.timeline.ui.theme.HarmonyGlassCard
import com.cronos.timeline.ui.theme.TextMuted
import com.cronos.timeline.ui.theme.TextPrimary
import com.cronos.timeline.ui.theme.TextSecondary
import com.example.R
import kotlin.math.roundToInt

@Composable
fun CountdownHeaderBar(
    countdownState: CountdownState,
    currentSecondsOfDay: Int,
    scaleX: Float,
    tickLevel: TimelineTickLevel,
    onJumpToNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor by animateColorAsState(
        targetValue = when (countdownState) {
            is CountdownState.ActiveBlockClosing ->
                TimelineMath.resolveBlockAccentColor(countdownState.block)
            is CountdownState.NextEventUpcoming -> AmberChronos
            is CountdownState.DayRemaining -> CyanChronos
        },
        label = "header_accent"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = countdownState.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "countdown_progress"
    )

    val statusLabel = when (countdownState) {
        is CountdownState.ActiveBlockClosing -> stringResource(R.string.countdown_label_closing)
        is CountdownState.NextEventUpcoming -> stringResource(R.string.countdown_label_next)
        is CountdownState.DayRemaining -> stringResource(R.string.countdown_label_day_end)
    }

    val targetTitle = when (countdownState) {
        is CountdownState.ActiveBlockClosing -> countdownState.block.title
        is CountdownState.NextEventUpcoming ->
            "${countdownState.block.title} · ${TimelineMath.formatMinuteOfDay(countdownState.block.startMinute)}"
        is CountdownState.DayRemaining ->
            if (countdownState.hasBlocksToday) stringResource(R.string.countdown_day_remaining)
            else stringResource(R.string.countdown_no_events)
    }

    val progressPct = (countdownState.progress * 100f).roundToInt().coerceIn(0, 100)
    val dayPct = (countdownState.dayProgress * 100f).roundToInt().coerceIn(0, 100)

    val progressSummaryText = when (countdownState) {
        is CountdownState.ActiveBlockClosing -> stringResource(R.string.progress_block_label, progressPct)
        is CountdownState.NextEventUpcoming -> stringResource(R.string.progress_next_label, progressPct)
        is CountdownState.DayRemaining -> stringResource(R.string.progress_day_label, progressPct)
    }

    val tickUnitText = when (tickLevel) {
        TimelineTickLevel.HOUR_60 -> stringResource(R.string.tick_unit_60m)
        TimelineTickLevel.HALF_HOUR_30 -> stringResource(R.string.tick_unit_30m)
        TimelineTickLevel.QUARTER_HOUR_15 -> stringResource(R.string.tick_unit_15m)
        TimelineTickLevel.FIVE_MIN_5 -> stringResource(R.string.tick_unit_5m)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("countdown_header_bar")
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = HarmonyGlassBase.copy(alpha = 0.72f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        accentColor.copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.12f)
                    )
                )
            ),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                HarmonyGlassCard.copy(alpha = 0.38f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top Row: App Brand + Live Clock + Zoom Granularity Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = stringResource(R.string.app_name).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = HarmonyGlassCard.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = CyanChronos,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = TimelineMath.formatHms(currentSecondsOfDay),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanChronos,
                                    modifier = Modifier.testTag("live_clock_text")
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accentColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.42f)),
                        modifier = Modifier.testTag("zoom_level_badge")
                    ) {
                        Text(
                            text = stringResource(R.string.zoom_badge_format, scaleX, tickUnitText),
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Countdown Display + Jump-to-Now CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = countdownState.formattedRemaining,
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary,
                            modifier = Modifier.testTag("countdown_hms_text")
                        )

                        Text(
                            text = targetTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("countdown_target_title")
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedButton(
                        onClick = onJumpToNow,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, CyanChronos.copy(alpha = 0.48f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CyanChronos.copy(alpha = 0.14f),
                            contentColor = CyanChronos
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("jump_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = stringResource(R.string.cd_jump_now),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.action_jump_now),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-Time Animated Progress Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = progressSummaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.testTag("countdown_progress_percent")
                    )
                    if (countdownState !is CountdownState.DayRemaining) {
                        Text(
                            text = stringResource(R.string.progress_day_chip, dayPct),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .testTag("countdown_progress_bar"),
                    color = accentColor,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }
        }
    }
}
