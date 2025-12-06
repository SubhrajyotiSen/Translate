package me.subhrajyoti.translate

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.assist.AssistContent
import android.app.assist.AssistStructure
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.delay
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
    private var gradientAnimator: ValueAnimator? = null

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

            // Create animated gradient background
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.parseColor("#CC1a1a2e"), // Dark blue-black
                    Color.parseColor("#CC16213e"), // Deep blue
                    Color.parseColor("#CC0f3460")  // Navy blue
                )
            )
            overlayContainer?.background = gradientDrawable

            // Start with transparent overlay
            overlayContainer?.alpha = 0f

            val loadingText = TextView(context).apply {
                text = "Reading Screen..."
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
                alpha = 0f // Start invisible
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            }
            overlayContainer?.addView(loadingText)
            window.setContentView(overlayContainer!!)

            // Animate reveal
            animateReveal()
        } else {
            // Reset and animate for subsequent shows
            overlayContainer?.alpha = 0f
            animateReveal()
        }
    }

    private fun animateReveal() {
        // Fade in the overlay with gradient
        overlayContainer?.animate()
            ?.alpha(1f)
            ?.setDuration(500)
            ?.setInterpolator(AccelerateDecelerateInterpolator())
            ?.start()

        // Cancel previous gradient animation if any
        gradientAnimator?.cancel()

        // Animate gradient colors
        val gradientDrawable = overlayContainer?.background as? GradientDrawable
        if (gradientDrawable != null) {
            gradientAnimator = ValueAnimator.ofFloat(0f, 1f)
            gradientAnimator?.duration = 800
            gradientAnimator?.addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                val colors = intArrayOf(
                    blendColors(
                        Color.parseColor("#CC1a1a2e"),
                        Color.parseColor("#CC0f3460"),
                        fraction
                    ),
                    blendColors(
                        Color.parseColor("#CC16213e"),
                        Color.parseColor("#CC1a1a2e"),
                        fraction
                    ),
                    blendColors(
                        Color.parseColor("#CC0f3460"),
                        Color.parseColor("#CC16213e"),
                        fraction
                    )
                )
                gradientDrawable.colors = colors
            }
            gradientAnimator?.repeatCount = ValueAnimator.INFINITE
            gradientAnimator?.repeatMode = ValueAnimator.REVERSE
            gradientAnimator?.start()
        }
    }

    private fun stopGradientAnimation() {
        gradientAnimator?.cancel()
        gradientAnimator = null
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r = Color.red(color1) * inverseRatio + Color.red(color2) * ratio
        val g = Color.green(color1) * inverseRatio + Color.green(color2) * ratio
        val b = Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio
        val a = Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio
        return Color.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
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
                    stopGradientAnimation()
                    showToast("No text found")
                    hide()
                    return@launch
                }

                // 1. Detect Language (using the full text for better accuracy)
                val fullText = visionText.text
                val languageIdentifier = LanguageIdentification.getClient()
                val languageCode = languageIdentifier.identifyLanguage(fullText).await()

                if (languageCode == "und") {
                    stopGradientAnimation()
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

                // Add a small delay before showing translations for better UX
                delay(800)

                // Stop the gradient animation
                stopGradientAnimation()

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
                stopGradientAnimation()
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
        textView.alpha = 0f // Start invisible for animation

        val params = FrameLayout.LayoutParams(
            rect.width(),
            FrameLayout.LayoutParams.WRAP_CONTENT // Allow height to expand
        )
        params.leftMargin = rect.left
        params.topMargin = rect.top
        textView.layoutParams = params

        overlayContainer?.addView(textView)

        // Animate fade in with slight scale
        textView.scaleX = 0.95f
        textView.scaleY = 0.95f
        textView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun showLoading(message: String) {
        overlayContainer?.removeAllViews()
        val loadingText = TextView(context).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 20f
            alpha = 0f // Start invisible
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
        overlayContainer?.addView(loadingText)

        // Animate loading text fade in
        loadingText.animate()
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Add a gentle pulse animation
        val scaleAnimator = ObjectAnimator.ofFloat(loadingText, "scaleX", 1f, 1.05f, 1f)
        scaleAnimator.duration = 1500
        scaleAnimator.repeatCount = ValueAnimator.INFINITE
        scaleAnimator.interpolator = AccelerateDecelerateInterpolator()
        scaleAnimator.start()

        val scaleYAnimator = ObjectAnimator.ofFloat(loadingText, "scaleY", 1f, 1.05f, 1f)
        scaleYAnimator.duration = 1500
        scaleYAnimator.repeatCount = ValueAnimator.INFINITE
        scaleYAnimator.interpolator = AccelerateDecelerateInterpolator()
        scaleYAnimator.start()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
