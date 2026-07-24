package com.vatsalp.transitlens.ml

import android.content.Context
import android.graphics.Bitmap
import com.vatsalp.transitlens.core.model.SceneClass
import com.vatsalp.transitlens.core.model.SceneClassification
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import javax.inject.Inject
import javax.inject.Singleton

/** Wraps the MobileNetV3-Small scene classifier TFLite model (see :core SceneClass). */
@Singleton
class TfLiteSceneClassifier @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val interpreter: Interpreter? = runCatching {
        Interpreter(
            ImageUtils.loadModelFile(context, ASSET),
            Interpreter.Options().apply { numThreads = 4 },
        )
    }.getOrNull()

    private val inputSize: Int = interpreter?.getInputTensor(0)?.shape()?.getOrNull(1) ?: 224

    val isReady: Boolean get() = interpreter != null

    fun classify(bitmap: Bitmap): SceneClassification {
        val model = interpreter ?: return SceneClassification(SceneClass.UNKNOWN, 0f)
        val input = ImageUtils.toFloatBuffer(bitmap, inputSize, normalize01 = false)
        val output = Array(1) { FloatArray(SceneClass.entries.size) }
        model.run(input, output)
        val scores = output[0]
        var best = 0
        for (i in scores.indices) if (scores[i] > scores[best]) best = i
        return SceneClassification(SceneClass.fromIndex(best), scores[best])
    }

    companion object {
        private const val ASSET = "models/scene_classifier_fp16.tflite"
    }
}
