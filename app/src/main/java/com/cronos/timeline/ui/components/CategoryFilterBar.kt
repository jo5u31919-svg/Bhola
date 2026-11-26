package com.cronos.timeline.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cronos.timeline.model.BlockCategory
import com.cronos.timeline.ui.theme.CyanChronos
import com.cronos.timeline.ui.theme.HarmonyGlassBase
import com.cronos.timeline.ui.theme.HarmonyGlassCard
import com.cronos.timeline.ui.theme.TextPrimary
import com.cronos.timeline.ui.theme.TextSecondary
import com.example.R

@Composable
fun CategoryFilterBar(
    selectedCategory: BlockCategory?,
    totalCount: Int,
    categoryCounts: Map<BlockCategory, Int>,
    onSelectCategory: (BlockCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .testTag("category_filter_bar")
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = HarmonyGlassBase.copy(alpha = 0.68f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            tonalElevation = 0.dp
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "all") {
                    val isAllSelected = selectedCategory == null
                    val allLabel = stringResource(R.string.filter_all)
                    val chipText = if (totalCount > 0) "$allLabel · $totalCount" else allLabel
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { onSelectCategory(null) },
                        label = {
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = if (isAllSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(999.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = HarmonyGlassCard.copy(alpha = 0.65f),
                            labelColor = TextSecondary,
                            selectedContainerColor = CyanChronos.copy(alpha = 0.22f),
                            selectedLabelColor = CyanChronos,
                            selectedLeadingIconColor = CyanChronos
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isAllSelected) {
                                CyanChronos.copy(alpha = 0.68f)
                            } else {
                                Color.White.copy(alpha = 0.14f)
                            }
                        ),
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                }

                items(
                    items = BlockCategory.entries,
                    key = { it.storageKey }
                ) { category ->
                    val isSelected = selectedCategory == category
                    val count = categoryCounts[category] ?: 0
                    val categoryLabel = stringResource(category.labelResId)
                    val chipText = if (count > 0) "$categoryLabel · $count" else categoryLabel
                    val filterDesc = stringResource(R.string.cd_filter_category, categoryLabel)

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onSelectCategory(if (isSelected) null else category)
                        },
                        label = {
                            Text(
                                text = chipText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = category.accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(category.accentColor)
                                )
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = HarmonyGlassCard.copy(alpha = 0.65f),
                            labelColor = if (count > 0) TextPrimary else TextSecondary,
                            selectedContainerColor = category.accentColor.copy(alpha = 0.22f),
                            selectedLabelColor = category.accentColor
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) {
                                category.accentColor.copy(alpha = 0.72f)
                            } else {
                                Color.White.copy(alpha = 0.14f)
                            }
                        ),
                        modifier = Modifier
                            .semantics { contentDescription = filterDesc }
                            .testTag("filter_chip_${category.storageKey.lowercase()}")
                    )
                }
            }
        }
    }
}
