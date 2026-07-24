package com.vatsalp.transitlens.ml

import android.content.Context
import android.graphics.Bitmap
import com.vatsalp.transitlens.core.model.BoundingBox
import com.vatsalp.transitlens.core.model.DetectedObject
import com.vatsalp.transitlens.core.model.Detection
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Wraps the YOLOv8n transit object detector. YOLO output is [1, 4+numClasses,
 * anchors]; this decodes it (score filter + NMS) into :core [Detection]s. The
 * safety threshold from ADR-006 is applied later in ContextFusionEngine, so this
 * only pre-filters weak candidates.
 */
@Singleton
class TfLiteObjectDetector @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val interpreter: Interpreter? = runCatching {
        Interpreter(
            ImageUtils.loadModelFile(context, ASSET),
            Interpreter.Options().apply { numThreads = 4 },
        )
    }.getOrNull()

    private val inputSize: Int = interpreter?.getInputTensor(0)?.shape()?.getOrNull(1) ?: 160

    val isReady: Boolean get() = interpreter != null

    fun detect(bitmap: Bitmap): List<Detection> {
        val model = interpreter ?: return emptyList()
        val outShape = model.getOutputTensor(0).shape() // [1, rows, anchors]
        val rows = outShape[1]
        val anchors = outShape[2]
        val numClasses = rows - 4
        if (numClasses <= 0) return emptyList()

        val input = ImageUtils.toFloatBuffer(bitmap, inputSize, normalize01 = true)
        val output = Array(1) { Array(rows) { FloatArray(anchors) } }
        model.run(input, output)
        return decode(output[0], numClasses, anchors)
    }

    private fun decode(out: Array<FloatArray>, numClasses: Int, anchors: Int): List<Detection> {
        val candidates = ArrayList<Detection>()
        val classes = minOf(numClasses, LABELS.size)
        for (a in 0 until anchors) {
            var bestClass = 0
            var bestScore = 0f
            for (c in 0 until classes) {
                val s = out[4 + c][a]
                if (s > bestScore) {
                    bestScore = s
                    bestClass = c
                }
            }
            if (bestScore < CANDIDATE_THRESHOLD) continue
            val cx = out[0][a] / inputSize
            val cy = out[1][a] / inputSize
            val w = out[2][a] / inputSize
            val h = out[3][a] / inputSize
            candidates.add(
                Detection(
                    label = LABELS[bestClass],
                    confidence = bestScore,
                    box = BoundingBox(
                        left = (cx - w / 2).coerceIn(0f, 1f),
                        top = (cy - h / 2).coerceIn(0f, 1f),
                        right = (cx + w / 2).coerceIn(0f, 1f),
                        bottom = (cy + h / 2).coerceIn(0f, 1f),
                    ),
                ),
            )
        }
        return nonMaxSuppression(candidates)
    }

    private fun nonMaxSuppression(dets: List<Detection>): List<Detection> {
        val sorted = dets.sortedByDescending { it.confidence }.toMutableList()
        val kept = ArrayList<Detection>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            kept.add(best)
            val bestBox = best.box ?: continue
            sorted.removeAll { it.label == best.label && it.box != null && iou(it.box!!, bestBox) > IOU_THRESHOLD }
        }
        return kept
    }

    private fun iou(a: BoundingBox, b: BoundingBox): Float {
        val x1 = max(a.left, b.left)
        val y1 = max(a.top, b.top)
        val x2 = min(a.right, b.right)
        val y2 = min(a.bottom, b.bottom)
        val inter = max(0f, x2 - x1) * max(0f, y2 - y1)
        val union = (a.right - a.left) * (a.bottom - a.top) + (b.right - b.left) * (b.bottom - b.top) - inter
        return if (union <= 0f) 0f else inter / union
    }

    companion object {
        private const val ASSET = "models/object_detector.tflite"
        private const val CANDIDATE_THRESHOLD = 0.25f
        private const val IOU_THRESHOLD = 0.45f

        // Index order must match the detector's training class order (ml_training).
        private val LABELS = listOf(
            DetectedObject.BUS,
            DetectedObject.TRAIN_CAR,
            DetectedObject.ELEVATOR_DOOR,
            DetectedObject.ESCALATOR,
            DetectedObject.CROSSWALK_MARKING,
            DetectedObject.WHEELCHAIR_RAMP,
            DetectedObject.TACTILE_PAVING,
            DetectedObject.ACCESSIBILITY_SIGN,
        )
    }
}
