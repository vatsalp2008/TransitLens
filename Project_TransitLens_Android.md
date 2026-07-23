# TransitLens — Multimodal Transit Assistant for Accessible Urban Navigation
## Complete Project Specification & Build Prompt

---

## PROJECT IDENTITY

**Name:** TransitLens  
**Tagline:** On-device multimodal ML assistant that turns complex urban transit into audio and haptic guidance for people with visual, cognitive, and language barriers  
**GitHub Repo:** `github.com/vatsalp2008/TransitLens`  
**Timeline:** 8–10 weeks  
**Stack Headline:** Kotlin · Jetpack Compose · CameraX · TensorFlow Lite · ML Kit · GTFS-RT · Android Accessibility Services  
**Platform:** Android 10+ (API 29+)  
**Target Device:** Pixel 6 / Samsung Galaxy S21 and above (on-device ML inference)

---

## WHAT THIS PROJECT IS

TransitLens is a production-grade Android application that uses on-device multimodal machine learning to make urban transit accessible to people with visual impairments, cognitive disabilities, and language barriers. It identifies transit scenes in real time through the camera, reads and interprets signage without requiring the user to read text, computes accessibility-aware routes respecting physical constraints, and delivers all guidance through a carefully designed audio and haptic language — zero text required.

This project mirrors the intersection of three major engineering domains that Google, Samsung, Qualcomm, and civic tech organizations actively hire for: on-device ML deployment (TensorFlow Lite), Android system-level accessibility integration (TalkBack, Accessibility Services), and real-time computer vision on constrained hardware. It is designed to be the most socially impactful and technically differentiated project in a mobile + ML portfolio.

---

## PROBLEM STATEMENT

Over 2.2 billion people globally have a vision impairment. In Seattle alone, Hopelink serves thousands of people with disabilities who rely on public transit but find it too complex to navigate independently. The engineering failures are specific:

1. **Interface assumes literacy** — every transit app requires reading route numbers, stop names, and map labels
2. **No physical constraint awareness** — Google Maps will route a wheelchair user up a steep hill or through a station without elevator access
3. **No environmental grounding** — apps tell you what to do but not what you are currently looking at
4. **Screen dependency** — visually impaired users cannot look at a screen while navigating a physical environment simultaneously
5. **Cognitive overload** — simultaneous voice directions, map rendering, and arrival countdowns overwhelm users with cognitive disabilities

TransitLens solves all five through a camera-first, text-free, haptic-guided interface backed by on-device ML that requires no internet connection for core functionality.

---

## ARCHITECTURE OVERVIEW

```
[Camera Feed — CameraX]
        │
        ▼
[Scene Understanding Pipeline — On Device]
  ├── Scene Classifier (TFLite)
  │     Classifies: bus_stop, train_platform, street_corner,
  │                 vehicle_interior, transfer_hub, unknown
  │     Model: MobileNetV3-Small fine-tuned on transit scenes
  │     Latency target: <50ms on Pixel 6
  │
  ├── Text Recognition (ML Kit)
  │     Detects: route numbers, stop names, arrival displays,
  │              accessibility symbols (wheelchair, elevator)
  │     Output: structured transit entities
  │
  └── Object Detection (TFLite — MobileNet SSD)
        Detects: bus, train, elevator_door, escalator,
                 crosswalk, pedestrian, wheelchair_ramp
        Confidence threshold: 0.65
        
        │
        ▼
[Context Fusion Engine]
  Combines: scene class + detected text + detected objects
            + user's current navigation state + physical constraints
  Output: ActionContext (what is happening + what to do next)
        │
        ▼
[Accessibility-Aware Router]
  Input: origin (GPS) + destination + user constraint profile
  Graph: GTFS stops + OpenStreetMap pedestrian network
  Constraints: avoid_stairs, avoid_hills (>5% grade),
               require_elevator, max_walk_meters, wheelchair_accessible
  Algorithm: modified Dijkstra with constraint-weighted edges
  Output: NavigationPlan (ordered steps with landmark anchors)
        │
        ▼
[Guidance Output Layer]
  ├── Haptic Engine
  │     Custom vibration patterns per action type
  │     (turn left, board vehicle, wait, arrived)
  │
  ├── Audio Engine (TTS + pre-recorded landmarks)
  │     "The 49 bus is arriving. It is accessible."
  │     "Walk toward the yellow tactile strip ahead."
  │
  └── Visual Overlay (for sighted companions / low vision)
        Minimal AR overlay: colored directional arrow +
        large-text next action card
        
        │
        ▼
[GTFS-RT Integration]
  Real-time arrival data: King County Metro GTFS-RT feed
  Updates: every 30 seconds
  Offline fallback: scheduled arrivals from cached GTFS static
```

