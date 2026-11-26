package com.cronos.timeline.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineBlockDao {

    @Query("SELECT * FROM timeline_blocks WHERE dayKey = :dayKey ORDER BY startMinute ASC, endMinute ASC")
    fun observeBlocksForDay(dayKey: Int): Flow<List<TimelineBlockEntity>>

    @Query(
        "SELECT * FROM timeline_blocks WHERE dayKey = :dayKey AND category = :category ORDER BY startMinute ASC, endMinute ASC"
    )
    fun observeBlocksForDayAndCategory(
        dayKey: Int,
        category: String
    ): Flow<List<TimelineBlockEntity>>

    @Query("SELECT * FROM timeline_blocks WHERE id = :id LIMIT 1")
    suspend fun getBlockById(id: Long): TimelineBlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: TimelineBlockEntity): Long

    @Update
    suspend fun updateBlock(block: TimelineBlockEntity)

    @Query("DELETE FROM timeline_blocks WHERE id = :id")
    suspend fun deleteBlockById(id: Long)
}
