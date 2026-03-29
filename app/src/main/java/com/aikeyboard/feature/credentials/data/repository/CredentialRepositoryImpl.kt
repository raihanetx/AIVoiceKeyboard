package com.aikeyboard.feature.credentials.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.aikeyboard.feature.credentials.domain.model.CredIconType
import com.aikeyboard.feature.credentials.domain.model.CredentialEntry
import com.aikeyboard.feature.credentials.domain.repository.CredentialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class CredentialRepositoryImpl(
    private val context: Context
) : CredentialRepository {

    companion object {
        private const val PREFS_NAME = "PixelProKeyboardPrefs"
        private const val CREDENTIALS_KEY = "saved_credentials"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(loadCredentials())

    override fun getCredentials(): Flow<List<CredentialEntry>> = _items.asStateFlow()

    override suspend fun addCredential(entry: CredentialEntry) {
        val currentList = _items.value.toMutableList()
        // Remove existing with same id
        currentList.removeAll { it.id == entry.id }
        // Add new at beginning
        currentList.add(0, entry)
        _items.value = currentList
        saveCredentials(currentList)
    }

    override suspend fun removeCredential(id: String) {
        val currentList = _items.value.filterNot { it.id == id }
        _items.value = currentList
        saveCredentials(currentList)
    }

    private fun loadCredentials(): List<CredentialEntry> {
        val json = prefs.getString(CREDENTIALS_KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                CredentialEntry(
                    id = obj.optString("id", "cr_$i"),
                    text = obj.optString("text", ""),
                    label = obj.optString("label", "Credential"),
                    iconType = try { CredIconType.valueOf(obj.optString("iconType", "LOCK")) } catch (e: Exception) { CredIconType.LOCK }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCredentials(credentials: List<CredentialEntry>) {
        val arr = JSONArray()
        credentials.forEach { entry ->
            arr.put(JSONObject().apply {
                put("id", entry.id)
                put("text", entry.text)
                put("label", entry.label)
                put("iconType", entry.iconType.name)
            })
        }
        prefs.edit().putString(CREDENTIALS_KEY, arr.toString()).apply()
    }
}
