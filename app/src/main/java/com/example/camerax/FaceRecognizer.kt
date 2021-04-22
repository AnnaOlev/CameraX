package com.example.camerax

import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.lang.StringBuilder

class FaceRecognizer(override val context: Context, override val mode: String) : PhotoAnalyzer {

    override fun analyze(image: InputImage) {
        val options = FaceDetectorOptions.Builder()
            .setClassificationMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

        val detector = FaceDetection.getClient(options)

        val resString = StringBuilder()

        detector.process(image)
            .addOnSuccessListener { faces ->
                for ((counter, face) in faces.withIndex()) {
                    resString.append("Person number " + (counter + 1))
                    if (face.smilingProbability != null) {
                        if (face.smilingProbability!! > 0.7) {
                            resString.append("This person is probably smiling;")
                        }
                    }
                    if (face.rightEyeOpenProbability != null && face.leftEyeOpenProbability != null) {
                        if (face.rightEyeOpenProbability!! > 0.7) {
                            resString.append("This person has the right eye opened;")
                            if (face.leftEyeOpenProbability!! < 0.7) {
                                resString.append("This person is probably winking;")
                            }
                        }

                        if (face.leftEyeOpenProbability!! > 0.7) {
                            resString.append("This person has the left eye opened;")
                            if (face.rightEyeOpenProbability!! < 0.7) {
                                resString.append("This person is probably winking;")
                            }
                        }

                    }
                }
                showResult(resString.toString())
            }
    }
}