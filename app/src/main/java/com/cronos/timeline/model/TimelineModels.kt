package com.cronos.timeline.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.cronos.timeline.data.local.TimelineBlockEntity
import com.cronos.timeline.ui.theme.AmberChronos
import com.cronos.timeline.ui.theme.CoralChronos
import com.cronos.timeline.ui.theme.CyanChronos
import com.cronos.timeline.ui.theme.MintChronos
import com.cronos.timeline.ui.theme.VioletChronos
import com.example.R
import java.util.Calendar
import java.util.Locale

enum class BlockCategory(
    val index: Int,
    val storageKey: String,
    val colorHex: Long,
    val accentColor: Color,
    @StringRes val labelResId: Int
) {
    WORK(0, "WORK", 0xFF38BDF8L, CyanChronos, R.string.category_work),
    MEETING(1, "MEETING", 0xFFA78BFAL, VioletChronos, R.string.category_meeting),
    PERSONAL(2, "PERSONAL", 0xFF34D399L, MintChronos, R.string.category_personal),
    URGENT(3, "URGENT", 0xFFFB7185L, CoralChronos, R.string.category_urgent),
    VOICE_NOTE(4, "VOICE_NOTE", 0xFFF59E0BL, AmberChronos, R.string.category_voice);

    companion object {
        fun fromIndex(index: Int): BlockCategory =
            entries.getOrElse(index) { WORK }

        fun fromStorageKey(key: String): BlockCategory =
            entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) } ?: WORK

        fun fromEntity(entity: TimelineBlockEntity): BlockCategory =
            entries.firstOrNull { it.storageKey.equals(entity.category, ignoreCase = true) }
                ?: fromIndex(entity.categoryIndex)
    }
}

enum class TimelineTickLevel(val intervalMinutes: Int) {
    HOUR_60(60),
    HALF_HOUR_30(30),
    QUARTER_HOUR_15(15),
    FIVE_MIN_5(5);

    companion object {
        fun fromScale(scaleX: Float): TimelineTickLevel = when {
            scaleX >= 6.0f -> FIVE_MIN_5
            scaleX >= 3.2f -> QUARTER_HOUR_15
            scaleX >= 1.6f -> HALF_HOUR_30
            else -> HOUR_60
        }
    }
}

data class PlacedTimelineBlock(
    val block: TimelineBlockEntity,
    val laneIndex: Int
)

sealed interface CountdownState {
    val formattedRemaining: String
    val progress: Float
    val dayProgress: Float

    data class ActiveBlockClosing(
        val block: TimelineBlockEntity,
        val remainingSeconds: Int,
        override val progress: Float,
        override val dayProgress: Float,
        override val formattedRemaining: String
    ) : CountdownState

    data class NextEventUpcoming(
        val block: TimelineBlockEntity,
        val remainingSeconds: Int,
        override val progress: Float,
        override val dayProgress: Float,
        override val formattedRemaining: String
    ) : CountdownState

    data class DayRemaining(
        val remainingSeconds: Int,
        val hasBlocksToday: Boolean,
        override val progress: Float,
        override val dayProgress: Float,
        override val formattedRemaining: String
    ) : CountdownState
}

data class EditorDraft(
    val editingId: Long? = null,
    val title: String = "",
    val detailedNote: String = "",
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 10 * 60,
    val countdownEnabled: Boolean = true,
    val category: BlockCategory = BlockCategory.WORK,
    val customColorHex: Long? = null,
    val createdWithVoice: Boolean = false
) {
    val effectiveAccentColor: Color
        get() = customColorHex?.let { Color(it) } ?: category.accentColor
}

object TimelineMath {
    const val MINUTES_IN_DAY = 24 * 60
    const val SECONDS_IN_DAY = 24 * 3600
    const val BASE_HOUR_WIDTH_DP = 116f
    const val MIN_SCALE_X = 0.65f
    const val MAX_SCALE_X = 12.0f
    const val DETAIL_EXPANSION_SCALE_THRESHOLD = 1.8f

    fun currentDayKey(calendar: Calendar = Calendar.getInstance()): Int {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return year * 10000 + month * 100 + day
    }

    fun currentSecondsOfDay(calendar: Calendar = Calendar.getInstance()): Int {
        val h = calendar.get(Calendar.HOUR_OF_DAY)
        val m = calendar.get(Calendar.MINUTE)
        val s = calendar.get(Calendar.SECOND)
        return h * 3600 + m * 60 + s
    }

