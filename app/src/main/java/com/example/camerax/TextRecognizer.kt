package com.example.camerax

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition

class TextRecognizer(override val context: Context, override val mode: String) : PhotoAnalyzer {

    override fun analyze(image: InputImage) {
        val recognizer = TextRecognition.getClient()
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                var finalString = ""
                for (block in visionText.textBlocks) {

                    for (line in block.lines) {

                        for (element in line.elements) {
                            finalString += " "
                            finalString += element.text
                        }
                    }
                    Log.d("HELLO ITS ME", finalString)
                }
                showResult(finalString)
            }
    }
}