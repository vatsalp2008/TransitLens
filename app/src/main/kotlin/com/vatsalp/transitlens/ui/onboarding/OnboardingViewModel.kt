package com.vatsalp.transitlens.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vatsalp.transitlens.core.model.ConstraintProfile
import com.vatsalp.transitlens.core.model.HapticPattern
import com.vatsalp.transitlens.data.preferences.UserProfileStore
import com.vatsalp.transitlens.guidance.AudioEngine
import com.vatsalp.transitlens.guidance.HapticEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val store: UserProfileStore,
    private val haptics: HapticEngine,
    private val audio: AudioEngine,
) : ViewModel() {

    val profile: StateFlow<ConstraintProfile> =
        store.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConstraintProfile.NONE)

    fun announce(text: String) = audio.speak(text)

    fun toggleWheelchair(current: Boolean) = tap { store.setWheelchair(!current) }

    fun toggleAvoidStairs(current: Boolean) = tap { store.setAvoidStairs(!current) }

    fun toggleAvoidHills(current: Boolean) = tap { store.setAvoidHills(!current) }

    fun setMaxWalk(meters: Int) = tap { store.setMaxWalk(meters) }

    fun complete(onDone: () -> Unit) {
        haptics.play(HapticPattern.ARRIVED)
        viewModelScope.launch {
            store.setOnboardingCompleted(true)
            onDone()
        }
    }

    private inline fun tap(crossinline write: suspend () -> Unit) {
        haptics.play(HapticPattern.SINGLE_PULSE)
        viewModelScope.launch { write() }
    }
}
