package com.cronos.timeline.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timeline_blocks")
data class TimelineBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dayKey: Int,
    val title: String,
    val detailedNote: String,
    val startMinute: Int,
    val endMinute: Int,
    val countdownEnabled: Boolean = true,
    val categoryIndex: Int = 0,
    @ColumnInfo(defaultValue = "WORK")
    val category: String = "WORK",
    @ColumnInfo(defaultValue = "4281909240")
    val categoryColorHex: Long = 0xFF38BDF8L,
    @ColumnInfo(defaultValue = "NULL")
    val customColorHex: Long? = null,
    val createdWithVoice: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
