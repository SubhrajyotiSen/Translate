package me.subhrajyoti.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.tasks.await
import java.util.Locale

class LanguageModelManager {

    private val modelManager = RemoteModelManager.getInstance()

    fun getAvailableLanguages(): List<String> {
        return TranslateLanguage.getAllLanguages()
    }

    suspend fun isModelDownloaded(languageCode: String): Boolean {
        val model = TranslateRemoteModel.Builder(languageCode).build()
        return modelManager.isModelDownloaded(model).await()
    }

    suspend fun downloadModel(languageCode: String) {
        val model = TranslateRemoteModel.Builder(languageCode).build()
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        modelManager.download(model, conditions).await()
    }

    suspend fun deleteModel(languageCode: String): Boolean {
        return try {
            val model = TranslateRemoteModel.Builder(languageCode).build()
            modelManager.deleteDownloadedModel(model).await()
            true
        } catch (e: Exception) {
            // Model might be pre-installed or deletion failed
            false
        }
    }

    suspend fun canDeleteModel(languageCode: String): Boolean {
        // English is typically pre-installed and cannot be deleted
        // Also check if model is actually downloaded
        if (languageCode == TranslateLanguage.ENGLISH) {
            return false
        }
        return isModelDownloaded(languageCode)
    }

    fun getDisplayName(languageCode: String): String {
        return Locale.Builder().setLanguage(languageCode).build().displayName
    }
}