---

## TECH STACK — FULL SPECIFICATION

### Android Core
| Component | Technology | Why |
|---|---|---|
| Language | Kotlin 1.9 | Modern Android standard; coroutines for async |
| UI framework | Jetpack Compose | Declarative UI; accessibility-first component model |
| Camera | CameraX | Lifecycle-aware; abstracts camera2 complexity |
| Navigation | Navigation Component | Type-safe navigation graph |
| Dependency injection | Hilt | Standard Android DI |
| Async | Kotlin Coroutines + Flow | Reactive streams for camera + sensor data |
| Local storage | Room + DataStore | User profiles, cached GTFS, preference storage |

### On-Device ML
| Component | Technology | Why |
|---|---|---|
| Scene classification | TensorFlow Lite (MobileNetV3-Small) | <50ms inference; 4MB model size; runs offline |
| Object detection | TFLite MobileNet SSD | Real-time 30fps detection on mid-range hardware |
| Text recognition | ML Kit Text Recognition v2 | On-device, no network required; handles rotated text |
| Model optimization | TFLite GPU delegate + NNAPI | Hardware-accelerated inference on supported devices |
| Training pipeline | Python + TensorFlow + transfer learning | Fine-tune on custom transit scene dataset |

### Routing & Transit Data
| Component | Technology | Why |
|---|---|---|
| Transit data | GTFS static (King County Metro) | Free, standard format, covers Seattle |
| Real-time arrivals | GTFS-RT (OneBusAway API) | Live vehicle positions + arrival predictions |
| Pedestrian network | OpenStreetMap Overpass API | Sidewalk grades, elevator locations, curb cuts |
| Routing algorithm | Custom Dijkstra (Kotlin) | Constraint-weighted graph for accessibility |
| Offline maps | Cached GTFS + OSM tiles | Core functionality works without internet |

### Accessibility & Sensors
| Component | Technology | Why |
|---|---|---|
| Haptics | VibrationEffect + HapticFeedbackConstants | Precise pattern control for navigation cues |
| Audio | Android TTS + ExoPlayer | Voice guidance + pre-recorded landmark audio |
| Accessibility | AccessibilityService + TalkBack integration | Screen reader compatible throughout |
| Sensors | GPS + Accelerometer + Compass | Location + orientation for AR overlay |
| AR overlay | ARCore (lite) | Directional overlay without full AR complexity |

---

## ON-DEVICE ML PIPELINE — DETAILED SPEC

### Model 1: Transit Scene Classifier
```
Architecture: MobileNetV3-Small
Input: 224×224 RGB image (CameraX frame)
Output: softmax over 6 scene classes

Classes:
  0: bus_stop          (outdoor stop with signage)
  1: train_platform    (elevated or underground platform)
  2: street_corner     (intersection, crosswalk)
  3: vehicle_interior  (inside bus or train)
  4: transfer_hub      (major station, multiple lines)
  5: unknown           (confidence < 0.6 → fallback to GPS)

Training data:
  - Base: ImageNet pretrained MobileNetV3-Small
  - Fine-tune dataset: 
      * Google Open Images (bus_stop, train_station labels)
      * Mapillary Vistas (street-level transit imagery)
      * Custom captured: 200+ photos across Seattle transit stops
  - Augmentation: random crop, brightness ±30%, rotation ±15°,
                  Gaussian noise (simulate low-vision conditions)
  - Train/val/test split: 70/15/15

Target metrics:
  - Top-1 accuracy: >85% on val set
  - Inference latency: <50ms on Pixel 6 (GPU delegate)
  - Model size: <5MB (post quantization INT8)

TFLite conversion:
  converter = tf.lite.TFLiteConverter.from_saved_model(model_path)
  converter.optimizations = [tf.lite.Optimize.DEFAULT]
  converter.target_spec.supported_types = [tf.float16]
  tflite_model = converter.convert()
  # INT8 quantization for further size reduction:
  converter.representative_dataset = representative_data_gen
```

