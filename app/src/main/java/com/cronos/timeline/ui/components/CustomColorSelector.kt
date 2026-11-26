package com.cronos.timeline.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cronos.timeline.model.BlockCategory
import com.cronos.timeline.model.TimelineMath
import com.cronos.timeline.ui.theme.HarmonyGlassBase
import com.cronos.timeline.ui.theme.ObsidianVoid
import com.cronos.timeline.ui.theme.TextMuted
import com.cronos.timeline.ui.theme.TextPrimary
import com.cronos.timeline.ui.theme.TextSecondary
import com.example.R

@Composable
fun CustomColorSelectorSection(
    selectedCategory: BlockCategory,
    customColorHex: Long?,
    onSelectCustomColor: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePreviewColor = customColorHex?.let { Color(it) } ?: selectedCategory.accentColor
    val effectiveColorHex = customColorHex ?: selectedCategory.colorHex
    var showSliders by remember { mutableStateOf(false) }

    val initialHsv = remember(effectiveColorHex) { TimelineMath.colorHexToHsv(effectiveColorHex) }
    var hue by remember(effectiveColorHex) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(effectiveColorHex) { mutableFloatStateOf(initialHsv[1].coerceAtLeast(0.15f)) }
    var brightness by remember(effectiveColorHex) { mutableFloatStateOf(initialHsv[2].coerceAtLeast(0.55f)) }
    var hexInput by remember(effectiveColorHex) {
        mutableStateOf(TimelineMath.formatHexColor(effectiveColorHex))
    }

    LaunchedEffect(customColorHex, selectedCategory) {
        val targetHex = customColorHex ?: selectedCategory.colorHex
        val hsv = TimelineMath.colorHexToHsv(targetHex)
        hue = hsv[0]
        saturation = hsv[1].coerceAtLeast(0.12f)
        brightness = hsv[2].coerceAtLeast(0.45f)
        hexInput = TimelineMath.formatHexColor(targetHex)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Row: Label + Live Color Preview + Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                val previewDesc = stringResource(R.string.cd_custom_color_preview)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(activePreviewColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                        .semantics { contentDescription = previewDesc }
                        .testTag("custom_color_preview")
                )
                Column {
                    Text(
                        text = stringResource(R.string.field_custom_color_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = if (customColorHex != null) {
                            TimelineMath.formatHexColor(customColorHex)
                        } else {
                            stringResource(R.string.custom_color_auto)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (customColorHex != null) activePreviewColor else TextMuted,
                        fontWeight = if (customColorHex != null) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.testTag("custom_color_status_text")
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (customColorHex != null) {
                    Surface(
                        onClick = { onSelectCustomColor(null) },
                        shape = RoundedCornerShape(999.dp),
                        color = HarmonyGlassBase.copy(alpha = 0.56f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier.testTag("reset_custom_color_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_use_category_color),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Surface(
                    onClick = { showSliders = !showSliders },
                    shape = RoundedCornerShape(999.dp),
                    color = if (showSliders) {
                        activePreviewColor.copy(alpha = 0.20f)
                    } else {
                        HarmonyGlassBase.copy(alpha = 0.56f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (showSliders) {
                            activePreviewColor.copy(alpha = 0.65f)
                        } else {
                            Color.White.copy(alpha = 0.16f)
                        }
                    ),
                    modifier = Modifier.testTag("toggle_color_sliders_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = if (showSliders) activePreviewColor else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = stringResource(
                                if (showSliders) R.string.action_hide_color_sliders
                                else R.string.action_customize_color
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (showSliders) activePreviewColor else TextSecondary
                        )
                    }
                }
            }
        }

        // Palette Swatches Row (Auto + Vivid & Soft Presets)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isAuto = customColorHex == null
            val autoCd = stringResource(R.string.cd_custom_color_auto)
            Surface(
                onClick = { onSelectCustomColor(null) },
                shape = CircleShape,
                color = selectedCategory.accentColor.copy(alpha = if (isAuto) 0.28f else 0.14f),
                border = BorderStroke(
                    width = if (isAuto) 2.dp else 1.dp,
                    color = if (isAuto) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f)
                ),
                modifier = Modifier
                    .size(34.dp)
                    .semantics { contentDescription = autoCd }
                    .testTag("custom_color_swatch_auto")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = selectedCategory.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            TimelineMath.CUSTOM_COLOR_PALETTE.forEachIndexed { index, hex ->
                val swatchColor = Color(hex)
                val isSelected = customColorHex == hex
                val hexText = TimelineMath.formatHexColor(hex)
                val swatchCd = stringResource(R.string.cd_custom_color_swatch, hexText)

                Surface(
                    onClick = { onSelectCustomColor(hex) },
                    shape = CircleShape,
                    color = swatchColor,
                    border = BorderStroke(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.24f)
                    ),
                    modifier = Modifier
                        .size(34.dp)
                        .semantics { contentDescription = swatchCd }
                        .testTag("custom_color_swatch_$index")
                ) {
                    if (isSelected) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = ObsidianVoid,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Expandable Fine-Tune Sliders (Hue, Saturation, Brightness + Hex input)
        AnimatedVisibility(
            visible = showSliders,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = HarmonyGlassBase.copy(alpha = 0.56f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Live Color Preview Strip + Hex Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(activePreviewColor)
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.35f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = TimelineMath.formatHexColor(effectiveColorHex),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianVoid,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { raw ->
                                val filtered = raw.take(7)
                                hexInput = filtered
                                TimelineMath.parseHexColor(filtered)?.let { parsedHex ->
                                    onSelectCustomColor(parsedHex)
                                }
                            },
                            label = { Text(stringResource(R.string.custom_color_hex_label)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activePreviewColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .width(126.dp)
                                .testTag("custom_color_hex_input")
                        )
                    }

                    // Hue Spectrum Slider
                    SpectrumSliderRow(
                        label = stringResource(R.string.custom_color_hue),
                        value = hue,
                        valueRange = 0f..360f,
                        gradientColors = listOf(
                            Color(0xFFFF4D4DL),
                            Color(0xFFFFD64DL),
                            Color(0xFF4DFF6EL),
                            Color(0xFF4DFFFFL),
                            Color(0xFF4D6EFFL),
                            Color(0xFFFF4DF0L),
                            Color(0xFFFF4D4DL)
                        ),
                        thumbColor = activePreviewColor,
                        onValueChange = { newHue ->
                            hue = newHue
                            val nextHex = TimelineMath.hsvToColorHex(newHue, saturation, brightness)
                            hexInput = TimelineMath.formatHexColor(nextHex)
                            onSelectCustomColor(nextHex)
                        },
                        sliderTag = "custom_color_hue_slider"
                    )

                    // Saturation Slider
                    val grayAtBrightness = Color(TimelineMath.hsvToColorHex(hue, 0.10f, brightness))
                    val vividAtHue = Color(TimelineMath.hsvToColorHex(hue, 1f, brightness))
                    SpectrumSliderRow(
                        label = stringResource(R.string.custom_color_saturation),
                        value = saturation,
                        valueRange = 0.10f..1f,
                        gradientColors = listOf(grayAtBrightness, vividAtHue),
                        thumbColor = activePreviewColor,
                        onValueChange = { newSat ->
                            saturation = newSat
                            val nextHex = TimelineMath.hsvToColorHex(hue, newSat, brightness)
                            hexInput = TimelineMath.formatHexColor(nextHex)
                            onSelectCustomColor(nextHex)
                        },
                        sliderTag = "custom_color_sat_slider"
                    )

                    // Brightness Slider
                    val dimColor = Color(TimelineMath.hsvToColorHex(hue, saturation, 0.45f))
                    val brightColor = Color(TimelineMath.hsvToColorHex(hue, saturation, 1f))
                    SpectrumSliderRow(
                        label = stringResource(R.string.custom_color_brightness),
                        value = brightness,
                        valueRange = 0.45f..1f,
                        gradientColors = listOf(dimColor, brightColor),
                        thumbColor = activePreviewColor,
                        onValueChange = { newVal ->
                            brightness = newVal
                            val nextHex = TimelineMath.hsvToColorHex(hue, saturation, newVal)
                            hexInput = TimelineMath.formatHexColor(nextHex)
                            onSelectCustomColor(nextHex)
                        },
                        sliderTag = "custom_color_val_slider"
                    )
                }
            }
        }
    }
}

@Composable
private fun SpectrumSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    gradientColors: List<Color>,
    thumbColor: Color,
    onValueChange: (Float) -> Unit,
    sliderTag: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Brush.horizontalGradient(gradientColors))
                    .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
            )
            Slider(
                value = value.coerceIn(valueRange.start, valueRange.endInclusive),
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = thumbColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag(sliderTag)
            )
        }
    }
}
