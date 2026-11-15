package dev.goalocus.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.goalocus.R
import dev.goalocus.core.content.JourneyOverview
import dev.goalocus.core.designsystem.theme.GoalocusTheme
import dev.goalocus.core.domain.capability.Capability
import dev.goalocus.core.domain.capability.CapabilityStatus
import dev.goalocus.core.domain.capability.UserAction
import dev.goalocus.core.domain.model.Strand
import dev.goalocus.core.domain.model.Topic
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshCapabilities()
        onPauseOrDispose { }
    }
    HomeScreen(state = state, onRetry = viewModel::retry, onUserAction = viewModel::onUserAction)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRetry: () -> Unit,
    onUserAction: (UserAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = GoalocusTheme.spacing
    Scaffold(modifier = modifier) { insets ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = spacing.md,
                end = spacing.md,
                top = insets.calculateTopPadding() + spacing.md,
                bottom = insets.calculateBottomPadding() + spacing.md,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
            }
            when (val journey = state.journey) {
                JourneyState.Loading -> item { LoadingCard() }
                is JourneyState.Failed -> item { FailedCard(journey.reason, onRetry) }
                is JourneyState.Ready -> journeySection(journey.overview)
            }
            if (state.capabilities.isNotEmpty()) {
                item { SectionTitle(R.string.home_device_title) }
                item { CapabilitiesCard(state.capabilities, onUserAction) }
            }
        }
    }
}

private fun LazyListScope.journeySection(overview: JourneyOverview) {
    item { PhaseCard(overview) }
    item { SectionTitle(R.string.home_up_next_title) }
    items(overview.upNext, key = { it.id.value }) { topic -> TopicCard(topic) }
    item {
        Text(
            text = stringResource(
                R.string.home_pack_summary,
                overview.topicCount,
                overview.questionCount,
                overview.packVersion,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PhaseCard(overview: JourneyOverview) {
    val phase = overview.phase
    val start = overview.journeyStart
    val text = when {
        phase != null -> stringResource(R.string.home_phase_current, phase.name, formatDate(phase.end))
        start != null && overview.today.isBefore(start) -> {
            val days = ChronoUnit.DAYS.between(overview.today, start).toInt()
            pluralStringResource(R.plurals.home_journey_starts_in, days, formatDate(start), days)
        }
        else -> stringResource(R.string.home_journey_done)
    }
    Card(Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(GoalocusTheme.spacing.md))
    }
}

@Composable
private fun TopicCard(topic: Topic) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(GoalocusTheme.spacing.md)) {
            Text(topic.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    R.string.home_topic_meta,
                    stringResource(topic.strand.label),
                    topic.estimatedMinutes.core,
                    topic.interviewWeight,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CapabilitiesCard(items: List<CapabilityItem>, onUserAction: (UserAction) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = GoalocusTheme.spacing.sm)) {
            items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider()
                CapabilityRow(item, onUserAction)
            }
        }
    }
}

@Composable
private fun CapabilityRow(item: CapabilityItem, onUserAction: (UserAction) -> Unit) {
    val spacing = GoalocusTheme.spacing
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.xs),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(item.capability.label), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = statusText(item.status),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.status is CapabilityStatus.NeedsUserAction) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        val status = item.status
        if (status is CapabilityStatus.NeedsUserAction) {
            TextButton(onClick = { onUserAction(status.action) }) {
                Text(stringResource(R.string.capability_fix))
            }
        }
    }
}

@Composable
private fun statusText(status: CapabilityStatus): String = when (status) {
    CapabilityStatus.Ready -> stringResource(R.string.capability_ready)
    is CapabilityStatus.NeedsUserAction -> stringResource(status.action.label)
    is CapabilityStatus.Unsupported -> status.reason
    CapabilityStatus.NotBuiltYet -> stringResource(R.string.capability_not_built)
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun LoadingCard() {
    Box(Modifier.fillMaxWidth().padding(GoalocusTheme.spacing.xl), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FailedCard(reason: String, onRetry: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(GoalocusTheme.spacing.md)) {
            Text(stringResource(R.string.home_load_failed), style = MaterialTheme.typography.titleMedium)
            Text(reason, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onRetry, modifier = Modifier.padding(top = GoalocusTheme.spacing.sm)) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatDate(date: LocalDate): String = DATE_FORMAT.format(date)

@get:StringRes
private val Strand.label: Int
    get() = when (this) {
        Strand.SYSTEMS -> R.string.strand_systems
        Strand.ALGORITHMS -> R.string.strand_algorithms
        Strand.INTELLIGENCE -> R.string.strand_intelligence
    }

@get:StringRes
private val Capability.label: Int
    get() = when (this) {
        Capability.NOTIFICATIONS -> R.string.capability_notifications
        Capability.EXACT_ALARMS -> R.string.capability_exact_alarms
        Capability.FOCUS_MODES -> R.string.capability_focus_modes
        Capability.LIVE_UPDATES -> R.string.capability_live_updates
        Capability.NFC_TRIGGER -> R.string.capability_nfc
        Capability.ACCESSIBILITY_ENGINE -> R.string.capability_accessibility
        Capability.USAGE_ACCESS -> R.string.capability_usage_access
        Capability.HEALTH_CONNECT -> R.string.capability_health_connect
        Capability.ON_DEVICE_AI -> R.string.capability_on_device_ai
        Capability.ADVANCED_PROTECTION_MONITOR -> R.string.capability_advanced_protection
    }

@get:StringRes
private val UserAction.label: Int
    get() = when (this) {
        UserAction.GRANT_NOTIFICATIONS -> R.string.action_grant_notifications
        UserAction.ALLOW_EXACT_ALARMS -> R.string.action_allow_exact_alarms
        UserAction.GRANT_DO_NOT_DISTURB_ACCESS -> R.string.action_grant_dnd
        UserAction.ALLOW_LIVE_UPDATES -> R.string.action_allow_live_updates
        UserAction.TURN_ON_NFC -> R.string.action_turn_on_nfc
    }

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val start = LocalDate.of(2026, 12, 7)
    GoalocusTheme(dynamicColor = false) {
        HomeScreen(
            state = HomeUiState(
                journey = JourneyState.Ready(
                    JourneyOverview(
                        packVersion = "preview",
                        topicCount = 7,
                        questionCount = 63,
                        today = LocalDate.of(2026, 11, 15),
                        journeyStart = start,
                        phase = null,
                        upNext = emptyList(),
                    ),
                ),
                capabilities = listOf(
                    CapabilityItem(Capability.NOTIFICATIONS, CapabilityStatus.Ready),
                    CapabilityItem(
                        Capability.EXACT_ALARMS,
                        CapabilityStatus.NeedsUserAction(UserAction.ALLOW_EXACT_ALARMS),
                    ),
                    CapabilityItem(Capability.HEALTH_CONNECT, CapabilityStatus.NotBuiltYet),
                ),
            ),
            onRetry = {},
            onUserAction = {},
        )
    }
}
