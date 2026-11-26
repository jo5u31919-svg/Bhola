package com.example

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cronos.timeline.data.local.CronosDatabase
import com.cronos.timeline.data.local.TimelineBlockEntity
import com.cronos.timeline.model.CountdownState
import com.cronos.timeline.model.TimelineMath
import com.cronos.timeline.model.TimelineTickLevel
import com.cronos.timeline.ui.components.TimelineBlockCard
import com.cronos.timeline.ui.theme.CronosSystemTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: CronosDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CronosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cronos system", appName)
    }

    @Test
    fun `room dao inserts observes updates and deletes timeline blocks`() = runTest {
        val dao = database.timelineBlockDao()
        val dayKey = 20261125
        val insertedId = dao.insertBlock(
            TimelineBlockEntity(
                dayKey = dayKey,
                title = "Deep Work",
                detailedNote = "Implementar zoom horizontal y dictado por voz",
                startMinute = 9 * 60,
                endMinute = 11 * 60,
                countdownEnabled = true,
                categoryIndex = 0,
                createdWithVoice = true
            )
        )

        val blocksAfterInsert = dao.observeBlocksForDay(dayKey).first()
        assertEquals(1, blocksAfterInsert.size)
        assertEquals("Deep Work", blocksAfterInsert.first().title)

        val updated = blocksAfterInsert.first().copy(
            id = insertedId,
            title = "Deep Work Actualizado",
            endMinute = 11 * 60 + 30
        )
        dao.updateBlock(updated)

        val blocksAfterUpdate = dao.observeBlocksForDay(dayKey).first()
        assertEquals("Deep Work Actualizado", blocksAfterUpdate.first().title)
        assertEquals(11 * 60 + 30, blocksAfterUpdate.first().endMinute)

        dao.deleteBlockById(insertedId)
        val blocksAfterDelete = dao.observeBlocksForDay(dayKey).first()
        assertTrue(blocksAfterDelete.isEmpty())
    }

    @Test
    fun `progressive zoom thresholds switch 60m 30m 15m and 5m subdivisions`() {
        assertEquals(TimelineTickLevel.HOUR_60, TimelineTickLevel.fromScale(1.0f))
        assertEquals(TimelineTickLevel.HALF_HOUR_30, TimelineTickLevel.fromScale(1.8f))
        assertEquals(TimelineTickLevel.QUARTER_HOUR_15, TimelineTickLevel.fromScale(3.5f))
        assertEquals(TimelineTickLevel.FIVE_MIN_5, TimelineTickLevel.fromScale(6.2f))
    }

    @Test
    fun `countdown computes active block closing and next upcoming event`() {
        val dayKey = 20261125
        val activeBlock = TimelineBlockEntity(
            id = 1L,
            dayKey = dayKey,
            title = "Bloque Activo",
            detailedNote = "Nota",
            startMinute = 10 * 60,
            endMinute = 11 * 60,
            countdownEnabled = true
        )
        val nextBlock = TimelineBlockEntity(
            id = 2L,
            dayKey = dayKey,
            title = "Reunión",
            detailedNote = "Agenda",
            startMinute = 14 * 60,
            endMinute = 15 * 60,
            countdownEnabled = true
        )

        val activeState = TimelineMath.computeCountdown(
            blocks = listOf(activeBlock, nextBlock),
            currentSecondsOfDay = 10 * 3600 + 30 * 60
        )
        assertTrue(activeState is CountdownState.ActiveBlockClosing)
        assertEquals("00:30:00", activeState.formattedRemaining)
        assertEquals(0.5f, activeState.progress, 0.001f)

        val upcomingState = TimelineMath.computeCountdown(
            blocks = listOf(activeBlock, nextBlock),
            currentSecondsOfDay = 12 * 3600 + 30 * 60
        )
        assertTrue(upcomingState is CountdownState.NextEventUpcoming)
        assertEquals("01:30:00", upcomingState.formattedRemaining)
        // Between 11:00 (end of activeBlock) and 14:00 (start of nextBlock), 12:30 is 50%
        assertEquals(0.5f, upcomingState.progress, 0.001f)
    }

    @Test
    fun `overlapping blocks are placed on separate lanes`() {
        val b1 = TimelineBlockEntity(id = 1, dayKey = 20261125, title = "A", detailedNote = "", startMinute = 600, endMinute = 720)
        val b2 = TimelineBlockEntity(id = 2, dayKey = 20261125, title = "B", detailedNote = "", startMinute = 660, endMinute = 780)
        val b3 = TimelineBlockEntity(id = 3, dayKey = 20261125, title = "C", detailedNote = "", startMinute = 730, endMinute = 800)

        val (placed, laneCount) = TimelineMath.assignLanes(listOf(b1, b2, b3))
        assertEquals(2, laneCount)
        assertEquals(0, placed.first { it.block.id == 1L }.laneIndex)
        assertEquals(1, placed.first { it.block.id == 2L }.laneIndex)
        assertEquals(0, placed.first { it.block.id == 3L }.laneIndex)
    }

    @Test
    fun `timeline block expands on manual tap and on zoom threshold`() {
        val sampleBlock = TimelineBlockEntity(
            id = 42L,
            dayKey = 20261125,
            title = "Nota de diseño",
            detailedNote = "Definir animación horizontal con resorte sin rebote.",
            startMinute = 9 * 60,
            endMinute = 10 * 60,
            countdownEnabled = true,
            createdWithVoice = true
        )
        var manualOverride by mutableStateOf<Boolean?>(null)
        var scaleX by mutableFloatStateOf(1.0f)

        composeRule.setContent {
            CronosSystemTheme {
                val isExpanded = manualOverride ?: (scaleX >= TimelineMath.DETAIL_EXPANSION_SCALE_THRESHOLD)
                TimelineBlockCard(
                    block = sampleBlock,
                    baseWidthDp = 116.dp,
                    isExpanded = isExpanded,
                    isActiveNow = false,
                    onToggleExpand = {
                        val next = !isExpanded
                        manualOverride = next
                    },
                    onEditClick = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("block_detail_note_42", useUnmergedTree = true).assertDoesNotExist()

        // Trigger 1: Tap block manually to expand
        composeRule.onNodeWithTag("timeline_block_42").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("block_detail_note_42", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("edit_block_button_42").assertIsDisplayed()

        // Tap again to collapse manually
        composeRule.onNodeWithTag("timeline_block_42").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("block_detail_note_42", useUnmergedTree = true).assertDoesNotExist()

        // Trigger 2: Zoom >= 1.8f expands automatically when no manual override
        manualOverride = null
        scaleX = 1.85f
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("block_detail_note_42", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `countdown header displays real time progress bar and percentage`() {
        val state = TimelineMath.computeCountdown(
            blocks = emptyList(),
            currentSecondsOfDay = 12 * 3600 // 50% of day
        )
        composeRule.setContent {
            CronosSystemTheme {
                com.cronos.timeline.ui.components.CountdownHeaderBar(
                    countdownState = state,
                    currentSecondsOfDay = 12 * 3600,
                    scaleX = 1.0f,
                    tickLevel = TimelineTickLevel.HOUR_60,
                    onJumpToNow = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("countdown_hms_text").assertIsDisplayed()
        composeRule.onNodeWithTag("countdown_progress_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("countdown_progress_percent").assertIsDisplayed()
    }

    @Test
    fun `room dao filters blocks by color coded category`() = runTest {
        val dao = database.timelineBlockDao()
        val dayKey = 20261125

        dao.insertBlock(
            TimelineBlockEntity(
                dayKey = dayKey,
                title = "Sprint Planning",
                detailedNote = "Equipo backend",
                startMinute = 9 * 60,
                endMinute = 10 * 60,
                categoryIndex = com.cronos.timeline.model.BlockCategory.WORK.index,
                category = com.cronos.timeline.model.BlockCategory.WORK.storageKey,
                categoryColorHex = com.cronos.timeline.model.BlockCategory.WORK.colorHex
            )
        )
        dao.insertBlock(
            TimelineBlockEntity(
                dayKey = dayKey,
                title = "Cita médica",
                detailedNote = "Clínica central",
                startMinute = 12 * 60,
                endMinute = 13 * 60,
                categoryIndex = com.cronos.timeline.model.BlockCategory.PERSONAL.index,
                category = com.cronos.timeline.model.BlockCategory.PERSONAL.storageKey,
                categoryColorHex = com.cronos.timeline.model.BlockCategory.PERSONAL.colorHex
            )
        )
        dao.insertBlock(
            TimelineBlockEntity(
                dayKey = dayKey,
                title = "Incidente producción",
                detailedNote = "Revisar logs",
                startMinute = 15 * 60,
                endMinute = 16 * 60,
                categoryIndex = com.cronos.timeline.model.BlockCategory.URGENT.index,
                category = com.cronos.timeline.model.BlockCategory.URGENT.storageKey,
                categoryColorHex = com.cronos.timeline.model.BlockCategory.URGENT.colorHex
            )
        )

        val urgentBlocks = dao.observeBlocksForDayAndCategory(dayKey, "URGENT").first()
        assertEquals(1, urgentBlocks.size)
        assertEquals("Incidente producción", urgentBlocks.first().title)
        assertEquals(0xFFFB7185L, urgentBlocks.first().categoryColorHex)

        val personalBlocks = dao.observeBlocksForDayAndCategory(dayKey, "PERSONAL").first()
        assertEquals(1, personalBlocks.size)
        assertEquals("Cita médica", personalBlocks.first().title)
    }

    @Test
    fun `category filter bar selects and clears active category`() {
        var selected by mutableStateOf<com.cronos.timeline.model.BlockCategory?>(null)
        composeRule.setContent {
            CronosSystemTheme {
                com.cronos.timeline.ui.components.CategoryFilterBar(
                    selectedCategory = selected,
                    totalCount = 3,
                    categoryCounts = mapOf(
                        com.cronos.timeline.model.BlockCategory.WORK to 1,
                        com.cronos.timeline.model.BlockCategory.PERSONAL to 1,
                        com.cronos.timeline.model.BlockCategory.URGENT to 1
                    ),
                    onSelectCategory = { selected = it }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("filter_chip_all").assertIsDisplayed()
        composeRule.onNodeWithTag("filter_chip_work").performClick()
        composeRule.waitForIdle()
        assertEquals(com.cronos.timeline.model.BlockCategory.WORK, selected)

        composeRule.onNodeWithTag("filter_chip_all").performClick()
        composeRule.waitForIdle()
        assertEquals(null, selected)
    }

    @Test
    fun `block editor sheet reveals staggered sections and handles form actions`() {
        var draft by mutableStateOf(
            com.cronos.timeline.model.EditorDraft(
                title = "",
                detailedNote = "",
                startMinute = 9 * 60,
                endMinute = 10 * 60,
                countdownEnabled = false
            )
        )
        var saved = false
        var dismissed = false

        composeRule.setContent {
            CronosSystemTheme {
                com.cronos.timeline.ui.components.BlockEditorSheet(
                    draft = draft,
                    validationErrorResId = null,
                    onDraftChange = { updater -> draft = updater(draft) },
                    onVoiceTranscript = { _, _ -> },
                    onSave = { saved = true },
                    onDelete = {},
                    onDismiss = { dismissed = true }
                )
            }
        }

        // Advance past staggered entrance delays (6 * 45ms = 270ms)
        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("block_editor_sheet_surface").assertIsDisplayed()
        composeRule.onNodeWithTag("editor_title_input").assertIsDisplayed()
        composeRule.onNodeWithTag("voice_title_button").assertIsDisplayed()
        composeRule.onNodeWithTag("start_time_button").assertIsDisplayed()
        composeRule.onNodeWithTag("end_time_button").assertIsDisplayed()
        composeRule.onNodeWithTag("editor_category_urgent")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("countdown_toggle_switch")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("voice_dictate_note_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("cancel_block_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("save_block_button")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertEquals(com.cronos.timeline.model.BlockCategory.URGENT, draft.category)
        assertTrue(draft.countdownEnabled)
        assertTrue(saved)
        assertTrue(!dismissed)
    }

    @Test
    fun `room dao persists and clears optional customColorHex with category fallback`() = runTest {
        val dao = database.timelineBlockDao()
        val dayKey = 20261125
        val customHex = 0xFFE879F9L

        val id = dao.insertBlock(
            TimelineBlockEntity(
                dayKey = dayKey,
                title = "Sesión Creativa",
                detailedNote = "Prototipo visual",
                startMinute = 11 * 60,
                endMinute = 12 * 60,
                categoryIndex = com.cronos.timeline.model.BlockCategory.WORK.index,
                category = com.cronos.timeline.model.BlockCategory.WORK.storageKey,
                categoryColorHex = com.cronos.timeline.model.BlockCategory.WORK.colorHex,
                customColorHex = customHex
            )
        )

        val storedCustom = dao.getBlockById(id)!!
        assertEquals(customHex, storedCustom.customColorHex)
        assertEquals(
            androidx.compose.ui.graphics.Color(customHex),
            TimelineMath.resolveBlockAccentColor(storedCustom)
        )

        dao.updateBlock(storedCustom.copy(customColorHex = null))
        val storedFallback = dao.getBlockById(id)!!
        assertEquals(null, storedFallback.customColorHex)
        assertEquals(
            com.cronos.timeline.model.BlockCategory.WORK.accentColor,
            TimelineMath.resolveBlockAccentColor(storedFallback)
        )
    }

    @Test
    fun `custom color hex and hsv helpers convert accurately`() {
        assertEquals("#E879F9", TimelineMath.formatHexColor(0xFFE879F9L))
        assertEquals(0xFFE879F9L, TimelineMath.parseHexColor("#e879f9"))
        assertEquals(0xFF34D399L, TimelineMath.parseHexColor("34D399"))
        assertEquals(null, TimelineMath.parseHexColor("#ZZZZZZ"))

        val generatedHex = TimelineMath.hsvToColorHex(180f, 0.8f, 0.9f)
        val hsv = TimelineMath.colorHexToHsv(generatedHex)
        assertEquals(180f, hsv[0], 2f)
    }

    @Test
    fun `block editor sheet selects custom color swatch hex input and resets to category`() {
        var draft by mutableStateOf(
            com.cronos.timeline.model.EditorDraft(
                title = "Diseño UI",
                detailedNote = "",
                startMinute = 10 * 60,
                endMinute = 11 * 60,
                category = com.cronos.timeline.model.BlockCategory.WORK,
                customColorHex = null
            )
        )

        composeRule.setContent {
            CronosSystemTheme {
                com.cronos.timeline.ui.components.BlockEditorSheet(
                    draft = draft,
                    validationErrorResId = null,
                    onDraftChange = { updater -> draft = updater(draft) },
                    onVoiceTranscript = { _, _ -> },
                    onSave = {},
                    onDelete = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.waitForIdle()

        // Pick a custom swatch from the palette (index 3 = 0xFFE879F9L)
        composeRule.onNodeWithTag("custom_color_swatch_3")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(TimelineMath.CUSTOM_COLOR_PALETTE[3], draft.customColorHex)

        // Open fine-tune sliders & type custom hex
        composeRule.onNodeWithTag("toggle_color_sliders_button")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("custom_color_hex_input")
            .performScrollTo()
            .performTextReplacement("#FF6B6B")
        composeRule.waitForIdle()
        assertEquals(0xFFFF6B6BL, draft.customColorHex)

        // Reset back to automatic category color
        composeRule.onNodeWithTag("reset_custom_color_button")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertEquals(null, draft.customColorHex)
    }

    @Test
    fun `toggling timeline block triggers haptic feedback on expand and collapse`() {
        val sampleBlock = TimelineBlockEntity(
            id = 77L,
            dayKey = 20261125,
            title = "Bloque Háptico",
            detailedNote = "Confirmación táctil al expandir.",
            startMinute = 9 * 60,
            endMinute = 10 * 60
        )
        val recordedHaptics = mutableListOf<HapticFeedbackType>()
        val recordingHaptic = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                recordedHaptics.add(hapticFeedbackType)
            }
        }
        var isExpanded by mutableStateOf(false)

        composeRule.setContent {
            CronosSystemTheme {
                CompositionLocalProvider(LocalHapticFeedback provides recordingHaptic) {
                    TimelineBlockCard(
                        block = sampleBlock,
                        baseWidthDp = 116.dp,
                        isExpanded = isExpanded,
                        isActiveNow = false,
                        onToggleExpand = { isExpanded = !isExpanded },
                        onEditClick = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("timeline_block_77").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("timeline_block_77").performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf(HapticFeedbackType.LongPress, HapticFeedbackType.TextHandleMove),
            recordedHaptics
        )
    }

    @Test
    fun `pinch zoom gesture triggers haptic feedback across thresholds`() {
        val recordedHaptics = mutableListOf<HapticFeedbackType>()
        val recordingHaptic = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                recordedHaptics.add(hapticFeedbackType)
            }
        }
        var scaleX by mutableFloatStateOf(1.0f)
        var scrollOffset by mutableFloatStateOf(0f)

        composeRule.setContent {
            CronosSystemTheme {
                CompositionLocalProvider(LocalHapticFeedback provides recordingHaptic) {
                    com.cronos.timeline.ui.components.HorizontalTimelineCanvas(
                        blocks = emptyList(),
                        currentSecondsOfDay = 10 * 3600,
                        scaleX = scaleX,
                        scrollOffsetPx = scrollOffset,
                        manualExpansionOverrides = emptyMap(),
                        selectedCategoryFilter = null,
                        onClearCategoryFilter = {},
                        onTransformGesture = { newScale, newScroll ->
                            scaleX = newScale
                            scrollOffset = newScroll
                        },
                        onToggleBlockExpand = {},
                        onEditBlockClick = {},
                        onEmptySlotLongPress = {},
                        onViewportWidthChanged = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("horizontal_timeline_canvas").performTouchInput {
            pinch(
                start0 = Offset(centerX - 30f, centerY),
                end0 = Offset(centerX - 130f, centerY),
                start1 = Offset(centerX + 30f, centerY),
                end1 = Offset(centerX + 130f, centerY)
            )
        }
        composeRule.waitForIdle()

        assertTrue(scaleX >= TimelineMath.DETAIL_EXPANSION_SCALE_THRESHOLD)
        assertTrue(recordedHaptics.contains(HapticFeedbackType.LongPress))
        assertTrue(recordedHaptics.contains(HapticFeedbackType.TextHandleMove))
    }
}
