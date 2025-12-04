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

    fun getDisplayName(languageCode: String): String {
        return Locale(languageCode).displayName
    }
}
