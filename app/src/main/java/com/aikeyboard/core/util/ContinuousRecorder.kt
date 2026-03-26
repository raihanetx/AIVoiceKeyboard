package com.aikeyboard.core.util

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "ContinuousRecorder"

/**
 * Continuous audio recorder - records until manually stopped
 * Solves the problem of Android SpeechRecognizer auto-stopping
 */
class ContinuousRecorder(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private var audioFile: File? = null
    private var fileOutputStream: FileOutputStream? = null

    // Audio settings
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

    /**
     * Start recording - will continue until stopRecording() is called
     */
    fun startRecording(): File? {
        if (isRecording.get()) {
            Log.w(TAG, "Already recording")
            return null
        }

        try {
            // Create output file (WAV format for Android offline)
            val fileName = "${context.cacheDir.absolutePath}/voice_${System.currentTimeMillis()}.wav"
            audioFile = File(fileName)

            // Initialize AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                return null
            }

            // Create file and write WAV header placeholder
            fileOutputStream = FileOutputStream(audioFile)
            writeWavHeader(fileOutputStream!!, 0) // Placeholder, will update on stop

            // Start recording
            audioRecord?.startRecording()
            isRecording.set(true)

            // Recording thread
            recordingThread = Thread {
                val buffer = ByteArray(bufferSize)
                var totalBytes = 0L

                while (isRecording.get()) {
                    val bytesread = audioRecord?.read(buffer, 0, bufferSize) ?: -1
                    if (bytesread > 0) {
                        try {
                            fileOutputStream?.write(buffer, 0, bytesread)
                            totalBytes += bytesread
                        } catch (e: IOException) {
                            Log.e(TAG, "Error writing audio data", e)
                            break
                        }
                    }
                }

                // Update WAV header with actual size
                try {
                    fileOutputStream?.close()
                    updateWavHeader(audioFile!!, totalBytes)
                } catch (e: Exception) {
                    Log.e(TAG, "Error finalizing WAV file", e)
                }
            }.apply { start() }

            Log.d(TAG, "Recording started: $fileName")
            return audioFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            release()
            return null
        }
    }

    /**
     * Stop recording and return the audio file
     */
    fun stopRecording(): File? {
        if (!isRecording.get()) {
            Log.w(TAG, "Not recording")
            return null
        }

        isRecording.set(false)

        try {
            // Stop recording
            audioRecord?.stop()
            recordingThread?.join(1000)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        } finally {
            release()
        }

        val file = audioFile
        if (file?.exists() == true && file.length() > 44) { // 44 = WAV header size
            Log.d(TAG, "Recording saved: ${file.absolutePath}, size: ${file.length()}")
            return file
        } else {
            Log.w(TAG, "Recording file is empty or invalid")
            file?.delete()
            return null
        }
    }

    /**
     * Check if currently recording
     */
    fun isCurrentlyRecording(): Boolean = isRecording.get()

    /**
     * Cancel recording
     */
    fun cancelRecording() {
        isRecording.set(false)
        try {
            audioRecord?.stop()
            recordingThread?.join(500)
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling", e)
        } finally {
            release()
            audioFile?.delete()
            audioFile = null
        }
    }

    /**
     * Release resources
     */
    private fun release() {
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord", e)
        }
        audioRecord = null
        recordingThread = null
        fileOutputStream = null
    }

    /**
     * Write WAV file header
     */
    private fun writeWavHeader(out: FileOutputStream, dataLength: Long) {
        val totalDataLen = dataLength + 36
        val byteRate = (sampleRate * 2).toLong() // 16-bit mono

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat (PCM)
        header[21] = 0
        header[22] = 1 // NumChannels (mono)
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = 2 // BlockAlign
        header[33] = 0
        header[34] = 16 // BitsPerSample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (dataLength and 0xff).toByte()
        header[41] = (dataLength shr 8 and 0xff).toByte()
        header[42] = (dataLength shr 16 and 0xff).toByte()
        header[43] = (dataLength shr 24 and 0xff).toByte()

        out.write(header)
    }

    /**
     * Update WAV header with actual data size
     */
    private fun updateWavHeader(file: File, dataLength: Long) {
        try {
            val randomAccessFile = java.io.RandomAccessFile(file, "rw")
            randomAccessFile.seek(4)
            randomAccessFile.write(((dataLength + 36) and 0xff).toInt())
            randomAccessFile.write(((dataLength + 36) shr 8 and 0xff).toInt())
            randomAccessFile.write(((dataLength + 36) shr 16 and 0xff).toInt())
            randomAccessFile.write(((dataLength + 36) shr 24 and 0xff).toInt())
            randomAccessFile.seek(40)
            randomAccessFile.write((dataLength and 0xff).toInt())
            randomAccessFile.write((dataLength shr 8 and 0xff).toInt())
            randomAccessFile.write((dataLength shr 16 and 0xff).toInt())
            randomAccessFile.write((dataLength shr 24 and 0xff).toInt())
            randomAccessFile.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating WAV header", e)
        }
    }

    fun release() {
        cancelRecording()
    }
}
