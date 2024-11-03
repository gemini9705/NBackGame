package mobappdev.example.nback_cimpl.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository

interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val feedback: StateFlow<GameVM.FeedbackType>
    val nBack: Int
    val currentEventNumber: StateFlow<Int>
    val correctResponses: StateFlow<Int>

    fun setGameType(gameType: GameType)
    fun startGame()
    fun resetGame()
    fun checkMatch(selectedTile: Int)
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score.asStateFlow()

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore.asStateFlow()

    private val _feedback = MutableStateFlow(FeedbackType.None)
    override val feedback: StateFlow<FeedbackType> = _feedback.asStateFlow()

    enum class FeedbackType { Correct, Incorrect, None }

    override val nBack: Int = 1  // Assuming 1-back; modify for other values

    private var job: Job? = null
    private val eventInterval: Long = 2000L
    private val nBackHelper = NBackHelper()
    private var events = emptyArray<Int>()
    private var eventHistory = mutableListOf<Int>()  // Track history of events

    private val _currentEventNumber = MutableStateFlow(1)
    override val currentEventNumber: StateFlow<Int> = _currentEventNumber.asStateFlow()

    private val _correctResponses = MutableStateFlow(0)
    override val correctResponses: StateFlow<Int> = _correctResponses.asStateFlow()

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
        startGame()
    }

    override fun startGame() {
        _score.value = 0
        _feedback.value = FeedbackType.None
        _correctResponses.value = 0
        _currentEventNumber.value = 1
        eventHistory.clear()

        events = nBackHelper.generateNBackString(size = 10, combinations = 9, percentMatch = 30, nBack = nBack).toTypedArray()
        Log.d("GameVM", "New N-back sequence generated: ${events.contentToString()}")

        job?.cancel()
        job = viewModelScope.launch {
            runGameLoop(events)
        }
    }

    private suspend fun runGameLoop(events: Array<Int>) {
        for ((index, event) in events.withIndex()) {
            _currentEventNumber.value = index + 1  // Update the event number
            _gameState.value = _gameState.value.copy(eventValue = -1)
            delay(300)

            // Add the new event to the event history
            eventHistory.add(event)
            if (eventHistory.size > nBack + 1) {
                eventHistory.removeAt(0)  // Keep only the last `nBack + 1` events
            }

            _gameState.value = _gameState.value.copy(eventValue = event)
            Log.d("GameVM", "Current eventValue: $event, Event history: $eventHistory")

            delay(eventInterval)
        }
        _currentEventNumber.value = 1  // Reset to 1 for a new round if needed
    }

    override fun checkMatch(selectedTile: Int) {
        val currentEventValue = gameState.value.eventValue
        Log.d("GameVM", "Selected Tile: $selectedTile, Current Event Value: $currentEventValue, Event History: $eventHistory")

        // Check if `eventHistory` has enough items to validate an n-back match
        if (eventHistory.size > nBack && selectedTile == eventHistory[eventHistory.size - (nBack + 1)]) {
            _score.value += 1
            _correctResponses.value += 1 // Update correct responses count
            updateHighScore(_score.value)
            _feedback.value = FeedbackType.Correct
            Log.d("GameVM", "Correct n-back match! Score updated to ${_score.value}")
        } else {
            _feedback.value = FeedbackType.Incorrect
            Log.d("GameVM", "Incorrect match!")
        }

        // Reset feedback after a delay for visual feedback
        viewModelScope.launch {
            delay(500)
            _feedback.value = FeedbackType.None
        }
    }


    override fun resetGame() {
        _score.value = 0
        _feedback.value = FeedbackType.None
        eventHistory.clear()
        startGame()
    }

    private fun updateHighScore(newScore: Int) {
        viewModelScope.launch {
            userPreferencesRepository.saveHighScore(newScore)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository)
            }
        }
    }

    init {
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }
}

class FakeVM: GameViewModel{
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(42).asStateFlow()
    override val feedback: StateFlow<GameVM.FeedbackType>
        get() = MutableStateFlow(GameVM.FeedbackType.None).asStateFlow()
    override val nBack: Int
        get() = 2
    override val currentEventNumber: StateFlow<Int>
        get() = MutableStateFlow(1).asStateFlow()
    override val correctResponses: StateFlow<Int>
        get() = MutableStateFlow(0).asStateFlow()

    override fun setGameType(gameType: GameType) {}
    override fun startGame() {}
    override fun resetGame() {}
    override fun checkMatch(selectedTile: Int) {}
}
