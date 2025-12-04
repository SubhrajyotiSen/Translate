package me.subhrajyoti.translate

import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class TranslationSession(context: Context) : VoiceInteractionSession(context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var overlayContainer: FrameLayout? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        // Ensure we request the screenshot
        // The system typically calls onHandleScreenshot after onShow if the flags are right or default.
        // However, we can just wait for onHandleScreenshot.
        // We might need to set a loading UI here.

        // Clear previous overlay
        overlayContainer?.removeAllViews()

        // Setup basic UI
        if (overlayContainer == null) {
            overlayContainer = FrameLayout(context)
            overlayContainer?.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            overlayContainer?.setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent background

            val loadingText = TextView(context).apply {
                text = "Reading Screen..."
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            }
            overlayContainer?.addView(loadingText)
            window.setContentView(overlayContainer!!)
        }
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        if (screenshot == null) {
            Toast.makeText(context, "No screenshot captured", Toast.LENGTH_SHORT).show()
            hide()
            return
        }

        processScreenshot(screenshot)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onHandleAssist(
        data: Bundle?,
        structure: AssistStructure?,
        content: AssistContent?
    ) {
        super.onHandleAssist(data, structure, content)
        // We rely on onHandleScreenshot. 
        // Sometimes onHandleScreenshot is not called if secure flag is set on the target app.
    }

    private fun processScreenshot(bitmap: Bitmap) {
        scope.launch {
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                val visionText = recognizer.process(image).await()

                if (visionText.textBlocks.isEmpty()) {
                    showToast("No text found")
                    hide()
                    return@launch
                }

                // 1. Detect Language (using the full text for better accuracy)
                val fullText = visionText.text
                val languageIdentifier = LanguageIdentification.getClient()
                val languageCode = languageIdentifier.identifyLanguage(fullText).await()

                if (languageCode == "und") {
                    showToast("Could not identify language")
                    return@launch
                }

                // 2. Prepare Translator
                val sourceLang =
                    TranslateLanguage.fromLanguageTag(languageCode) ?: TranslateLanguage.ENGLISH
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(TranslateLanguage.ENGLISH)
                    .build()
                val translator = Translation.getClient(options)

                // Download model if needed
                val conditions = DownloadConditions.Builder()
                    .requireWifi()
                    .build()

                // Show downloading status
                showLoading("Downloading translation model...")
                translator.downloadModelIfNeeded(conditions).await()

                // 3. Translate and Draw
                showLoading("Translating...")

                // Prepare overlay
                overlayContainer?.removeAllViews()
                // Add close button or tap to dismiss
                overlayContainer?.setOnClickListener {
                    hide()
                }

                val processedBlocks: List<Text.TextBlock> = visionText.textBlocks
                for (block in processedBlocks) {
                    val blockText = block.text
                    val translatedText = translator.translate(blockText).await()
                    val rect = block.boundingBox

                    if (rect != null) {
                        drawTextOverlay(translatedText, rect)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error: ${e.message}")
                hide()
            }
        }
    }

    private fun drawTextOverlay(text: String, rect: Rect) {
        val textView = TextView(context)
        textView.text = text
        textView.setTextColor(Color.WHITE)
        textView.setBackgroundColor(Color.BLACK) // Solid background to cover original text
        textView.textSize = 14f // Dynamic size would be better but fixed is okay for now
        textView.setPadding(4, 4, 4, 4)

        val params = FrameLayout.LayoutParams(
            rect.width(),
            FrameLayout.LayoutParams.WRAP_CONTENT // Allow height to expand
        )
        params.leftMargin = rect.left
        params.topMargin = rect.top
        textView.layoutParams = params

        overlayContainer?.addView(textView)
    }

    private fun showLoading(message: String) {
        overlayContainer?.removeAllViews()
        val loadingText = TextView(context).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 20f
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
        overlayContainer?.addView(loadingText)
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
