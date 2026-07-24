package com.vatsalp.transitlens.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vatsalp.transitlens.R
import com.vatsalp.transitlens.data.preferences.UserProfileStore
import com.vatsalp.transitlens.ui.onboarding.OnboardingViewModel

/**
 * Zero-text, icon-first onboarding. Each tile toggles a constraint, gives haptic
 * feedback, and carries a spoken prompt via its content description for TalkBack.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val title = stringResource(R.string.onboard_title)

    LaunchedEffect(Unit) { viewModel.announce(title) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            ToggleTile(
                icon = Icons.AutoMirrored.Filled.Accessible,
                label = "Wheelchair / mobility aid",
                description = stringResource(R.string.onboard_wheelchair),
                selected = profile.wheelchairAccessible,
                onClick = { viewModel.toggleWheelchair(profile.wheelchairAccessible) },
            )
            ToggleTile(
                icon = Icons.Filled.Stairs,
                label = "Avoid stairs",
                description = stringResource(R.string.onboard_stairs),
                selected = profile.avoidStairs,
                onClick = { viewModel.toggleAvoidStairs(profile.avoidStairs) },
            )
            ToggleTile(
                icon = Icons.Filled.Terrain,
                label = "Avoid steep hills",
                description = stringResource(R.string.onboard_hills),
                selected = profile.avoidHillsAboveGrade < UserProfileStore.NO_HILL_RESTRICTION,
                onClick = {
                    viewModel.toggleAvoidHills(profile.avoidHillsAboveGrade < UserProfileStore.NO_HILL_RESTRICTION)
                },
            )

            WalkDistancePicker(
                selectedMeters = profile.maxWalkingMeters,
                onSelect = { viewModel.setMaxWalk(it) },
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.complete(onDone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .semantics { contentDescription = "" },
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.onboard_done), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ToggleTile(
    icon: ImageVector,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val stateDesc = stringResource(
        if (selected) R.string.cd_selected else R.string.cd_not_selected,
    )
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .semantics {
                contentDescription = description
                stateDescription = stateDesc
            },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        ),
        border = if (selected) null else BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(20.dp))
            Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun WalkDistancePicker(
    selectedMeters: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(20.dp))
            Text(
                stringResource(R.string.onboard_walk),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UserProfileStore.WALK_OPTIONS.forEach { meters ->
                val label = if (meters >= 1000) "1 km" else "$meters m"
                val selected = selectedMeters == meters
                val mod = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .semantics { stateDescription = if (selected) "selected" else "not selected" }
                if (selected) {
                    FilledTonalButton(onClick = { onSelect(meters) }, modifier = mod) { Text(label) }
                } else {
                    OutlinedButton(onClick = { onSelect(meters) }, modifier = mod) { Text(label) }
                }
            }
        }
    }
}
