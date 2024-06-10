package com.aj.recon.ocr


import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

interface OCRResultListener {
    fun onOCRSuccess(text: String)
    fun onOCRFailure(exception: Exception)
}

class OCRrecon {

    fun getImage(bitmap: Bitmap, listener: OCRResultListener) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val recognizedText = StringBuilder()

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        recognizedText.append(line.text).append("\n")
                    }
                }

                // Notify listener of success with the recognized text
                listener.onOCRSuccess(recognizedText.toString())
            }
            .addOnFailureListener { e ->
                // Notify listener of failure with the exception
                listener.onOCRFailure(e)
            }
    }
}