### Model 2: Transit Object Detector
```
Architecture: MobileNet SSD v2 (TFLite Model Maker fine-tuned)
Input: 320×320 RGB image
Output: [bounding boxes, class labels, confidence scores]

Detection classes:
  - bus (city bus, school bus)
  - train_car
  - elevator_door (open/closed state)
  - escalator
  - crosswalk_marking
  - wheelchair_ramp
  - tactile_paving (yellow guidance strips)
  - accessibility_sign

Training data:
  - Base: COCO pretrained MobileNet SSD
  - Fine-tune: Open Images V7 (bus, train classes)
  - Custom annotations: 500+ images of Seattle-specific
    accessibility infrastructure (labeled via LabelImg)

Target metrics:
  - mAP@0.5: >72% on val set
  - Inference latency: <35ms on Pixel 6
  - False positive rate on elevator_door: <10%
    (critical — false "elevator open" could cause fall)

Safety-critical threshold:
  elevator_door confidence threshold: 0.80 (higher than others)
  Rationale: false positive here has physical safety consequences
```

### Model 3: ML Kit Text Recognition Pipeline
```
Library: ML Kit Text Recognition v2 (on-device)
Input: CameraX ImageProxy frame
Output: List<TextBlock> with bounding boxes and text content

Post-processing pipeline (custom):
  Step 1: Filter by confidence > 0.70
  Step 2: Extract transit entities using regex patterns:
    - Route numbers: r'\b\d{1,3}[A-Z]?\b' (e.g., "49", "550", "Link")
    - Stop IDs: r'\bStop\s+\d{4,5}\b'
    - Arrival times: r'\b\d{1,2}:\d{2}\s*(AM|PM)?\b'
    - Accessibility symbols: detect wheelchair/elevator unicode glyphs
  Step 3: Deduplicate across consecutive frames (sliding window)
  Step 4: Map route number → route metadata via GTFS lookup

Output: TransitTextContext {
    route_numbers: List<String>,
    stop_id: String?,
    arrival_display: String?,
    has_accessibility_symbol: Boolean
}
```

### Context Fusion Engine
```kotlin
data class ActionContext(
    val sceneClass: SceneClass,           // From scene classifier
    val detectedObjects: List<Detection>, // From object detector
    val transitText: TransitTextContext,  // From ML Kit
    val navigationState: NavState,        // Current step in NavigationPlan
    val userConstraints: ConstraintProfile,
    val timestamp: Long
)

class ContextFusionEngine {
    fun fuse(
        scene: SceneClassification,
        objects: List<Detection>,
        text: TransitTextContext,
        navState: NavState
    ): ActionContext {
        // Rule-based fusion with confidence weighting
        // e.g., if scene=bus_stop AND text has route number matching
        //        current navigation step → HIGH confidence "correct stop"
        //
        // e.g., if scene=vehicle_interior AND object=bus detected outside
        //        → user has just boarded
        //
        // e.g., if elevator_door detected + user constraint requires_elevator
        //        → trigger elevator guidance cue
    }

    fun deriveAction(context: ActionContext): GuidanceAction {
        // Maps ActionContext → specific audio + haptic output
        // GuidanceAction: WAIT, BOARD, ALIGHT, TURN_LEFT, TURN_RIGHT,
        //                 ARRIVED, SEEK_ELEVATOR, CROSS_NOW, CROSS_WAIT
    }
}
```

---

## ACCESSIBILITY-AWARE ROUTER — DETAILED SPEC

