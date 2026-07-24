package com.vatsalp.transitlens.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vatsalp.transitlens.core.model.TransitTextContext
import com.vatsalp.transitlens.core.text.TransitTextExtractor
import javax.inject.Inject
import javax.inject.Singleton

/** On-device OCR via ML Kit, mapped to structured transit entities by :core. */
@Singleton
class MlKitTextRecognizer @Inject constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Asynchronous; [onResult] is called on the main thread with extracted entities. */
    fun recognize(bitmap: Bitmap, rotationDegrees: Int = 0, onResult: (TransitTextContext) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { onResult(TransitTextExtractor.extract(it.text)) }
            .addOnFailureListener { onResult(TransitTextContext.EMPTY) }
    }
}
