package me.subhrajyoti.translate

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val modelManager = LanguageModelManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                        val canDelete =
                            if (isDownloaded) modelManager.canDeleteModel(code) else false
                        LanguageUiModel(
                            code = code,
                            name = modelManager.getDisplayName(code),
                            status = if (isDownloaded) ModelStatus.DOWNLOADED else ModelStatus.NOT_DOWNLOADED,
                            canDelete = canDelete
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
            title = stringResource(R.string.app_name),
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
                    val success = modelManager.deleteModel(code)
                    if (success) {
                        refreshLanguages()
                    } else {
                        Toast.makeText(
                            context,
                            "Cannot delete this language. It may be pre-installed or required by the system.",
                            Toast.LENGTH_LONG
                        ).show()
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
