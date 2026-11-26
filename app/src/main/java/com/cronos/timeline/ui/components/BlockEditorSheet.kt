package com.cronos.timeline.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cronos.timeline.model.BlockCategory
import com.cronos.timeline.model.EditorDraft
import com.cronos.timeline.model.TimelineMath
import com.cronos.timeline.ui.theme.AmberChronos
import com.cronos.timeline.ui.theme.CoralChronos
import com.cronos.timeline.ui.theme.CyanChronos
import com.cronos.timeline.ui.theme.HarmonyAmbientGlow
import com.cronos.timeline.ui.theme.HarmonyGlassBase
import com.cronos.timeline.ui.theme.HarmonyGlassCard
import com.cronos.timeline.ui.theme.ObsidianSurfaceElevated
import com.cronos.timeline.ui.theme.ObsidianVoid
import com.cronos.timeline.ui.theme.TextMuted
import com.cronos.timeline.ui.theme.TextPrimary
import com.cronos.timeline.ui.theme.TextSecondary
import com.cronos.timeline.voice.VoiceDictationManager
import com.cronos.timeline.voice.VoiceTargetField
import com.example.R
import kotlinx.coroutines.delay

private enum class TimePickerTarget {
    START,
    END
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorSheet(
    draft: EditorDraft,
    validationErrorResId: Int?,
    onDraftChange: ((EditorDraft) -> EditorDraft) -> Unit,
    onVoiceTranscript: (VoiceTargetField, String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var timePickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }
    var voiceFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var pendingVoiceField by remember { mutableStateOf<VoiceTargetField?>(null) }

    val voiceManager = remember {
        VoiceDictationManager(
            context = context,
            onFinalTranscript = { field, transcript ->
                onVoiceTranscript(field, transcript)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
        }
    }

    val voiceState by voiceManager.state.collectAsStateWithLifecycle()

    val permissionDeniedText = stringResource(R.string.voice_permission_denied)
    val voiceUnavailableText = stringResource(R.string.voice_unavailable)

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val field = pendingVoiceField
        pendingVoiceField = null
        if (granted && field != null) {
            voiceFeedbackMessage = null
            voiceManager.startListening(field)
        } else if (!granted) {
            voiceFeedbackMessage = permissionDeniedText
        }
    }

    fun triggerVoiceDictation(field: VoiceTargetField) {
        if (voiceState.isListening && voiceState.activeField == field) {
            voiceManager.stopListening()
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            voiceFeedbackMessage = voiceUnavailableText
            return
        }
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            voiceFeedbackMessage = null
            voiceManager.startListening(field)
        } else {
            pendingVoiceField = field
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val isEditing = draft.editingId != null
    val selectedCategory = draft.category
    val accentColor by animateColorAsState(
        targetValue = draft.effectiveAccentColor,
        animationSpec = tween(durationMillis = 260),
        label = "sheet_accent"
    )

    val listeningTitle = voiceState.isListening && voiceState.activeField == VoiceTargetField.TITLE
    val listeningNote = voiceState.isListening && voiceState.activeField == VoiceTargetField.NOTE

    // Shared infinite pulse for active voice recording indicators
    val pulseTransition = rememberInfiniteTransition(label = "voice_pulse_transition")
    val pulseProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_pulse_progress"
    )

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    ModalBottomSheet(
        onDismissRequest = {
            voiceManager.stopListening()
            onDismiss()
        },
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = Color.Transparent,
        contentColor = TextPrimary,
        scrimColor = ObsidianVoid.copy(alpha = 0.58f),
        dragHandle = null
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("block_editor_sheet_surface"),
            shape = sheetShape,
            color = HarmonyGlassBase.copy(alpha = 0.84f),
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.28f),
                        accentColor.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            ),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                HarmonyAmbientGlow.copy(alpha = 0.34f),
                                HarmonyGlassCard.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .navigationBarsPadding()
                    .padding(top = 10.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sleek HarmonyOS Drag Handle
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.30f))
                    )
                }

                // Section 0: Header (Title + Category Indicator + Delete)
                StaggeredSheetSection(index = 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Text(
                                text = stringResource(
                                    if (isEditing) R.string.sheet_title_edit else R.string.sheet_title_new
                                ),
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (isEditing) {
                            Surface(
                                shape = CircleShape,
                                color = CoralChronos.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, CoralChronos.copy(alpha = 0.36f))
                            ) {
                                IconButton(
                                    onClick = {
                                        voiceManager.stopListening()
                                        onDelete()
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("delete_block_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = CoralChronos,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 1: Title Input + Pulsing Voice Dictation Icon
                StaggeredSheetSection(index = 1) {
                    HarmonyModularCard(
                        borderColor = if (listeningTitle) {
                            CoralChronos.copy(alpha = 0.48f + 0.25f * pulseProgress)
                        } else {
                            Color.White.copy(alpha = 0.14f)
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        OutlinedTextField(
                            value = draft.title,
                            onValueChange = { newTitle -> onDraftChange { it.copy(title = newTitle) } },
                            label = { Text(stringResource(R.string.field_title_label)) },
                            placeholder = { Text(stringResource(R.string.field_title_placeholder)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = {
                                val titleMicScale = if (listeningTitle) 1f + 0.14f * pulseProgress else 1f
                                val titleMicHaloAlpha = if (listeningTitle) 0.16f + 0.22f * pulseProgress else 0.14f
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    if (listeningTitle) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .scale(1f + 0.18f * pulseProgress)
                                                .clip(CircleShape)
                                                .background(CoralChronos.copy(alpha = 0.18f * (1f - 0.35f * pulseProgress)))
                                        )
                                    }
                                    IconButton(
                                        onClick = { triggerVoiceDictation(VoiceTargetField.TITLE) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .scale(titleMicScale)
                                            .clip(CircleShape)
                                            .background(
                                                if (listeningTitle) {
                                                    CoralChronos.copy(alpha = titleMicHaloAlpha)
                                                } else {
                                                    CyanChronos.copy(alpha = 0.14f)
                                                }
                                            )
                                            .testTag("voice_title_button")
                                    ) {
                                        Icon(
                                            imageVector = if (listeningTitle) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                            contentDescription = stringResource(R.string.voice_dictate_title),
                                            tint = if (listeningTitle) CoralChronos else CyanChronos,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor.copy(alpha = 0.85f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                                focusedContainerColor = HarmonyGlassBase.copy(alpha = 0.45f),
                                unfocusedContainerColor = HarmonyGlassBase.copy(alpha = 0.35f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = TextSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_title_input")
                        )
                    }
                }

                // Section 2: Modular Card for Start/End Time Pickers & Quick Duration Chips
                StaggeredSheetSection(index = 2) {
                    HarmonyModularCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TimeBoxSelector(
                                label = stringResource(R.string.field_start_time),
                                timeText = TimelineMath.formatMinuteOfDay(draft.startMinute),
                                accentColor = accentColor,
                                onClick = { timePickerTarget = TimePickerTarget.START },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("start_time_button")
                            )
                            TimeBoxSelector(
                                label = stringResource(R.string.field_end_time),
                                timeText = TimelineMath.formatMinuteOfDay(draft.endMinute),
                                accentColor = accentColor,
                                onClick = { timePickerTarget = TimePickerTarget.END },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("end_time_button")
                            )
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.09f),
                            thickness = 1.dp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.field_duration_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    15 to R.string.chip_15m,
                                    30 to R.string.chip_30m,
                                    60 to R.string.chip_1h,
                                    120 to R.string.chip_2h
                                ).forEach { (minutesToAdd, labelRes) ->
                                    val isSelected = (draft.endMinute - draft.startMinute) == minutesToAdd
                                    Surface(
                                        onClick = {
                                            onDraftChange { current ->
                                                current.copy(
                                                    endMinute = (current.startMinute + minutesToAdd)
                                                        .coerceAtMost(TimelineMath.MINUTES_IN_DAY)
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isSelected) {
                                            accentColor.copy(alpha = 0.22f)
                                        } else {
                                            HarmonyGlassBase.copy(alpha = 0.52f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) {
                                                accentColor.copy(alpha = 0.72f)
                                            } else {
                                                Color.White.copy(alpha = 0.14f)
                                            }
                                        )
                                    ) {
                                        Text(
                                            text = stringResource(labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) accentColor else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Modular Card for Color-Coded Categories
                StaggeredSheetSection(index = 3) {
                    HarmonyModularCard {
                        Text(
                            text = stringResource(R.string.field_category_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BlockCategory.entries.forEach { category ->
                                val selected = draft.category == category
                                Surface(
                                    onClick = {
                                        onDraftChange { it.copy(category = category) }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (selected) {
                                        category.accentColor.copy(alpha = 0.22f)
                                    } else {
                                        HarmonyGlassBase.copy(alpha = 0.52f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) {
                                            category.accentColor.copy(alpha = 0.80f)
                                        } else {
                                            Color.White.copy(alpha = 0.14f)
                                        }
                                    ),
                                    modifier = Modifier
                                        .testTag("editor_category_${category.storageKey.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(category.accentColor)
                                        )
                                        Text(
                                            text = stringResource(category.labelResId),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) category.accentColor else TextSecondary,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.09f),
                            thickness = 1.dp
                        )

                        CustomColorSelectorSection(
                            selectedCategory = selectedCategory,
                            customColorHex = draft.customColorHex,
                            onSelectCustomColor = { newColorHex ->
                                onDraftChange { it.copy(customColorHex = newColorHex) }
                            }
                        )
                    }
                }

                // Section 4: Modular Card for Priority Countdown Toggle
                StaggeredSheetSection(index = 4) {
                    HarmonyModularCard(
                        borderColor = if (draft.countdownEnabled) {
                            AmberChronos.copy(alpha = 0.36f)
                        } else {
                            Color.White.copy(alpha = 0.14f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AmberChronos.copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, AmberChronos.copy(alpha = 0.34f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Timer,
                                        contentDescription = null,
                                        tint = AmberChronos,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.field_countdown_toggle),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.field_countdown_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = draft.countdownEnabled,
                                onCheckedChange = { enabled ->
                                    onDraftChange { it.copy(countdownEnabled = enabled) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ObsidianVoid,
                                    checkedTrackColor = AmberChronos,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = HarmonyGlassBase.copy(alpha = 0.7f),
                                    uncheckedBorderColor = Color.White.copy(alpha = 0.20f)
                                ),
                                modifier = Modifier.testTag("countdown_toggle_switch")
                            )
                        }
                    }
                }

                // Section 5: Modular Card for Detailed Note + Pulsing Voice Dictation Button
                StaggeredSheetSection(index = 5) {
                    HarmonyModularCard(
                        borderColor = if (listeningNote) {
                            CoralChronos.copy(alpha = 0.48f + 0.25f * pulseProgress)
                        } else {
                            Color.White.copy(alpha = 0.14f)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.field_note_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )

                            val buttonScale = if (listeningNote) 1f + 0.05f * pulseProgress else 1f
                            val micIconScale = if (listeningNote) 1f + 0.20f * pulseProgress else 1f
                            val dictateContainerColor = if (listeningNote) {
                                CoralChronos.copy(alpha = 0.18f + 0.14f * pulseProgress)
                            } else {
                                AmberChronos.copy(alpha = 0.14f)
                            }
                            val dictateBorderColor = if (listeningNote) {
                                CoralChronos.copy(alpha = 0.65f + 0.30f * pulseProgress)
                            } else {
                                AmberChronos.copy(alpha = 0.50f)
                            }

                            OutlinedButton(
                                onClick = { triggerVoiceDictation(VoiceTargetField.NOTE) },
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, dictateBorderColor),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = dictateContainerColor,
                                    contentColor = if (listeningNote) CoralChronos else AmberChronos
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .scale(buttonScale)
                                    .testTag("voice_dictate_note_button")
                            ) {
                                Icon(
                                    imageVector = if (listeningNote) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                                    contentDescription = stringResource(R.string.cd_voice_dictate),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .scale(micIconScale)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        if (listeningNote) R.string.voice_stop else R.string.voice_dictate_note
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = voiceState.isListening,
                            enter = fadeIn(tween(180)),
                            exit = fadeOut(tween(140))
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AmberChronos.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, AmberChronos.copy(alpha = 0.42f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (voiceState.partialTranscript.isNotBlank()) {
                                        voiceState.partialTranscript
                                    } else {
                                        stringResource(R.string.voice_listening)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AmberChronos,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        val errorText = voiceFeedbackMessage ?: voiceState.errorCode?.let { code ->
                            stringResource(R.string.voice_error_generic, code.toString())
                        }
                        if (!errorText.isNullOrBlank()) {
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoralChronos
                            )
                        }

                        OutlinedTextField(
                            value = draft.detailedNote,
                            onValueChange = { note -> onDraftChange { it.copy(detailedNote = note) } },
                            placeholder = { Text(stringResource(R.string.field_note_placeholder)) },
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor.copy(alpha = 0.85f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.14f),
                                focusedContainerColor = HarmonyGlassBase.copy(alpha = 0.45f),
                                unfocusedContainerColor = HarmonyGlassBase.copy(alpha = 0.35f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_note_input")
                        )
                    }
                }

                if (validationErrorResId != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CoralChronos.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, CoralChronos.copy(alpha = 0.42f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(validationErrorResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoralChronos,
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("editor_validation_error")
                        )
                    }
                }

                // Section 6: Bottom Actions (Translucent Cancel + Smooth Gradient Save)
                StaggeredSheetSection(index = 6) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val actionShape = RoundedCornerShape(16.dp)
                        OutlinedButton(
                            onClick = {
                                voiceManager.stopListening()
                                onDismiss()
                            },
                            shape = actionShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = HarmonyGlassCard.copy(alpha = 0.60f),
                                contentColor = TextSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("cancel_block_button")
                        ) {
                            Text(
                                text = stringResource(R.string.action_cancel),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        val saveGradient = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor,
                                lerpColor(accentColor, Color.White, 0.22f)
                            )
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .clip(actionShape)
                                .background(brush = saveGradient, shape = actionShape)
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.28f),
                                    shape = actionShape
                                )
                                .clickable(role = Role.Button) {
                                    voiceManager.stopListening()
                                    onSave()
                                }
                                .testTag("save_block_button")
                        ) {
                            Text(
                                text = stringResource(
                                    if (isEditing) R.string.action_update else R.string.action_save
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianVoid
                            )
                        }
                    }
                }
            }
        }
    }

    timePickerTarget?.let { target ->
        val initialMinutes = when (target) {
            TimePickerTarget.START -> draft.startMinute
            TimePickerTarget.END -> draft.endMinute.coerceAtMost(23 * 60 + 59)
        }
        val pickerState = rememberTimePickerState(
            initialHour = (initialMinutes / 60).coerceIn(0, 23),
            initialMinute = (initialMinutes % 60).coerceIn(0, 59),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { timePickerTarget = null },
            containerColor = ObsidianSurfaceElevated,
            titleContentColor = TextPrimary,
            title = {
                Text(
                    text = stringResource(
                        if (target == TimePickerTarget.START) R.string.field_start_time
                        else R.string.field_end_time
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedMinutes = pickerState.hour * 60 + pickerState.minute
                        onDraftChange { current ->
                            when (target) {
                                TimePickerTarget.START -> {
                                    val duration = (current.endMinute - current.startMinute)
                                        .coerceAtLeast(15)
                                    val adjustedEnd = (pickedMinutes + duration)
                                        .coerceAtMost(TimelineMath.MINUTES_IN_DAY)
                                    current.copy(
                                        startMinute = pickedMinutes.coerceAtMost(TimelineMath.MINUTES_IN_DAY - 1),
                                        endMinute = adjustedEnd
                                    )
                                }

                                TimePickerTarget.END -> {
                                    current.copy(
                                        endMinute = pickedMinutes.coerceIn(1, TimelineMath.MINUTES_IN_DAY)
                                    )
                                }
                            }
                        }
                        timePickerTarget = null
                    }
                ) {
                    Text(stringResource(R.string.action_confirm), color = CyanChronos)
                }
            },
            dismissButton = {
                TextButton(onClick = { timePickerTarget = null }) {
                    Text(stringResource(R.string.action_cancel), color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun StaggeredSheetSection(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 45L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        ) + slideInVertically(
            initialOffsetY = { fullHeight -> (fullHeight * 0.35f).toInt().coerceAtLeast(24) },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    ) {
        content()
    }
}

@Composable
private fun HarmonyModularCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.White.copy(alpha = 0.14f),
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = HarmonyGlassCard.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun TimeBoxSelector(
    label: String,
    timeText: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = HarmonyGlassBase.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (stop.red - start.red) * t,
        green = start.green + (stop.green - start.green) * t,
        blue = start.blue + (stop.blue - start.blue) * t,
        alpha = start.alpha + (stop.alpha - start.alpha) * t
    )
}
