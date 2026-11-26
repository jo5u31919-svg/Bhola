package com.cronos.timeline.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceTargetField {
    TITLE,
    NOTE
}

data class VoiceDictationState(
    val isListening: Boolean = false,
    val activeField: VoiceTargetField? = null,
    val partialTranscript: String = "",
    val errorCode: Int? = null,
    val isRecognizerAvailable: Boolean = true
)

class VoiceDictationManager(
    private val context: Context,
    private val onFinalTranscript: (VoiceTargetField, String) -> Unit
) {
    private val _state = MutableStateFlow(
        VoiceDictationState(
            isRecognizerAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        )
    )
    val state: StateFlow<VoiceDictationState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    fun startListening(field: VoiceTargetField) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = _state.value.copy(
                isListening = false,
                isRecognizerAvailable = false,
                activeField = null
            )
            return
        }

        stopListening()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = _state.value.copy(
                    isListening = true,
                    activeField = field,
                    partialTranscript = "",
                    errorCode = null
                )
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                _state.value = _state.value.copy(isListening = false)
            }

            override fun onError(error: Int) {
                _state.value = _state.value.copy(
                    isListening = false,
                    activeField = null,
                    partialTranscript = "",
                    errorCode = error
                )
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val transcript = matches?.firstOrNull()?.trim().orEmpty()
                if (transcript.isNotEmpty()) {
                    onFinalTranscript(field, transcript)
                }
                _state.value = _state.value.copy(
                    isListening = false,
                    activeField = null,
                    partialTranscript = ""
                )
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (partial.isNotEmpty()) {
                    _state.value = _state.value.copy(partialTranscript = partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        _state.value = _state.value.copy(
            isListening = true,
            activeField = field,
            partialTranscript = "",
            errorCode = null,
            isRecognizerAvailable = true
        )
        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.let { recognizer ->
            runCatching { recognizer.stopListening() }
            runCatching { recognizer.destroy() }
        }
        speechRecognizer = null
        _state.value = _state.value.copy(
            isListening = false,
            activeField = null,
            partialTranscript = ""
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(errorCode = null)
    }

    fun destroy() {
        stopListening()
    }
}