    fun formatHms(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val s = safe % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    fun formatMinuteOfDay(minuteOfDay: Int): String {
        val clamped = minuteOfDay.coerceIn(0, MINUTES_IN_DAY)
        if (clamped == MINUTES_IN_DAY) return "23:59"
        val h = clamped / 60
        val m = clamped % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun formatExactMinuteWithSeconds(minuteOfDay: Int): String {
        val clamped = minuteOfDay.coerceIn(0, MINUTES_IN_DAY)
        if (clamped == MINUTES_IN_DAY) return "23:59:59"
        val h = clamped / 60
        val m = clamped % 60
        return String.format(Locale.US, "%02d:%02d:00", h, m)
    }

    fun assignLanes(blocks: List<TimelineBlockEntity>): Pair<List<PlacedTimelineBlock>, Int> {
        if (blocks.isEmpty()) return emptyList<PlacedTimelineBlock>() to 1
        val sorted = blocks.sortedWith(
            compareBy<TimelineBlockEntity> { it.startMinute }.thenBy { it.endMinute }
        )
        val laneEndMinutes = mutableListOf<Int>()
        val placed = sorted.map { block ->
            var assignedLane = -1
            for (i in laneEndMinutes.indices) {
                if (laneEndMinutes[i] <= block.startMinute) {
                    assignedLane = i
                    laneEndMinutes[i] = block.endMinute
                    break
                }
            }
            if (assignedLane == -1) {
                assignedLane = laneEndMinutes.size
                laneEndMinutes.add(block.endMinute)
            }
            PlacedTimelineBlock(block = block, laneIndex = assignedLane)
        }
        return placed to laneEndMinutes.size.coerceAtLeast(1)
    }

    fun computeCountdown(
        blocks: List<TimelineBlockEntity>,
        currentSecondsOfDay: Int
    ): CountdownState {
        val safeNow = currentSecondsOfDay.coerceIn(0, SECONDS_IN_DAY - 1)
        val dayProgress = (safeNow.toFloat() / SECONDS_IN_DAY.toFloat()).coerceIn(0f, 1f)

        val activeBlocks = blocks.filter { block ->
            val startSec = block.startMinute * 60
            val endSec = if (block.endMinute >= MINUTES_IN_DAY) SECONDS_IN_DAY else block.endMinute * 60
            safeNow in startSec until endSec
        }

        if (activeBlocks.isNotEmpty()) {
            val prioritized = activeBlocks
                .sortedWith(
                    compareByDescending<TimelineBlockEntity> { it.countdownEnabled }
                        .thenBy { it.endMinute }
                )
                .first()
            val startSec = prioritized.startMinute * 60
            val endSec = if (prioritized.endMinute >= MINUTES_IN_DAY) SECONDS_IN_DAY else prioritized.endMinute * 60
            val remaining = (endSec - safeNow).coerceAtLeast(0)
            val total = (endSec - startSec).coerceAtLeast(1)
            val progress = ((safeNow - startSec).toFloat() / total.toFloat()).coerceIn(0f, 1f)
            return CountdownState.ActiveBlockClosing(
                block = prioritized,
                remainingSeconds = remaining,
                progress = progress,
                dayProgress = dayProgress,
                formattedRemaining = formatHms(remaining)
            )
        }

        val upcomingBlocks = blocks.filter { it.startMinute * 60 > safeNow }
        if (upcomingBlocks.isNotEmpty()) {
            val prioritized = upcomingBlocks
                .sortedWith(
                    compareByDescending<TimelineBlockEntity> { it.countdownEnabled }
                        .thenBy { it.startMinute }
                )
                .first()
            val targetStartSec = prioritized.startMinute * 60
            val remaining = (targetStartSec - safeNow).coerceAtLeast(0)
            val previousEndSec = blocks
                .filter { it.endMinute * 60 <= safeNow }
                .maxOfOrNull { it.endMinute * 60 } ?: 0
            val span = (targetStartSec - previousEndSec).coerceAtLeast(1)
            val upcomingProgress = ((safeNow - previousEndSec).toFloat() / span.toFloat()).coerceIn(0f, 1f)
            return CountdownState.NextEventUpcoming(
                block = prioritized,
                remainingSeconds = remaining,
                progress = upcomingProgress,
                dayProgress = dayProgress,
                formattedRemaining = formatHms(remaining)
            )
        }

        val dayRemaining = (SECONDS_IN_DAY - safeNow).coerceAtLeast(0)
        return CountdownState.DayRemaining(
            remainingSeconds = dayRemaining,
            hasBlocksToday = blocks.isNotEmpty(),
            progress = dayProgress,
            dayProgress = dayProgress,
            formattedRemaining = formatHms(dayRemaining)
        )
    }

    val CUSTOM_COLOR_PALETTE: List<Long> = listOf(
        0xFF38BDF8L, // Cyan eléctrico
        0xFF60A5FAL, // Azul cielo
        0xFFA78BFAL, // Violeta suave
        0xFFE879F9L, // Orquídea
        0xFFF472B6L, // Rosa vivo
        0xFFFB7185L, // Coral
        0xFFFB923CL, // Mandarina
        0xFFF59E0BL, // Ámbar
        0xFFA3E635L, // Lima
        0xFF34D399L, // Menta
        0xFF2DD4BFL, // Turquesa
        0xFF93C5FDL  // Lavanda claro
    )

    fun resolveBlockAccentColor(block: TimelineBlockEntity): Color {
        val custom = block.customColorHex
        return if (custom != null) {
            Color(custom)
        } else {
            BlockCategory.fromEntity(block).accentColor
        }
    }

    fun formatHexColor(colorHex: Long): String {
        val rgb = (colorHex and 0x00FFFFFFL).toInt()
        return String.format(Locale.US, "#%06X", rgb)
    }

    fun parseHexColor(input: String): Long? {
        val cleaned = input.trim().removePrefix("#")
        if (cleaned.length != 6) return null
        val rgb = cleaned.toLongOrNull(16) ?: return null
        return 0xFF000000L or rgb
    }

    fun hsvToColorHex(hue: Float, saturation: Float, value: Float): Long {
        val argb = android.graphics.Color.HSVToColor(
            floatArrayOf(
                hue.coerceIn(0f, 360f),
                saturation.coerceIn(0f, 1f),
                value.coerceIn(0f, 1f)
            )
        )
        return 0xFF000000L or (argb.toLong() and 0x00FFFFFFL)
    }

    fun colorHexToHsv(colorHex: Long): FloatArray {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV((0xFF000000L or (colorHex and 0x00FFFFFFL)).toInt(), hsv)
        return hsv
    }
}