```kotlin
data class ConstraintProfile(
    val avoidStairs: Boolean = false,
    val requireElevator: Boolean = false,
    val avoidHillsAboveGrade: Float = 1.0f,  // 1.0 = no restriction
    val maxWalkingMeters: Int = 500,
    val wheelchairAccessible: Boolean = false,
    val cognitiveSimplification: Boolean = false  // Prefer fewer transfers
)

data class TransitNode(
    val id: String,
    val type: NodeType,  // STOP, WAYPOINT, TRANSFER
    val lat: Double,
    val lon: Double,
    val hasElevator: Boolean,
    val hasWheelchairAccess: Boolean,
    val hillGrade: Float,      // OSM incline tag
    val hasAudioAnnouncement: Boolean
)

data class TransitEdge(
    val from: String,
    val to: String,
    val mode: TransitMode,  // WALK, BUS, TRAIN, TRANSFER
    val durationSeconds: Int,
    val distanceMeters: Int,
    val requiresStairs: Boolean,
    val hillGrade: Float,
    val routeId: String?
)

class AccessibilityRouter {
    fun route(
        origin: LatLng,
        destination: LatLng,
        constraints: ConstraintProfile,
        gtfsData: GTFSGraph,
        osmData: OSMPedestrianGraph
    ): NavigationPlan {
        // Modified Dijkstra:
        // Edge weight = travel_time + constraint_penalty
        //
        // constraint_penalty:
        //   if edge.requiresStairs && constraints.avoidStairs → weight = INFINITY
        //   if edge.hillGrade > constraints.avoidHillsAboveGrade → weight × 3.0
        //   if !edge.wheelchairAccessible && constraints.wheelchairAccessible → INFINITY
        //   if constraints.cognitiveSimplification → transfer_penalty × 2.0
        //
        // Returns: NavigationPlan with ordered steps, landmark anchors,
        //          estimated duration, accessibility compliance score
    }
}

data class NavigationStep(
    val action: StepAction,
    val instruction: String,           // Human-readable (for TalkBack)
    val landmarkAnchor: String,        // e.g., "the yellow tactile strip"
    val hapticPattern: HapticPattern,
    val audioFile: String?,            // Pre-recorded landmark audio
    val durationSeconds: Int,
    val distanceMeters: Int,
    val gtfsArrival: GTFSArrival?      // If boarding a vehicle
)
```

---

## HAPTIC LANGUAGE DESIGN

This is one of the most carefully designed layers — a haptic vocabulary that is learnable, distinguishable, and meaningful without any text.

```
Pattern Name      | Vibration Sequence          | Meaning
------------------|----------------------------|---------------------------
TURN_LEFT         | SHORT-SHORT-LONG (left)    | Turn left ahead
TURN_RIGHT        | SHORT-SHORT-LONG (right)   | Turn right ahead  
CONTINUE          | Single medium pulse         | Keep going straight
BOARD_NOW         | Three rapid pulses          | Vehicle arriving, board now
ALIGHT_NOW        | Two long pulses             | Your stop, alight now
WAIT              | Slow rhythmic pulse (3s)    | Stand by, wait here
ARRIVED           | Long-SHORT-LONG celebration | You have arrived
CROSSING_SAFE     | Rapid triple pulse          | Safe to cross now
ELEVATOR_AHEAD    | Double-pulse + pause ×3     | Elevator detected ahead
ALERT             | Irregular urgent pattern    | Attention needed
RECALCULATING     | Gentle single pulse         | Route updating

Implementation:
  VibrationEffect.createWaveform(
      timings  = longArrayOf(0, 100, 50, 100, 50, 300),
      amplitudes = intArrayOf(0, 200, 0, 200, 0, 255),
      repeat = -1  // -1 = no repeat
  )

Intensity calibration:
  - Outdoor: amplitude 200-255 (full power, ambient noise)
  - Indoor: amplitude 100-150 (quieter environment)
  - Auto-detect using microphone ambient level
```

---

## GTFS-RT INTEGRATION — DETAILED SPEC

```kotlin
class GTFSRealTimeService {
    // King County Metro GTFS-RT endpoint
    private val GTFS_RT_URL = "https://api.pugetsound.onebusaway.org/api/..."

    suspend fun getArrivals(stopId: String): List<ArrivalPrediction> {
        // Fetch protobuf feed, parse FeedMessage
        // Filter: tripUpdates for this stopId
        // Return: sorted by arrival_time, with accessibility flags
    }

    suspend fun getVehiclePosition(vehicleId: String): VehiclePosition? {
        // Real-time vehicle location for "bus is 2 stops away" guidance
    }

    fun cacheStaticGTFS() {
        // Download + parse GTFS static feed (routes, stops, shapes)
        // Store in Room database for offline fallback
        // Refresh weekly
    }
}

data class ArrivalPrediction(
    val routeId: String,
    val routeShortName: String,    // "49", "Link Light Rail"
    val headsign: String,          // "University District"
    val arrivalSeconds: Int,       // Seconds until arrival
    val isWheelchairAccessible: Boolean,
    val confidence: PredictionConfidence  // SCHEDULED, PREDICTED, REAL_TIME
)
```

