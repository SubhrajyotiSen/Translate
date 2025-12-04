package me.subhrajyoti.translate

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val modelManager = LanguageModelManager()
    private val PREFS_NAME = "TranslatePrefs"
    private val KEY_DEFAULT_LANG = "default_language_code"

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

        // State
        var languages by remember { mutableStateOf(emptyList<LanguageOption>()) }
        var selectedLanguage by remember { mutableStateOf<LanguageOption?>(null) }
        var statusText by remember { mutableStateOf("") }
        var statusColor by remember { mutableStateOf(Color.Black) }
        var isWorking by remember { mutableStateOf(false) } // Downloading or checking
        var isDownloaded by remember { mutableStateOf(false) }

        // Logic to check status
        fun checkStatus(code: String) {
            scope.launch {
                isWorking = true
                statusText = "Checking status..."
                statusColor = Color.Black

                isDownloaded = modelManager.isModelDownloaded(code)
                isWorking = false

                if (isDownloaded) {
                    statusText = "Model is ready"
                    statusColor = Color.Green
                } else {
                    statusText = "Model not downloaded"
                    statusColor = Color.Red
                }
            }
        }

        // Initialize
        LaunchedEffect(Unit) {
            languages = modelManager.getAvailableLanguages().map { code ->
                LanguageOption(modelManager.getDisplayName(code), code)
            }.sortedBy { it.name }

            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val savedLangCode = prefs.getString(KEY_DEFAULT_LANG, "en")

            val initialLang = languages.find { it.code == savedLangCode } ?: languages.firstOrNull()
            selectedLanguage = initialLang

            initialLang?.let { checkStatus(it.code) }
        }

        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                    ) {
                        Text("Set as Default Assistant")
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text("Select Default Language")

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { expanded = true }) {
                            Text(selectedLanguage?.name ?: "Select Language")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.name) },
                                    onClick = {
                                        selectedLanguage = lang
                                        expanded = false
                                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                            .edit()
                                            .putString(KEY_DEFAULT_LANG, lang.code)
                                            .apply()
                                        checkStatus(lang.code)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = statusText, color = statusColor)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isWorking) {
                        CircularProgressIndicator()
                    } else {
                        val currentLang = selectedLanguage
                        if (!isDownloaded && currentLang != null) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isWorking = true
                                        statusText = "Downloading..."
                                        statusColor = Color.Black
                                        try {
                                            modelManager.downloadModel(currentLang.code)
                                            statusText = "Download Complete"
                                            statusColor = Color.Green
                                            isDownloaded = true
                                        } catch (e: Exception) {
                                            statusText = "Download Failed: ${e.message}"
                                            statusColor = Color.Red
                                        } finally {
                                            isWorking = false
                                        }
                                    }
                                }
                            ) {
                                Text("Download Model")
                            }
                        }
                    }
                }
            }
        }
    }

    data class LanguageOption(val name: String, val code: String)
}
