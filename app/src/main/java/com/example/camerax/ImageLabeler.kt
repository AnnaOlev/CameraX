package com.example.camerax

import android.content.Context
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class ImageLabeler(override val context: Context, override val mode: String) : PhotoAnalyzer {

    private fun proceedRes(labels : List<ImageLabel>) : String {
        val textRes = StringBuilder()

        for (label in labels) {
            val text = label.text
            val confidence = label.confidence
            Log.d("HELLO ITS ME", text)
            textRes.append(text)
            if (confidence < 0.5)
                textRes.append(" really not sure!")
            if (confidence > 0.85)
                textRes.append(" for sure")
            textRes.append("; ")
        }
        return textRes.toString()
    }

    override fun analyze(image : InputImage) {

        var text: String

        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.8f)
            .build()

        val labeler = ImageLabeling.getClient(options)

        labeler.process(image)
            .addOnCompleteListener {
                text = proceedRes(it.result)
                showResult(text)
                Log.d("Test result", text)
            }
    }
}