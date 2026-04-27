package com.sailapp.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sailapp.data.db.entities.Trip
import com.sailapp.data.repository.TripRepository
import com.sailapp.data.repository.TripStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val trip: Trip? = null,
    val stats: TripStats? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    fun loadTrip(tripId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val trip = repository.getTripById(tripId)
            val stats = repository.getTripStats(tripId)
            _uiState.update { it.copy(trip = trip, stats = stats, isLoading = false) }
        }
    }
}
