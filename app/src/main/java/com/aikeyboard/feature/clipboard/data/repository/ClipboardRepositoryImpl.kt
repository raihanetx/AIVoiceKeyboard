package com.aikeyboard.feature.clipboard.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.aikeyboard.feature.clipboard.domain.model.ClipIconType
import com.aikeyboard.feature.clipboard.domain.model.ClipboardEntry
import com.aikeyboard.feature.clipboard.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ClipboardRepositoryImpl(
    private val context: Context
) : ClipboardRepository {

    companion object {
        private const val PREFS_NAME = "PixelProKeyboardPrefs"
        private const val CLIPBOARD_KEY = "clipboard_history"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(loadClipboardItems())

    override fun getClipboardItems(): Flow<List<ClipboardEntry>> = _items.asStateFlow()

    override suspend fun removeItem(id: String) {
        val currentList = _items.value.filterNot { it.id == id }
        _items.value = currentList
        saveClipboardItems(currentList)
    }
    
    fun addToClipboard(text: String) {
        if (text.length <= 2) return // Skip short text
        
        val iconType = when {
            text.startsWith("http://") || text.startsWith("https://") -> ClipIconType.LINK
            else -> ClipIconType.TEXT
        }
        
        val entry = ClipboardEntry(
            id = UUID.randomUUID().toString(),
            text = text,
            timeLabel = "Just now",
            iconType = iconType
        )
        
        val currentList = _items.value.toMutableList()
        // Remove duplicate
        currentList.removeAll { it.text == text }
        // Add at beginning
        currentList.add(0, entry)
        // Limit to 20 items
        if (currentList.size > 20) {
            currentList.removeLast()
        }
        
        _items.value = currentList
        saveClipboardItems(currentList)
    }

    private fun loadClipboardItems(): List<ClipboardEntry> {
        val json = prefs.getString(CLIPBOARD_KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ClipboardEntry(
                    id = obj.optString("id", "clip_$i"),
                    text = obj.optString("text", ""),
                    timeLabel = obj.optString("timeLabel", ""),
                    iconType = try { ClipIconType.valueOf(obj.optString("iconType", "TEXT")) } catch (e: Exception) { ClipIconType.TEXT }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveClipboardItems(items: List<ClipboardEntry>) {
        val arr = JSONArray()
        items.forEach { entry ->
            arr.put(JSONObject().apply {
                put("id", entry.id)
                put("text", entry.text)
                put("timeLabel", entry.timeLabel)
                put("iconType", entry.iconType.name)
            })
        }
        prefs.edit().putString(CLIPBOARD_KEY, arr.toString()).apply()
    }
}
