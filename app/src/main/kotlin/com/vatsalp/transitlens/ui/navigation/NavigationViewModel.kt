package com.vatsalp.transitlens.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vatsalp.transitlens.camera.FrameAnalyzer
import com.vatsalp.transitlens.core.fusion.ContextFusionEngine
import com.vatsalp.transitlens.core.model.ConstraintProfile
import com.vatsalp.transitlens.core.model.DetectedObject
import com.vatsalp.transitlens.core.model.GuidanceAction
import com.vatsalp.transitlens.core.model.NavState
import com.vatsalp.transitlens.core.model.SceneClass
import com.vatsalp.transitlens.data.preferences.UserProfileStore
import com.vatsalp.transitlens.guidance.GuidanceOrchestrator
import com.vatsalp.transitlens.ml.MlKitTextRecognizer
import com.vatsalp.transitlens.ml.TfLiteObjectDetector
import com.vatsalp.transitlens.ml.TfLiteSceneClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NavUiState(
    val scene: SceneClass = SceneClass.UNKNOWN,
    val action: GuidanceAction = GuidanceAction.CONTINUE,
    val detections: List<DetectedObject> = emptyList(),
)

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val sceneClassifier: TfLiteSceneClassifier,
    private val objectDetector: TfLiteObjectDetector,
    private val textRecognizer: MlKitTextRecognizer,
    private val fusion: ContextFusionEngine,
    private val orchestrator: GuidanceOrchestrator,
    userProfileStore: UserProfileStore,
) : ViewModel() {

    @Volatile
    private var constraints: ConstraintProfile = ConstraintProfile.NONE

    private val _ui = MutableStateFlow(NavUiState())
    val ui: StateFlow<NavUiState> = _ui.asStateFlow()

    val modelsReady: Boolean get() = sceneClassifier.isReady && objectDetector.isReady

    init {
        viewModelScope.launch {
            userProfileStore.profile.collect { constraints = it }
        }
    }

    fun createAnalyzer(): FrameAnalyzer = FrameAnalyzer(
        sceneClassifier = sceneClassifier,
        objectDetector = objectDetector,
        textRecognizer = textRecognizer,
        fusion = fusion,
        navStateProvider = { NavState() }, // routing state wired in Phase 4
        constraintsProvider = { constraints },
        onResult = { ctx, action ->
            _ui.value = NavUiState(
                scene = ctx.scene.sceneClass,
                action = action,
                detections = ctx.detectedObjects.map { it.label },
            )
            orchestrator.onAction(action)
        },
    )
}
