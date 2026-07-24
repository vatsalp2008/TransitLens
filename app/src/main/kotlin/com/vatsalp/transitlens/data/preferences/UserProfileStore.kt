package com.vatsalp.transitlens.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vatsalp.transitlens.core.model.ConstraintProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile")

/**
 * Persists the rider's onboarding answers and exposes them as a
 * [ConstraintProfile] the router understands.
 */
@Singleton
class UserProfileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val WHEELCHAIR = booleanPreferencesKey("wheelchair")
        val AVOID_STAIRS = booleanPreferencesKey("avoid_stairs")
        val AVOID_HILLS = booleanPreferencesKey("avoid_hills")
        val MAX_WALK = intPreferencesKey("max_walk_meters")
        val COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val profile: Flow<ConstraintProfile> = context.dataStore.data.map { prefs ->
        val avoidStairs = prefs[Keys.AVOID_STAIRS] ?: false
        val avoidHills = prefs[Keys.AVOID_HILLS] ?: false
        ConstraintProfile(
            avoidStairs = avoidStairs,
            requireElevator = avoidStairs,
            avoidHillsAboveGrade = if (avoidHills) HILL_GRADE_LIMIT else NO_HILL_RESTRICTION,
            maxWalkingMeters = prefs[Keys.MAX_WALK] ?: DEFAULT_WALK_METERS,
            wheelchairAccessible = prefs[Keys.WHEELCHAIR] ?: false,
        )
    }

    val onboardingCompleted: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.COMPLETED] ?: false }

    suspend fun setWheelchair(value: Boolean) = context.dataStore.edit { it[Keys.WHEELCHAIR] = value }
    suspend fun setAvoidStairs(value: Boolean) = context.dataStore.edit { it[Keys.AVOID_STAIRS] = value }
    suspend fun setAvoidHills(value: Boolean) = context.dataStore.edit { it[Keys.AVOID_HILLS] = value }
    suspend fun setMaxWalk(meters: Int) = context.dataStore.edit { it[Keys.MAX_WALK] = meters }
    suspend fun setOnboardingCompleted(value: Boolean) = context.dataStore.edit { it[Keys.COMPLETED] = value }

    companion object {
        const val HILL_GRADE_LIMIT = 0.05f
        const val NO_HILL_RESTRICTION = 1.0f
        const val DEFAULT_WALK_METERS = 500
        val WALK_OPTIONS = listOf(100, 250, 500, 1000)
    }
}