---

## USER ONBOARDING + CONSTRAINT PROFILE

```kotlin
// First-launch onboarding: zero text, icon-based selection
data class ConstraintSetupScreen(
    // Large icon buttons, no text labels
    // User taps icons that apply to them:
    val screens: List<OnboardingScreen> = listOf(
        OnboardingScreen(
            icon = R.drawable.ic_wheelchair,
            hapticFeedback = HapticPattern.SINGLE_PULSE,
            audioPrompt = "Do you use a wheelchair or mobility aid?",
            constraintKey = "wheelchair_accessible"
        ),
        OnboardingScreen(
            icon = R.drawable.ic_elevator,
            audioPrompt = "Do you need to avoid stairs?",
            constraintKey = "avoid_stairs"
        ),
        OnboardingScreen(
            icon = R.drawable.ic_hill,
            audioPrompt = "Do you need to avoid steep hills?",
            constraintKey = "avoid_hills"
        ),
        OnboardingScreen(
            icon = R.drawable.ic_walking_distance,
            audioPrompt = "How far can you comfortably walk?",
            constraintKey = "max_walk_meters"
            // Slider: 100m / 250m / 500m / 1km
        )
    )
)
```

---

## ACCESSIBILITY COMPLIANCE

TransitLens is built accessibility-first, not accessibility-retrofitted.

```
TalkBack compatibility:
  - All Compose elements have contentDescription
  - Custom semantics for camera viewfinder
  - Navigation actions announced before execution
  - No gesture conflicts with TalkBack swipe navigation

Large text support:
  - All UI scales with system font size (sp units only, no dp for text)
  - Minimum touch target: 48×48dp (Material Design + WCAG 2.1 AA)

Color contrast:
  - All text: minimum 4.5:1 contrast ratio (WCAG AA)
  - UI never conveys meaning through color alone (always + icon/pattern)

Reduce motion:
  - Respects system "Reduce Motion" setting
  - AR overlay animations disabled when set

Switch access:
  - All interactive elements reachable via switch scanning
  - No time-limited interactions

Screen reader audio:
  - All haptic patterns have audio equivalents
  - User can disable haptics and use audio-only mode
```

---

## ENGINEERING DECISIONS TO DOCUMENT (ADRs)

