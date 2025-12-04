package me.subhrajyoti.translate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ModelStatus {
    DOWNLOADED,
    DOWNLOADING,
    NOT_DOWNLOADED
}

data class LanguageUiModel(
    val code: String,
    val name: String,
    val status: ModelStatus,
    val canDelete: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLanguagesScreen(
    languages: List<LanguageUiModel>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: (String) -> Unit, // Placeholder for canceling download
    onSetDefaultAssistant: () -> Unit,
    isLoading: Boolean,
    error: String? = null,
    onRetry: () -> Unit = {},
    title: String = "Translate"
) {
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (!isLoading && error == null) {
                Button(
                    onClick = onSetDefaultAssistant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5F5B7E) // Approximate color from image
                    )
                ) {
                    // Placeholder for assistant icon if available, using a dot for now
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable as Default Assistant")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            if (isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF3D6A42), // Approximate green
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading languages...", fontSize = 16.sp)
                }
            } else if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = "Error",
                        tint = Color(0xFF8B3A3A), // Dark red
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Couldn't connect",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Check your internet connection and try\nagain.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D6A42))
                    ) {
                        Text("Retry")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Bar
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        placeholder = { Text("Search available languages...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )

                    val downloaded =
                        languages.filter { it.status == ModelStatus.DOWNLOADED || it.status == ModelStatus.DOWNLOADING }
                    val available = languages.filter { it.status == ModelStatus.NOT_DOWNLOADED }
                        .filter { it.name.contains(searchQuery, ignoreCase = true) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp) // Space for bottom button
                    ) {
                        if (downloaded.isNotEmpty()) {
                            item {
                                SectionHeader("YOUR LANGUAGES")
                            }
                            items(downloaded) { language ->
                                LanguageItem(
                                    language = language,
                                    onAction = {
                                        when (language.status) {
                                            ModelStatus.DOWNLOADED -> onDelete(language.code)
                                            ModelStatus.DOWNLOADING -> onCancel(language.code)
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }

                        if (available.isNotEmpty()) {
                            item {
                                SectionHeader("AVAILABLE LANGUAGES")
                            }
                            items(available) { language ->
                                LanguageItem(
                                    language = language,
                                    onAction = { onDownload(language.code) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun LanguageItem(
    language: LanguageUiModel,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = language.name,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        when (language.status) {
            ModelStatus.DOWNLOADED -> {
                if (language.canDelete) {
                    IconButton(onClick = onAction) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.Gray
                        )
                    }
                } else {
                    // Show nothing for pre-installed languages
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            ModelStatus.DOWNLOADING -> {
                IconButton(onClick = onAction) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp,
                            color = Color(0xFF5F5B7E)
                        )
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            ModelStatus.NOT_DOWNLOADED -> {
                IconButton(onClick = onAction) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Download",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
fun PreviewLoading() {
    MaterialTheme {
        ManageLanguagesScreen(
            languages = emptyList(),
            searchQuery = "",
            onSearchQueryChange = {},
            onDownload = {},
            onDelete = {},
            onCancel = {},
            onSetDefaultAssistant = {},
            isLoading = true,
            title = "Translate"
        )
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
fun PreviewError() {
    MaterialTheme {
        ManageLanguagesScreen(
            languages = emptyList(),
            searchQuery = "",
            onSearchQueryChange = {},
            onDownload = {},
            onDelete = {},
            onCancel = {},
            onSetDefaultAssistant = {},
            isLoading = false,
            error = "Connection Error",
            title = "Translate"
        )
    }
}

@Preview(showBackground = true, name = "Content State")
@Composable
fun PreviewContent() {
    MaterialTheme {
        val languages = listOf(
            LanguageUiModel("en", "English (US)", ModelStatus.DOWNLOADED, canDelete = false),
            LanguageUiModel("fr", "French (Français)", ModelStatus.DOWNLOADING, canDelete = true),
            LanguageUiModel("de", "German (Deutsch)", ModelStatus.NOT_DOWNLOADED, canDelete = true),
            LanguageUiModel(
                "it",
                "Italian (Italiano)",
                ModelStatus.NOT_DOWNLOADED,
                canDelete = true
            ),
            LanguageUiModel("ja", "Japanese (日本語)", ModelStatus.NOT_DOWNLOADED, canDelete = true)
        )
        ManageLanguagesScreen(
            languages = languages,
            searchQuery = "",
            onSearchQueryChange = {},
            onDownload = {},
            onDelete = {},
            onCancel = {},
            onSetDefaultAssistant = {},
            isLoading = false,
            title = "Translate"
        )
    }
}
