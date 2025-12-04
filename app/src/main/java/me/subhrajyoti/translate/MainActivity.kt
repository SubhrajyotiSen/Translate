package me.subhrajyoti.translate

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val modelManager = LanguageModelManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TranslateApp()
        }
    }

    @Composable
    fun TranslateApp() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var searchQuery by remember { mutableStateOf("") }
        var languages by remember { mutableStateOf(emptyList<LanguageUiModel>()) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        fun refreshLanguages() {
            scope.launch {
                try {
                    val available = modelManager.getAvailableLanguages()
                    val newLanguages = available.map { code ->
                        val isDownloaded = modelManager.isModelDownloaded(code)
                        LanguageUiModel(
                            code = code,
                            name = modelManager.getDisplayName(code),
                            status = if (isDownloaded) ModelStatus.DOWNLOADED else ModelStatus.NOT_DOWNLOADED
                        )
                    }.sortedBy { it.name }
                    languages = newLanguages
                    error = null
                } catch (e: Exception) {
                    error = e.message
                } finally {
                    isLoading = false
                }
            }
        }

        LaunchedEffect(Unit) {
            refreshLanguages()
        }

        ManageLanguagesScreen(
            languages = languages,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onDownload = { code ->
                scope.launch {
                    // Optimistic update
                    languages = languages.map {
                        if (it.code == code) it.copy(status = ModelStatus.DOWNLOADING) else it
                    }
                    try {
                        modelManager.downloadModel(code)
                        refreshLanguages()
                    } catch (e: Exception) {
                        // Revert on failure or show error
                        refreshLanguages() // Will reset state
                    }
                }
            },
            onDelete = { code ->
                scope.launch {
                    // Optimistic update (optional, but might be risky if delete fails)
                    languages = languages.map {
                        if (it.code == code) it.copy(status = ModelStatus.DOWNLOADING) else it // Show spinner while deleting? Or just remove immediately?
                    }
                    // Let's just delete and refresh
                    try {
                        modelManager.deleteModel(code)
                        refreshLanguages()
                    } catch (e: Exception) {
                        refreshLanguages()
                    }
                }
            },
            onCancel = { code ->
                // ML Kit doesn't easily support cancelling mid-flight in this simple setup without proper Task management.
                // For now, we just don't do anything or we could try to implement cancellation if we held onto the Task.
                // Given the scope, we'll just let it finish or fail.
            },
            onSetDefaultAssistant = {
                val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            },
            isLoading = isLoading,
            error = error,
            onRetry = {
                isLoading = true
                error = null
                refreshLanguages()
            }
        )
    }
}
