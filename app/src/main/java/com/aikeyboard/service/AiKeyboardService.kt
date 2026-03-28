package com.aikeyboard.service

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import com.aikeyboard.R
import com.aikeyboard.BuildConfig
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AiKeyboardService : android.inputmethodservice.InputMethodService() {

    private enum class KeyboardMode { ENGLISH, BANGLA, NUMBERS }
    private enum class VoiceEngine { ANDROID, GROQ }

    private var currentMode = KeyboardMode.ENGLISH
    private var currentEngine = VoiceEngine.ANDROID
    private var isCapsLock = false
    private var isShift = false
    private var isVoiceActive = false
    private var isRecording = false

    // Views
    private lateinit var voiceBar: LinearLayout
    private lateinit var keyboardKeys: LinearLayout
    private lateinit var btnEngineAndroid: TextView
    private lateinit var btnEngineGroq: TextView
    private lateinit var voiceStatus: TextView

    // Voice recognition
    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val client = OkHttpClient()

    // All letter key IDs
    private val letterKeyIds = listOf(
        R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
        R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
        R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
        R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
        R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
        R.id.key_n, R.id.key_m
    )

    override fun onCreate() {
        super.onCreate()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
    }

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_view, null)
        
        voiceBar = root.findViewById(R.id.voiceBar)
        keyboardKeys = root.findViewById(R.id.keyboardKeys)
        btnEngineAndroid = root.findViewById(R.id.btnEngineAndroid)
        btnEngineGroq = root.findViewById(R.id.btnEngineGroq)
        voiceStatus = root.findViewById(R.id.voiceStatus)

        // Setup all letter key clicks
        letterKeyIds.forEach { id ->
            root.findViewById<Button>(id)?.setOnClickListener { v ->
                val btn = v as Button
                val letter = btn.text.toString()
                val text = if (isShift || isCapsLock) letter.uppercase() else letter.lowercase()
                commitText(text)
                if (isShift && !isCapsLock) {
                    isShift = false
                    updateKeyCase()
                }
            }
        }

        // Special keys
        root.findViewById<Button>(R.id.key_shift)?.setOnClickListener { toggleShift() }
        root.findViewById<Button>(R.id.key_backspace)?.setOnClickListener { handleBackspace() }
        root.findViewById<Button>(R.id.key_numbers)?.setOnClickListener { toggleNumbers() }
        root.findViewById<Button>(R.id.key_comma)?.setOnClickListener { commitText(",") }
        root.findViewById<Button>(R.id.key_space)?.setOnClickListener { commitText(" ") }
        root.findViewById<Button>(R.id.key_dot)?.setOnClickListener { commitText(".") }
        root.findViewById<Button>(R.id.key_enter)?.setOnClickListener { handleEnter() }

        // Toolbar buttons
        root.findViewById<ImageButton>(R.id.btnVoice)?.setOnClickListener { toggleVoice() }
        root.findViewById<ImageButton>(R.id.btnLanguage)?.setOnClickListener { cycleLanguage() }
        root.findViewById<ImageButton>(R.id.btnEmoji)?.setOnClickListener { 
            Toast.makeText(this, "Emoji coming soon!", Toast.LENGTH_SHORT).show() 
        }
        root.findViewById<ImageButton>(R.id.btnClipboard)?.setOnClickListener { 
            Toast.makeText(this, "Clipboard coming soon!", Toast.LENGTH_SHORT).show() 
        }

        // Voice engine selector
        btnEngineAndroid.setOnClickListener { selectEngine(VoiceEngine.ANDROID) }
        btnEngineGroq.setOnClickListener { selectEngine(VoiceEngine.GROQ) }
        root.findViewById<ImageButton>(R.id.btnStopVoice)?.setOnClickListener { stopVoice() }

        return root
    }

    private fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun handleBackspace() {
        currentInputConnection?.let { ic ->
            val selectedText = ic.getSelectedText(0)
            if (selectedText.isNullOrEmpty()) {
                ic.deleteSurroundingText(1, 0)
            } else {
                ic.commitText("", 1)
            }
        }
    }

    private fun handleEnter() {
        currentInputConnection?.let { ic ->
            val action = currentInputEditorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            when (action) {
                EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_SEND, EditorInfo.IME_ACTION_DONE -> {
                    ic.performEditorAction(action)
                }
                else -> ic.commitText("\n", 1)
            }
        }
    }

    private fun toggleShift() {
        if (isCapsLock) {
            isCapsLock = false
            isShift = false
        } else if (isShift) {
            isCapsLock = true
            isShift = false
        } else {
            isShift = true
        }
        updateKeyCase()
        Toast.makeText(this, 
            if (isCapsLock) "CAPS LOCK ON" 
            else if (isShift) "Shift ON" 
            else "Shift OFF", 
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateKeyCase() {
        letterKeyIds.forEach { id ->
            findViewById<Button>(id)?.let { btn ->
                val letter = btn.text.toString().lowercase()
                btn.text = if (isShift || isCapsLock) letter.uppercase() else letter
            }
        }
    }

    private fun toggleNumbers() {
        Toast.makeText(this, "Numbers/Symbols coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun cycleLanguage() {
        currentMode = when (currentMode) {
            KeyboardMode.ENGLISH -> KeyboardMode.BANGLA
            KeyboardMode.BANGLA -> KeyboardMode.NUMBERS
            KeyboardMode.NUMBERS -> KeyboardMode.ENGLISH
        }
        Toast.makeText(this, 
            when (currentMode) {
                KeyboardMode.ENGLISH -> "English"
                KeyboardMode.BANGLA -> "Bangla (coming soon)"
                KeyboardMode.NUMBERS -> "Numbers (coming soon)"
            }, 
            Toast.LENGTH_SHORT
        ).show()
    }

    // Voice Recognition
    private fun toggleVoice() {
        if (isVoiceActive) {
            stopVoice()
        } else {
            startVoice()
        }
    }

    private fun startVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please grant microphone permission", Toast.LENGTH_LONG).show()
            return
        }

        isVoiceActive = true
        isRecording = true
        voiceBar.visibility = View.VISIBLE
        keyboardKeys.visibility = View.GONE

        when (currentEngine) {
            VoiceEngine.ANDROID -> startAndroidRecognition()
            VoiceEngine.GROQ -> startGroqRecording()
        }
    }

    private fun stopVoice() {
        isVoiceActive = false
        isRecording = false
        voiceBar.visibility = View.GONE
        keyboardKeys.visibility = View.VISIBLE

        when (currentEngine) {
            VoiceEngine.ANDROID -> stopAndroidRecognition()
            VoiceEngine.GROQ -> stopGroqRecording()
        }
    }

    private fun selectEngine(engine: VoiceEngine) {
        currentEngine = engine
        updateEngineUI()
        if (isVoiceActive) {
            stopVoice()
            startVoice()
        }
    }

    private fun updateEngineUI() {
        if (currentEngine == VoiceEngine.ANDROID) {
            btnEngineAndroid.setTextColor(Color.WHITE)
            btnEngineAndroid.setBackgroundResource(R.drawable.engine_active_bg)
            btnEngineGroq.setTextColor(Color.parseColor("#5F6368"))
            btnEngineGroq.setBackgroundColor(Color.TRANSPARENT)
        } else {
            btnEngineGroq.setTextColor(Color.WHITE)
            btnEngineGroq.setBackgroundResource(R.drawable.engine_active_bg)
            btnEngineAndroid.setTextColor(Color.parseColor("#5F6368"))
            btnEngineAndroid.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    // Android Offline Recognition
    private fun startAndroidRecognition() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                voiceStatus.text = "🎤 Listening..."
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (isRecording) startAndroidRecognition()
            }
            override fun onError(error: Int) {
                if (isRecording && error != SpeechRecognizer.ERROR_NO_MATCH) {
                    startAndroidRecognition()
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { commitText(it) }
                if (isRecording) startAndroidRecognition()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (currentMode == KeyboardMode.BANGLA) "bn-BD" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopAndroidRecognition() {
        speechRecognizer?.stopListening()
    }

    // Groq Online Recognition
    private fun startGroqRecording() {
        try {
            audioFile = File(cacheDir, "voice_recording.m4a")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile!!.absolutePath)
                prepare()
                start()
            }
            voiceStatus.text = "🎤 Recording... Tap stop when done"
        } catch (e: Exception) {
            Toast.makeText(this, "Recording failed: ${e.message}", Toast.LENGTH_LONG).show()
            stopVoice()
        }
    }

    private fun stopGroqRecording() {
        try {
            mediaRecorder?.apply { stop(); release() }
            mediaRecorder = null
            audioFile?.let { file ->
                if (file.exists() && file.length() > 0) sendToGroq(file)
            }
        } catch (e: Exception) { }
        audioFile = null
    }

    private fun sendToGroq(audioFile: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-large-v3")
                    .addFormDataPart("file", "audio.m4a", audioFile.asRequestBody("audio/mp4".toMediaType()))
                    .addFormDataPart("language", if (currentMode == KeyboardMode.BANGLA) "bn" else "en")
                    .build()

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/audio/transcriptions")
                    .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val text = body.substringAfter("\"text\":\"").substringBefore("\"")
                        .replace("\\n", "\n").replace("\\\"", "\"")
                    withContext(Dispatchers.Main) { commitText(text) }
                }
            } catch (e: Exception) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        scope.cancel()
        mediaRecorder?.release()
    }
}
