package com.aikeyboard.feature.keyboard.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.inputmethod.InputConnection
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.aikeyboard.BuildConfig
import com.aikeyboard.core.base.BaseViewModel
import com.aikeyboard.feature.clipboard.domain.model.ClipboardEntry
import com.aikeyboard.feature.clipboard.domain.usecase.GetClipboardItemsUseCase
import com.aikeyboard.feature.clipboard.domain.usecase.SaveToCredentialsUseCase
import com.aikeyboard.feature.credentials.domain.model.CredIconType
import com.aikeyboard.feature.credentials.domain.model.CredentialEntry
import com.aikeyboard.feature.credentials.domain.usecase.AddCredentialUseCase
import com.aikeyboard.feature.credentials.domain.usecase.GetCredentialsUseCase
import com.aikeyboard.feature.keyboard.domain.model.*
import com.aikeyboard.feature.keyboard.domain.usecase.GetSuggestionsUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single ViewModel for the entire keyboard surface.
 * All UI state lives here; composables only read [state] and call functions.
 */
class KeyboardViewModel(
    private val getSuggestionsUseCase    : GetSuggestionsUseCase,
    private val getClipboardItemsUseCase : GetClipboardItemsUseCase,
    private val getCredentialsUseCase    : GetCredentialsUseCase,
    private val addCredentialUseCase     : AddCredentialUseCase,
    private val saveToCredentialsUseCase : SaveToCredentialsUseCase,
    private val context                  : Context,
) : BaseViewModel<KeyboardState>(KeyboardState()) {

    // ── Input Connection ──────────────────────────────────────────────────────
    
    private var inputConnection: InputConnection? = null
    
    fun setInputConnection(ic: InputConnection?) {
        inputConnection = ic
    }

    // ── Clipboard & Credentials (streamed from their own features) ────────────

    val clipboardItems: StateFlow<List<com.aikeyboard.feature.clipboard.domain.model.ClipboardEntry>> =
        getClipboardItemsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val credentials: StateFlow<List<com.aikeyboard.feature.credentials.domain.model.CredentialEntry>> =
        getCredentialsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Voice Recognition ─────────────────────────────────────────────────────
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isSpeechAvailable = false
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private val isVoiceActiveFlag = AtomicBoolean(false)
    private val voiceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    init {
        initSpeechRecognizer()
    }
    
    private fun initSpeechRecognizer() {
        isSpeechAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        if (isSpeechAvailable) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    // ── Typing ────────────────────────────────────────────────────────────────

    /** Insert one character or string into the input field. */
    fun insert(text: String) {
        inputConnection?.commitText(text, 1)
        val newCaps = if (state.value.language == KeyboardLanguage.EN && state.value.isCaps && text.length == 1) false else state.value.isCaps
        val newBuffer = state.value.bufferText + text
        setState { copy(bufferText = newBuffer, isCaps = newCaps, suggestions = getSuggestions(newBuffer)) }
    }

    /** Delete the last character. */
    fun backspace() {
        inputConnection?.deleteSurroundingText(1, 0)
        val newBuffer = state.value.bufferText.dropLast(1).takeIf { state.value.bufferText.isNotEmpty() } ?: ""
        setState { copy(bufferText = newBuffer, suggestions = getSuggestions(newBuffer)) }
    }

    /** Replace the current partial word with the tapped prediction. */
    fun usePrediction(word: String) {
        val buffer = state.value.bufferText
        val newBuffer = if (!buffer.endsWith(' ') && !buffer.endsWith('\n') && buffer.isNotEmpty()) {
            val trimmed = Regex("[a-zA-Z]+$").find(buffer)?.let { m ->
                buffer.dropLast(m.value.length)
            } ?: buffer
            trimmed + word + " "
        } else {
            buffer + word + " "
        }
        
        // Delete current word and insert new one
        val currentWord = Regex("[a-zA-Z]+$").find(buffer)?.value?.length ?: 0
        if (currentWord > 0) {
            inputConnection?.deleteSurroundingText(currentWord, 0)
        }
        inputConnection?.commitText(word + " ", 1)
        
        setState { copy(bufferText = newBuffer, suggestions = getSuggestions(newBuffer)) }
    }

    private fun getSuggestions(buffer: String) = getSuggestionsUseCase(buffer)

    // ── Layout controls ───────────────────────────────────────────────────────

    fun toggleCaps()    = setState { copy(isCaps = !isCaps) }
    fun toggleMode()    = setState { copy(mode = if (mode == KeyboardMode.NUM) KeyboardMode.ALPHA else KeyboardMode.NUM) }
    fun swapLanguage()  = setState { copy(language = if (language == KeyboardLanguage.EN) KeyboardLanguage.BN else KeyboardLanguage.EN, mode = KeyboardMode.ALPHA, isCaps = false) }

    // ── Overlay navigation ────────────────────────────────────────────────────

    fun showOverlay(type: OverlayType) = setState { copy(activeOverlay = type) }
    fun hideOverlay()                  = setState { copy(activeOverlay = OverlayType.NONE) }

    // ── Voice input ───────────────────────────────────────────────────────────

    fun startVoice() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            setState { copy(voiceStatus = "Microphone permission required") }
            return
        }
        
        isVoiceActiveFlag.set(true)
        setState { copy(isVoiceActive = true, voiceLanguage = language, voiceStatus = "Listening...") }
        
        if (state.value.voiceEngine == VoiceEngine.ANDROID && isSpeechAvailable && speechRecognizer != null) {
            startAndroidVoice()
        } else if (state.value.voiceEngine == VoiceEngine.GROQ && isNetworkAvailable()) {
            startGroqVoice()
        } else {
            setState { copy(voiceStatus = "Voice not available", isVoiceActive = false) }
            isVoiceActiveFlag.set(false)
        }
    }
    
    fun stopVoice() {
        isVoiceActiveFlag.set(false)
        speechRecognizer?.stopListening()
        mediaRecorder?.apply { stop(); release() }
        mediaRecorder = null
        setState { copy(isVoiceActive = false, voiceStatus = "") }
    }
    
    fun toggleVoiceLanguage() = setState {
        copy(voiceLanguage = if (voiceLanguage == KeyboardLanguage.EN) KeyboardLanguage.BN else KeyboardLanguage.EN)
    }
    
    fun toggleVoiceEngine() = setState {
        copy(voiceEngine = if (voiceEngine == VoiceEngine.ANDROID) VoiceEngine.GROQ else VoiceEngine.ANDROID)
    }

    private fun startAndroidVoice() {
        try {
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(b: Bundle?) {
                    setState { copy(voiceStatus = "Listening...") }
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(r: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {
                    if (isVoiceActiveFlag.get()) {
                        voiceScope.launch { delay(500); startAndroidVoice() }
                    }
                }
                override fun onError(e: Int) {
                    if (isVoiceActiveFlag.get() && e != 7) {
                        setState { copy(voiceStatus = "Error: $e, retrying...") }
                        voiceScope.launch { delay(1000); startAndroidVoice() }
                    }
                }
                override fun onResults(b: Bundle?) {
                    b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { 
                        insert(it + " ")
                    }
                    if (isVoiceActiveFlag.get()) {
                        voiceScope.launch { delay(300); startAndroidVoice() }
                    }
                }
                override fun onPartialResults(b: Bundle?) {
                    b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                        setState { copy(voiceStatus = it) }
                    }
                }
                override fun onEvent(p: Int, b: Bundle?) {}
            })
            
            speechRecognizer?.startListening(android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (state.value.voiceLanguage == KeyboardLanguage.BN) "bn-BD" else "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            })
        } catch (e: Exception) {
            setState { copy(voiceStatus = "Error: ${e.message}") }
            stopVoice()
        }
    }

    private fun startGroqVoice() {
        try {
            val recordingStart = System.currentTimeMillis()
            audioFile = File(context.cacheDir, "voice_${recordingStart}.m4a")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }
            setState { copy(voiceStatus = "Recording... (tap stop to transcribe)") }
            
            // Auto-stop after 10 seconds
            voiceScope.launch {
                delay(10000)
                if (isVoiceActiveFlag.get()) {
                    stopAndTranscribeGroq()
                }
            }
        } catch (e: Exception) {
            setState { copy(voiceStatus = "Error: ${e.message}") }
            stopVoice()
        }
    }
    
    private fun stopAndTranscribeGroq() {
        mediaRecorder?.apply { stop(); release() }
        mediaRecorder = null
        
        val file = audioFile ?: return
        
        setState { copy(voiceStatus = "Transcribing with Groq...") }
        
        voiceScope.launch(Dispatchers.IO) {
            try {
                val result = transcribeWithGroq(file)
                withContext(Dispatchers.Main) {
                    if (result != null) {
                        insert(result + " ")
                        setState { copy(voiceStatus = "Listening...") }
                    } else {
                        setState { copy(voiceStatus = "Transcription failed") }
                    }
                    if (isVoiceActiveFlag.get()) {
                        delay(300)
                        startGroqVoice()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setState { copy(voiceStatus = "Error: ${e.message}") }
                }
            }
            file.delete()
        }
    }
    
    private suspend fun transcribeWithGroq(audioFile: File): String? = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GROQ_API_KEY
            if (apiKey == "YOUR_GROQ_API_KEY_HERE") {
                return@withContext null
            }
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", "whisper-large-v3")
                .addFormDataPart("language", if (state.value.voiceLanguage == KeyboardLanguage.BN) "bn" else "en")
                .addFormDataPart("file", audioFile.name, audioFile.asRequestBody("audio/m4a".toMediaType()))
                .build()
            
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/audio/transcriptions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                // Parse JSON response to get text
                responseBody?.let { body ->
                    val textMatch = Regex("\"text\"\\s*:\\s*\"([^\"]+)\"").find(body)
                    textMatch?.groupValues?.get(1)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.activeNetwork?.let { cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } ?: false
        } else @Suppress("DEPRECATION") cm.activeNetworkInfo?.isConnected == true
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun toggleDarkTheme() = setState { copy(isDarkTheme = !isDarkTheme) }
    fun toggleHaptic()    = setState { copy(isHapticEnabled = !isHapticEnabled) }

    // ── Clipboard / Credentials actions ──────────────────────────────────────

    fun pasteFromClipboard(text: String) { insert(text); hideOverlay() }
    fun pasteFromCredential(text: String) { insert(text); hideOverlay() }

    fun saveClipToCredentials(clip: ClipboardEntry) {
        viewModelScope.launch { saveToCredentialsUseCase(clip) }
    }
    
    // ── Credential Management ────────────────────────────────────────────────
    
    fun addCredential(name: String, value: String) {
        viewModelScope.launch {
            val entry = CredentialEntry(
                id = UUID.randomUUID().toString(),
                text = value,
                label = name,
                iconType = CredIconType.LOCK
            )
            addCredentialUseCase(entry)
        }
    }
    
    fun showAddCredentialDialog() = setState { copy(showAddCredential = true) }
    fun hideAddCredentialDialog() = setState { copy(showAddCredential = false) }
    
    fun setCredentialName(name: String) = setState { copy(newCredentialName = name) }
    fun setCredentialValue(value: String) = setState { copy(newCredentialValue = value) }
    
    fun saveNewCredential() {
        val name = state.value.newCredentialName
        val value = state.value.newCredentialValue
        if (name.isNotBlank() && value.isNotBlank()) {
            addCredential(name, value)
            setState { copy(showAddCredential = false, newCredentialName = "", newCredentialValue = "") }
        }
    }
    
    // ── Cleanup ──────────────────────────────────────────────────────────────
    
    override fun onCleared() {
        super.onCleared()
        voiceScope.cancel()
        speechRecognizer?.destroy()
        mediaRecorder?.release()
    }
}
