package dev.goalocus.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.goalocus.core.content.GetJourneyOverview
import dev.goalocus.core.content.InvalidContentPackException
import dev.goalocus.core.content.JourneyOverview
import dev.goalocus.core.domain.capability.Capability
import dev.goalocus.core.domain.capability.CapabilityRegistry
import dev.goalocus.core.domain.capability.CapabilityStatus
import dev.goalocus.core.domain.capability.UserAction
import dev.goalocus.core.domain.capability.UserActionLauncher
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val journey: JourneyState = JourneyState.Loading,
    /** Empty until the first resume, when the screen is visible and the check is meaningful. */
    val capabilities: List<CapabilityItem> = emptyList(),
)

sealed interface JourneyState {
    data object Loading : JourneyState

    data class Ready(val overview: JourneyOverview) : JourneyState

    data class Failed(val reason: String) : JourneyState
}

data class CapabilityItem(val capability: Capability, val status: CapabilityStatus)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getJourneyOverview: GetJourneyOverview,
    private val capabilities: CapabilityRegistry,
    private val userActions: UserActionLauncher,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadJourney()
    }

    fun retry() = loadJourney()

    /** Call on every resume: the user may have just changed a permission in Settings. */
    fun refreshCapabilities() {
        val items = capabilities.snapshot().map { (capability, status) -> CapabilityItem(capability, status) }
        _state.update { it.copy(capabilities = items) }
    }

    fun onUserAction(action: UserAction) = userActions.launch(action)

    private fun loadJourney() {
        if (loadJob?.isActive == true) return
        _state.update { it.copy(journey = JourneyState.Loading) }
        loadJob = viewModelScope.launch {
            val journey = try {
                // Progress tracking arrives in M2 (learning core); until then nothing counts as learned.
                JourneyState.Ready(getJourneyOverview(learned = emptySet()))
            } catch (e: InvalidContentPackException) {
                JourneyState.Failed(e.message ?: "The content pack is invalid")
            } catch (e: IOException) {
                JourneyState.Failed("Couldn't read the content pack: ${e.message}")
            }
            _state.update { it.copy(journey = journey) }
        }
    }
}