1. **Why on-device ML over cloud API** — accessibility users may have limited data plans; core navigation must work offline; latency of cloud round-trip unacceptable for real-time camera guidance; privacy (camera feed never leaves device)
2. **Why MobileNetV3-Small over larger models** — <50ms inference constraint on mid-range hardware; 85% accuracy sufficient for scene classification; larger models add latency without meaningful accuracy gain on 6-class problem
3. **Why haptic-first over voice-first** — noisy urban environments make voice unreliable; haptics work in any environment; users with hearing impairments also benefit; haptics don't require earphones
4. **Why GTFS over proprietary transit API** — GTFS is the open standard used by every major transit agency; project is portable to any US city; no API key required for static data
5. **Why modified Dijkstra over existing routing libraries** — no existing library supports custom accessibility constraint weighting at the edge level; OSRM and Valhalla don't handle elevator requirements natively
6. **Safety threshold design for elevator detection** — false positive (saying elevator is open when it isn't) has physical safety consequences; document why 0.80 threshold chosen vs. standard 0.65
7. **Why Jetpack Compose over XML layouts** — accessibility semantics are first-class in Compose; cleaner TalkBack integration; modern Android standard; faster iteration

---

## RESUME BULLETS (XYZ FORMULA)

**Project Heading:**
`TransitLens — Multimodal Transit Accessibility Assistant | Kotlin · TFLite · ML Kit · CameraX · GTFS-RT · Jetpack Compose`

**Bullets:**
- Engineered an on-device multimodal ML pipeline on Android fusing a fine-tuned **MobileNetV3-Small scene classifier** (<50ms, 85%+ accuracy across 6 transit scene classes), **MobileNet SSD object detector** (mAP@0.5 >72% on transit-specific classes), and **ML Kit text recognition** — all running offline on-device with no cloud dependency.
- Built an accessibility-aware transit router implementing constraint-weighted Dijkstra on a fused GTFS + OpenStreetMap pedestrian graph, routing around stairs, steep grades, and elevator requirements — serving users whose constraints are **unsupported by Google Maps or OneBusAway**.
- Designed a **10-pattern haptic navigation language** (board, alight, turn, cross, elevator, arrived) with amplitude auto-calibrated to ambient noise level, enabling zero-screen navigation for visually impaired users in loud urban environments.
- Integrated King County Metro GTFS-RT feed with **30-second refresh** and full offline fallback to cached static schedules, ensuring core navigation functionality without network connectivity.
- Delivered full **WCAG 2.1 AA accessibility compliance** including TalkBack screen reader integration, 4.5:1 contrast ratios, switch access support, and a zero-text icon-based onboarding flow — built accessibility-first, not retrofitted.

---

## FOLDER STRUCTURE

```
TransitLens/
├── app/
│   ├── src/main/
│   │   ├── java/com/vatsalp/transitlens/
│   │   │   ├── camera/
│   │   │   │   ├── CameraManager.kt          # CameraX setup + frame analysis
│   │   │   │   └── FrameAnalyzer.kt          # Routes frames to ML pipeline
│   │   │   ├── ml/
│   │   │   │   ├── SceneClassifier.kt        # TFLite scene classification
│   │   │   │   ├── ObjectDetector.kt         # TFLite object detection
│   │   │   │   ├── TextRecognizer.kt         # ML Kit text pipeline
│   │   │   │   └── ContextFusionEngine.kt    # Fuses all ML outputs
│   │   │   ├── routing/
│   │   │   │   ├── GTFSParser.kt             # Parse GTFS static feed
│   │   │   │   ├── OSMGraph.kt               # OpenStreetMap pedestrian graph
│   │   │   │   ├── AccessibilityRouter.kt    # Constraint-weighted Dijkstra
│   │   │   │   └── NavigationPlan.kt         # Route data model
│   │   │   ├── gtfsrt/
│   │   │   │   ├── GTFSRealTimeService.kt    # GTFS-RT API client
│   │   │   │   └── ArrivalCache.kt           # Offline arrival cache
│   │   │   ├── guidance/
│   │   │   │   ├── HapticEngine.kt           # Vibration pattern library
│   │   │   │   ├── AudioEngine.kt            # TTS + landmark audio
│   │   │   │   └── GuidanceOrchestrator.kt   # Coordinates haptic + audio
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── OnboardingScreen.kt   # Zero-text constraint setup
│   │   │   │   │   ├── NavigationScreen.kt   # Main camera + guidance view
│   │   │   │   │   ├── DestinationScreen.kt  # Destination selection
│   │   │   │   │   └── ArrivalScreen.kt      # Arrived confirmation
│   │   │   │   ├── components/
│   │   │   │   │   ├── AROverlay.kt          # Minimal AR directional overlay
│   │   │   │   │   ├── ArrivalCard.kt        # Large-text arrival display
│   │   │   │   │   └── AccessibilityBadge.kt # Wheelchair/elevator indicators
│   │   │   │   └── theme/
│   │   │   │       └── TransitLensTheme.kt   # High-contrast accessible theme
│   │   │   ├── data/
│   │   │   │   ├── db/
│   │   │   │   │   ├── TransitDatabase.kt    # Room database
│   │   │   │   │   ├── StopDao.kt
│   │   │   │   │   └── RouteDao.kt
│   │   │   │   └── preferences/
│   │   │   │       └── UserProfileStore.kt   # DataStore constraint profile
│   │   │   └── di/
│   │   │       └── AppModule.kt              # Hilt dependency injection
│   │   ├── assets/
│   │   │   ├── models/
│   │   │   │   ├── scene_classifier.tflite   # Fine-tuned MobileNetV3
│   │   │   │   └── object_detector.tflite    # Fine-tuned MobileNet SSD
│   │   │   └── audio/
│   │   │       └── landmarks/                # Pre-recorded landmark audio
│   │   └── res/
│   │       └── drawable/                     # Accessibility-compliant icons
├── ml_training/
│   ├── scene_classifier/
│   │   ├── collect_data.py                   # Dataset collection scripts
│   │   ├── train.py                          # MobileNetV3 fine-tuning
│   │   ├── evaluate.py                       # Accuracy + confusion matrix
│   │   └── export_tflite.py                  # TFLite + INT8 quantization
│   └── object_detector/
│       ├── label_images.py                   # LabelImg annotation helper
│       ├── train_tflite_model_maker.py        # TFLite Model Maker training
│       └── evaluate_map.py                   # mAP evaluation
├── docs/
│   ├── ADR-001-on-device-ml.md
│   ├── ADR-002-mobilenetv3-small.md
│   ├── ADR-003-haptic-first.md
│   ├── ADR-004-gtfs-standard.md
│   ├── ADR-005-modified-dijkstra.md
│   ├── ADR-006-elevator-safety-threshold.md
│   ├── ADR-007-jetpack-compose.md
│   └── architecture.png
├── tests/
│   ├── SceneClassifierTest.kt
│   ├── AccessibilityRouterTest.kt
│   ├── ContextFusionTest.kt
│   └── HapticEngineTest.kt
├── gradle/
├── build.gradle.kts
└── README.md
```

---

## README STRUCTURE

1. **One-liner** — what TransitLens does and who it serves
2. **Problem statement** — the 2.2B stat + Hopelink context
3. **Architecture diagram** — rendered from ASCII above
4. **ML pipeline results** — scene classifier accuracy, object detector mAP, inference latency table
5. **Haptic language table** — all 10 patterns with descriptions
6. **Accessibility compliance** — WCAG 2.1 AA checklist
7. **Demo video** — 60-second screen recording: onboarding → destination set → camera guidance → arrival
8. **Quick start** — clone → open in Android Studio → run on device/emulator
9. **ADR links** — all 7 documented
10. **Acknowledgements** — Hopelink, King County Metro open data

---

## WHAT MAKES THIS SENIOR-LEVEL

A junior project wraps a pretrained model in an app. This project demonstrates:

- **On-device ML deployment** — TFLite quantization, GPU delegate, latency benchmarking shows production deployment thinking
- **Multi-model fusion** — combining three ML outputs into a coherent ActionContext is an architectural problem, not just a modeling problem
- **Safety-critical threshold design** — the elevator detection threshold ADR shows you think about failure consequences
- **Accessibility-first engineering** — WCAG compliance built in, not added on; shows you understand the difference
- **Domain expertise** — GTFS is the actual standard transit agencies use; knowing this signals genuine research
- **Haptic language design** — designing a communication system from scratch demonstrates product thinking beyond pure engineering
- **Offline-first architecture** — on-device ML + cached GTFS means core functionality works without network; shows infrastructure maturity

---

## STRETCH GOALS

1. **Crowdsourced accessibility data** — let users report broken elevators, missing curb cuts, and blocked ramps; feed back into OSM via the OSM API
2. **Multi-language audio** — serve Seattle's large non-English-speaking population (Spanish, Vietnamese, Somali, Amharic) using pre-recorded landmark audio in multiple languages
3. **Indoor navigation** — extend to inside major transit hubs (Westlake Station, SeaTac Airport) using WiFi fingerprinting for positioning where GPS fails
4. **Caregiver companion mode** — paired app for caregivers to monitor a dependent's transit journey in real time with alert triggers
5. **Wearable integration** — Wear OS companion app delivering haptic cues to a smartwatch so phone stays pocketed during navigation

---

## HOW THIS CONNECTS TO YOUR EXISTING PORTFOLIO

```
Seattle Transit Pathfinding (existing)
  └──▶ Routing algorithm reused + extended with accessibility constraints
       Graph data structures directly transferable

PerceptNet (new)
  └──▶ Perception thinking carried into mobile domain
       Same object detection concepts, different deployment target (TFLite vs TensorRT)

FORUM (existing)
  └──▶ Civic tech narrative — both projects serve underrepresented populations
       Strengthens your "technology for social good" story in interviews

The combined narrative:
"I built a perception pipeline for autonomous vehicles (PerceptNet),
then applied the same multimodal sensing principles to make urban
transit accessible to people with disabilities (TransitLens) —
same domain of real-world perception, two very different deployment contexts."
```

---

*Built by Vatsal Patel | github.com/vatsalp2008/TransitLens*
