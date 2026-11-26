package com.cronos.timeline.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cronos.timeline.data.local.TimelineBlockEntity
import com.cronos.timeline.model.BlockCategory
import com.cronos.timeline.model.TimelineMath
import com.cronos.timeline.ui.theme.ObsidianSurfaceElevated
import com.cronos.timeline.ui.theme.ObsidianSurfaceHighest
import com.cronos.timeline.ui.theme.SlateBorder
import com.cronos.timeline.ui.theme.TextMuted
import com.cronos.timeline.ui.theme.TextPrimary
import com.cronos.timeline.ui.theme.TextSecondary
import com.example.R

@Composable
fun TimelineBlockCard(
    block: TimelineBlockEntity,
    baseWidthDp: Dp,
    isExpanded: Boolean,
    isActiveNow: Boolean,
    onToggleExpand: (TimelineBlockEntity) -> Unit,
    onEditClick: (TimelineBlockEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val category = BlockCategory.fromEntity(block)
    val accent = TimelineMath.resolveBlockAccentColor(block)
    val hasCustomColor = block.customColorHex != null
    val durationMinutes = (block.endMinute - block.startMinute).coerceAtLeast(1)
    val durationText = if (durationMinutes >= 60) {
        val h = durationMinutes / 60
        val m = durationMinutes % 60
        if (m == 0) "${h}h" else stringResource(R.string.duration_hours_minutes, h, m)
    } else {
        stringResource(R.string.duration_minutes_only, durationMinutes)
    }

    val compactTimeRange =
        "${TimelineMath.formatMinuteOfDay(block.startMinute)} - ${TimelineMath.formatMinuteOfDay(block.endMinute)}"
    val exactTimeRange =
        "${TimelineMath.formatExactMinuteWithSeconds(block.startMinute)} → ${TimelineMath.formatExactMinuteWithSeconds(block.endMinute)} · $durationText"

    val summaryWidthDp = if (isExpanded) {
        maxOf(132.dp, minOf(baseWidthDp, 180.dp))
    } else {
        baseWidthDp
    }
    val detailWidthDp = if (baseWidthDp > 340.dp) {
        (baseWidthDp - summaryWidthDp).coerceAtLeast(210.dp)
    } else {
        220.dp
    }

    val cardHeightDp = if (isExpanded) 146.dp else 76.dp
    val baseGlassColor = if (isActiveNow || isExpanded) {
        com.cronos.timeline.ui.theme.HarmonyGlassCardActive.copy(alpha = 0.80f)
    } else {
        com.cronos.timeline.ui.theme.HarmonyGlassCard.copy(alpha = 0.70f)
    }
    val cardContainerColor = if (hasCustomColor) {
        androidx.compose.ui.graphics.lerp(baseGlassColor, accent.copy(alpha = baseGlassColor.alpha), 0.16f)
    } else {
        baseGlassColor
    }

    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                haptic.performHapticFeedback(
                    if (!isExpanded) HapticFeedbackType.LongPress
                    else HapticFeedbackType.TextHandleMove
                )
                onToggleExpand(block)
            }
            .testTag("timeline_block_${block.id}"),
        shape = RoundedCornerShape(18.dp),
        color = cardContainerColor,
        border = BorderStroke(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    androidx.compose.ui.graphics.Color.White.copy(
                        alpha = if (isActiveNow || isExpanded) 0.24f else 0.17f
                    ),
                    accent.copy(alpha = if (isActiveNow || isExpanded || hasCustomColor) 0.65f else 0.34f),
                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.10f)
                )
            )
        ),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.height(cardHeightDp)
        ) {
            // Left Category Accent Pillar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accent)
            )

            // Compact Header Column (always visible: title + time + indicators)
            Column(
                modifier = Modifier
                    .width(summaryWidthDp - 5.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = if (isExpanded) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.UnfoldLess else Icons.Rounded.UnfoldMore,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = if (isExpanded) exactTimeRange else compactTimeRange,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isExpanded) accent else TextSecondary,
                        maxLines = if (isExpanded) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = accent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = stringResource(category.labelResId),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (block.createdWithVoice) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = stringResource(R.string.block_voice_badge),
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (block.countdownEnabled) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = stringResource(R.string.block_countdown_badge),
                            tint = accent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Horizontal Expanding Detail Panel (triggered by zoom >= 1.8f OR manual tap)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                    expandHorizontally(
                        expandFrom = Alignment.Start,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                exit = fadeOut(animationSpec = tween(durationMillis = 140)) +
                    shrinkHorizontally(
                        shrinkTowards = Alignment.Start,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(min = 190.dp, max = detailWidthDp)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp)
                            .background(SlateBorder)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        val noteScroll = rememberScrollState()
                        if (block.detailedNote.isNotBlank()) {
                            Text(
                                text = block.detailedNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary.copy(alpha = 0.94f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(noteScroll)
                                    .testTag("block_detail_note_${block.id}")
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.field_note_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("block_detail_note_${block.id}")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val editDesc = stringResource(R.string.cd_edit_block, block.title)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { onEditClick(block) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = accent.copy(alpha = 0.14f),
                                    contentColor = accent
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .semantics { contentDescription = editDesc }
                                    .testTag("edit_block_button_${block.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.action_edit_block),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
