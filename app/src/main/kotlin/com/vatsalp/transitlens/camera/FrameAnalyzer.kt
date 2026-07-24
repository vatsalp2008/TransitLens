package com.vatsalp.transitlens.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.vatsalp.transitlens.core.fusion.ContextFusionEngine
import com.vatsalp.transitlens.core.model.ActionContext
import com.vatsalp.transitlens.core.model.ConstraintProfile
import com.vatsalp.transitlens.core.model.GuidanceAction
import com.vatsalp.transitlens.core.model.NavState
import com.vatsalp.transitlens.core.model.TransitTextContext
import com.vatsalp.transitlens.ml.ImageUtils
import com.vatsalp.transitlens.ml.MlKitTextRecognizer
import com.vatsalp.transitlens.ml.TfLiteObjectDetector
import com.vatsalp.transitlens.ml.TfLiteSceneClassifier

/**
 * CameraX analyzer that runs the on-device pipeline per (throttled) frame:
 * scene + objects (+ async OCR) -> [ContextFusionEngine] -> [GuidanceAction].
 * Runs on a background executor; [onResult] must be thread-safe.
 */
class FrameAnalyzer(
    private val sceneClassifier: TfLiteSceneClassifier,
    private val objectDetector: TfLiteObjectDetector,
    private val textRecognizer: MlKitTextRecognizer,
    private val fusion: ContextFusionEngine,
    private val navStateProvider: () -> NavState,
    private val constraintsProvider: () -> ConstraintProfile,
    private val onResult: (ActionContext, GuidanceAction) -> Unit,
) : ImageAnalysis.Analyzer {

    @Volatile
    private var latestText: TransitTextContext = TransitTextContext.EMPTY
    private var lastRunMs = 0L

    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastRunMs < MIN_INTERVAL_MS) {
            image.close()
            return
        }
        lastRunMs = now
        try {
            val bitmap = ImageUtils.imageProxyToBitmap(image)
            val scene = sceneClassifier.classify(bitmap)
            val objects = objectDetector.detect(bitmap)
            textRecognizer.recognize(bitmap) { latestText = it }
            val context = fusion.fuse(
                scene = scene,
                objects = objects,
                text = latestText,
                navState = navStateProvider(),
                constraints = constraintsProvider(),
                timestamp = now,
            )
            onResult(context, fusion.deriveAction(context))
        } catch (t: Throwable) {
            // Per-frame best effort: drop this frame on any decode/inference error.
        } finally {
            image.close()
        }
    }

    companion object {
        private const val MIN_INTERVAL_MS = 500L
    }
}
