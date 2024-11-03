package mobappdev.example.nback_cimpl.ui.viewmodels

import android.app.GameState
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

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkMatch(selectedTile: Int)
    fun resetGame() // Added resetGame
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, ViewModel() {

    // Track the previous tile position for 1-back logic
    private var previousEventValue: Int = -1

    // State and feedback properties
    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score.asStateFlow()

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore.asStateFlow()

    private val _feedback = MutableStateFlow(FeedbackType.None)
    override val feedback: StateFlow<FeedbackType> = _feedback.asStateFlow()

    // Enum for feedback types
    enum class FeedbackType { Correct, Incorrect, None }

    // Hardcoded to 1-back for basic requirement
    override val nBack: Int = 1

    // Game variables
    private var job: Job? = null
    private val eventInterval: Long = 2000L
    private val nBackHelper = NBackHelper()
    private var events = emptyArray<Int>()

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        // Reset game state
        _score.value = 0
        _feedback.value = FeedbackType.None
        previousEventValue = -1

        // Generate a new sequence of events for the game
        events = nBackHelper.generateNBackString(size = 10, combinations = 9, percentMatch = 30, nBack = nBack).toTypedArray()
        Log.d("GameVM", "New N-back sequence generated: ${events.contentToString()}")

        // Start the game loop
        job?.cancel()
        job = viewModelScope.launch {
            runGameLoop(events)
        }
    }

    private suspend fun runGameLoop(events: Array<Int>) {
        for (index in events.indices) {
            _gameState.value = _gameState.value.copy(eventValue = events[index])
            Log.d("GameVM", "Current eventValue: ${events[index]}")
            delay(eventInterval)
        }
    }

    override fun checkMatch(selectedTile: Int) {
        val currentEventValue = gameState.value.eventValue
        Log.d("GameVM", "Selected Tile: $selectedTile, Current Event Value: $currentEventValue")

        // Check if selected tile matches 1-back value
        if (currentEventValue == previousEventValue && selectedTile == currentEventValue) {
            _score.value += 1
            updateHighScore(_score.value)
            _feedback.value = FeedbackType.Correct
            Log.d("GameVM", "Correct 1-back match! Score updated to ${_score.value}")
        } else {
            _feedback.value = FeedbackType.Incorrect
            Log.d("GameVM", "Incorrect match!")
        }

        // Update previousEventValue for the next check
        previousEventValue = currentEventValue

        // Clear feedback after a short delay
        viewModelScope.launch {
            delay(500)
            _feedback.value = FeedbackType.None
        }
    }

    // Reset function for resetting score, state, and restarting game
    override fun resetGame() {
        _score.value = 0
        _feedback.value = FeedbackType.None
        previousEventValue = -1
        startGame()  // Restart the game sequence
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
        // Collect highscore on ViewModel initialization
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }
}
