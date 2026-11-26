package com.cronos.timeline.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cronos.timeline.data.local.TimelineBlockEntity
import com.cronos.timeline.data.repository.TimelineRepository
import com.cronos.timeline.model.BlockCategory
import com.cronos.timeline.model.CountdownState
import com.cronos.timeline.model.EditorDraft
import com.cronos.timeline.model.TimelineMath
import com.cronos.timeline.voice.VoiceTargetField
import com.example.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimelineUiState(
    val dayKey: Int = TimelineMath.currentDayKey(),
    val blocks: List<TimelineBlockEntity> = emptyList(),
    val totalBlocksCount: Int = 0,
    val categoryCounts: Map<BlockCategory, Int> = emptyMap(),
    val selectedCategoryFilter: BlockCategory? = null,
    val currentSecondsOfDay: Int = TimelineMath.currentSecondsOfDay(),
    val countdownState: CountdownState = TimelineMath.computeCountdown(
        blocks = emptyList(),
        currentSecondsOfDay = currentSecondsOfDay
    ),
    val editorDraft: EditorDraft? = null,
    val validationErrorResId: Int? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val repository: TimelineRepository
) : ViewModel() {

    private val dayKeyFlow = MutableStateFlow(TimelineMath.currentDayKey())
    private val selectedCategoryFilterFlow = MutableStateFlow<BlockCategory?>(null)
    private val secondsOfDayFlow = MutableStateFlow(TimelineMath.currentSecondsOfDay())
    private val editorDraftFlow = MutableStateFlow<EditorDraft?>(null)
    private val validationErrorFlow = MutableStateFlow<Int?>(null)

    private val allBlocksForTodayFlow = dayKeyFlow.flatMapLatest { key ->
        repository.observeBlocksForDay(key, categoryFilter = null)
    }

    private val filteredBlocksForTodayFlow = combine(
        dayKeyFlow,
        selectedCategoryFilterFlow
    ) { key, categoryFilter ->
        key to categoryFilter
    }.flatMapLatest { (key, categoryFilter) ->
        repository.observeBlocksForDay(key, categoryFilter = categoryFilter)
    }

    private data class TimelineMetaState(
        val dayKey: Int,
        val nowSeconds: Int,
        val selectedFilter: BlockCategory?,
        val draft: EditorDraft?,
        val errorResId: Int?
    )

    private val metaFlow = combine(
        dayKeyFlow,
        secondsOfDayFlow,
        selectedCategoryFilterFlow,
        editorDraftFlow,
        validationErrorFlow
    ) { dayKey, nowSeconds, selectedFilter, draft, errorResId ->
        TimelineMetaState(
            dayKey = dayKey,
            nowSeconds = nowSeconds,
            selectedFilter = selectedFilter,
            draft = draft,
            errorResId = errorResId
        )
    }

    val uiState: StateFlow<TimelineUiState> = combine(
        allBlocksForTodayFlow,
        filteredBlocksForTodayFlow,
        metaFlow
    ) { allBlocks, filteredBlocks, meta ->
        val counts = allBlocks.groupingBy { BlockCategory.fromEntity(it) }.eachCount()
        TimelineUiState(
            dayKey = meta.dayKey,
            blocks = filteredBlocks,
            totalBlocksCount = allBlocks.size,
            categoryCounts = counts,
            selectedCategoryFilter = meta.selectedFilter,
            currentSecondsOfDay = meta.nowSeconds,
            countdownState = TimelineMath.computeCountdown(allBlocks, meta.nowSeconds),
            editorDraft = meta.draft,
            validationErrorResId = meta.errorResId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TimelineUiState()
    )

    init {
        viewModelScope.launch {
            while (isActive) {
                val nextDayKey = TimelineMath.currentDayKey()
                if (nextDayKey != dayKeyFlow.value) {
                    dayKeyFlow.value = nextDayKey
                }
                secondsOfDayFlow.value = TimelineMath.currentSecondsOfDay()
                delay(1_000L)
            }
        }
    }

    fun selectCategoryFilter(category: BlockCategory?) {
        selectedCategoryFilterFlow.value = category
    }

    fun openCreateSheet(suggestedStartMinute: Int? = null) {
        val nowMin = secondsOfDayFlow.value / 60
        val defaultStart = (suggestedStartMinute ?: ((nowMin / 5) * 5))
            .coerceIn(0, TimelineMath.MINUTES_IN_DAY - 15)
        val defaultEnd = (defaultStart + 30).coerceAtMost(TimelineMath.MINUTES_IN_DAY)
        val defaultCategory = selectedCategoryFilterFlow.value ?: BlockCategory.WORK
        validationErrorFlow.value = null
        editorDraftFlow.value = EditorDraft(
            editingId = null,
            title = "",
            detailedNote = "",
            startMinute = defaultStart,
            endMinute = defaultEnd,
            countdownEnabled = true,
            category = defaultCategory,
            customColorHex = null,
            createdWithVoice = false
        )
    }

    fun openEditSheet(block: TimelineBlockEntity) {
        validationErrorFlow.value = null
        editorDraftFlow.value = EditorDraft(
            editingId = block.id,
            title = block.title,
            detailedNote = block.detailedNote,
            startMinute = block.startMinute,
            endMinute = block.endMinute,
            countdownEnabled = block.countdownEnabled,
            category = BlockCategory.fromEntity(block),
            customColorHex = block.customColorHex,
            createdWithVoice = block.createdWithVoice
        )
    }

    fun dismissEditorSheet() {
        validationErrorFlow.value = null
        editorDraftFlow.value = null
    }

    fun updateDraft(transform: (EditorDraft) -> EditorDraft) {
        validationErrorFlow.value = null
        editorDraftFlow.update { current -> current?.let(transform) }
    }

    fun appendVoiceTranscript(field: VoiceTargetField, transcript: String) {
        if (transcript.isBlank()) return
        validationErrorFlow.value = null
        editorDraftFlow.update { current ->
            current ?: return@update null
            when (field) {
                VoiceTargetField.TITLE -> {
                    val updatedTitle = if (current.title.isBlank()) {
                        transcript
                    } else {
                        "${current.title.trimEnd()} $transcript"
                    }
                    current.copy(title = updatedTitle, createdWithVoice = true)
                }

                VoiceTargetField.NOTE -> {
                    val updatedNote = if (current.detailedNote.isBlank()) {
                        transcript
                    } else {
                        "${current.detailedNote.trimEnd()}\n$transcript"
                    }
                    val autoTitle = if (current.title.isBlank()) {
                        transcript.take(48).trim()
                    } else {
                        current.title
                    }
                    current.copy(
                        title = autoTitle,
                        detailedNote = updatedNote,
                        createdWithVoice = true
                    )
                }
            }
        }
    }

    fun saveDraft(onSuccess: (isUpdate: Boolean) -> Unit) {
        val draft = editorDraftFlow.value ?: return
        val trimmedTitle = draft.title.trim()
        if (trimmedTitle.isEmpty()) {
            validationErrorFlow.value = R.string.error_title_required
            return
        }
        if (draft.endMinute <= draft.startMinute) {
            validationErrorFlow.value = R.string.error_invalid_time_range
            return
        }

        val isUpdate = draft.editingId != null
        val entity = TimelineBlockEntity(
            id = draft.editingId ?: 0L,
            dayKey = dayKeyFlow.value,
            title = trimmedTitle,
            detailedNote = draft.detailedNote.trim(),
            startMinute = draft.startMinute.coerceIn(0, TimelineMath.MINUTES_IN_DAY - 1),
            endMinute = draft.endMinute.coerceIn(1, TimelineMath.MINUTES_IN_DAY),
            countdownEnabled = draft.countdownEnabled,
            categoryIndex = draft.category.index,
            category = draft.category.storageKey,
            categoryColorHex = draft.category.colorHex,
            customColorHex = draft.customColorHex,
            createdWithVoice = draft.createdWithVoice
        )

        viewModelScope.launch {
            repository.saveBlock(entity)
            editorDraftFlow.value = null
            validationErrorFlow.value = null
            onSuccess(isUpdate)
        }
    }

    fun deleteEditingBlock(onDeleted: () -> Unit) {
        val editingId = editorDraftFlow.value?.editingId ?: return
        viewModelScope.launch {
            repository.deleteBlockById(editingId)
            editorDraftFlow.value = null
            validationErrorFlow.value = null
            onDeleted()
        }
    }

    class Factory(
        private val repository: TimelineRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TimelineViewModel(repository) as T
        }
    }
}
