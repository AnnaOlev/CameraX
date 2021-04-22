package com.example.camerax

import android.content.Context
import android.content.Intent
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import java.util.*

interface PhotoAnalyzer {

    val context : Context
    val mode : String

    fun analyze(image : InputImage)

    fun showResult(textResult : String) {

        val intent = Intent(context, PopUpWindow::class.java)

        var titleString = ""

        when (mode) {
            "label" -> titleString = context.getString(R.string.labels_result)
            "face" -> titleString = context.getString(R.string.faces_results)
            "text" -> titleString == context.getString(R.string.texts_result)
        }

        if (Locale.getDefault().language != "en") {
            if (mode != "text") {
                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(TranslateLanguage.RUSSIAN)
                    .build()
                var textToShow: String
                val englishRussianTranslator = Translation.getClient(options)
                val conditions = DownloadConditions.Builder()
                    .requireWifi()
                    .build()
                englishRussianTranslator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        englishRussianTranslator.translate(textResult)
                            .addOnSuccessListener { translatedText ->
                                textToShow = translatedText
                                englishRussianTranslator.close()
                                intent.putExtra("result", textToShow)
                                intent.putExtra("popuptitle", titleString)
                                intent.putExtra("popupbtn", R.string.popup_close)
                                intent.putExtra("darkstatusbar", false)
                                context.startActivity(intent)
                            }
                            .addOnFailureListener {
                                // Error.
                                // ...
                            }
                    }
                    .addOnFailureListener {
                        // Model couldn’t be downloaded or other internal error.
                        // ...
                    }
            }
        }
        else {
            intent.putExtra("result", textResult)
            intent.putExtra("popuptitle", titleString)
            intent.putExtra("popupbtn", R.string.popup_close)
            intent.putExtra("darkstatusbar", false)
            context.startActivity(intent)
        }
    }
}