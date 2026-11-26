package com.cronos.timeline.data.repository

import com.cronos.timeline.data.local.TimelineBlockDao
import com.cronos.timeline.data.local.TimelineBlockEntity
import com.cronos.timeline.model.BlockCategory
import kotlinx.coroutines.flow.Flow

class TimelineRepository(
    private val dao: TimelineBlockDao
) {
    fun observeBlocksForDay(
        dayKey: Int,
        categoryFilter: BlockCategory? = null
    ): Flow<List<TimelineBlockEntity>> {
        return if (categoryFilter == null) {
            dao.observeBlocksForDay(dayKey)
        } else {
            dao.observeBlocksForDayAndCategory(dayKey, categoryFilter.storageKey)
        }
    }

    suspend fun getBlockById(id: Long): TimelineBlockEntity? =
        dao.getBlockById(id)

    suspend fun saveBlock(block: TimelineBlockEntity): Long {
        return if (block.id == 0L) {
            dao.insertBlock(block)
        } else {
            dao.updateBlock(block)
            block.id
        }
    }

    suspend fun deleteBlockById(id: Long) {
        dao.deleteBlockById(id)
    }
}
