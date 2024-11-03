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

/**
 * This is the GameViewModel.
 *
 * It is good practice to first make an interface, which acts as the blueprint
 * for your implementation. With this interface we can create fake versions
 * of the viewmodel, which we can use to test other parts of our app that depend on the VM.
 *
 * Our viewmodel itself has functions to start a game, to specify a gametype,
 * and to check if we are having a match
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */


interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val feedback: StateFlow<GameVM.FeedbackType>  // Add feedback here
    val nBack: Int

    fun setGameType(gameType: GameType)
    fun startGame()
    fun resetGame()
    fun checkMatch(selectedTile: Int)  // Modified to accept selectedTile
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository
) : GameViewModel, ViewModel() {

    private var previousEventValue: Int = -1
    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score.asStateFlow()

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore.asStateFlow()

    private val _feedback = MutableStateFlow(FeedbackType.None)
    override val feedback: StateFlow<FeedbackType> = _feedback.asStateFlow()

    enum class FeedbackType { Correct, Incorrect, None }

    override val nBack: Int = 1

    private var job: Job? = null
    private val eventInterval: Long = 2000L
    private val nBackHelper = NBackHelper()
    private var events = emptyArray<Int>()
    private var isFirstEvent = true // Flag to prevent scoring on the first step

    override fun setGameType(gameType: GameType) {
        _gameState.value = _gameState.value.copy(gameType = gameType)
        startGame()
    }

    override fun startGame() {
        _score.value = 0
        _feedback.value = FeedbackType.None
        previousEventValue = -1
        isFirstEvent = true // Reset flag for a new game

        events = nBackHelper.generateNBackString(size = 10, combinations = 9, percentMatch = 30, nBack = nBack).toTypedArray()
        Log.d("GameVM", "New N-back sequence generated: ${events.contentToString()}")

        job?.cancel()
        job = viewModelScope.launch {
            runGameLoop(events)
        }
    }

    private suspend fun runGameLoop(events: Array<Int>) {
        for (index in events.indices) {
            _gameState.value = _gameState.value.copy(eventValue = -1)
            delay(300)

            _gameState.value = _gameState.value.copy(eventValue = events[index])
            Log.d("GameVM", "Current eventValue: ${events[index]}")
            previousEventValue = events[index]

            delay(eventInterval)
        }
    }

    override fun checkMatch(selectedTile: Int) {
        val currentEventValue = gameState.value.eventValue
        Log.d("GameVM", "Selected Tile: $selectedTile, Current Event Value: $currentEventValue, Previous Event Value: $previousEventValue")

        // Only allow scoring if this isn't the first event
        if (!isFirstEvent && selectedTile == previousEventValue && currentEventValue == previousEventValue) {
            _score.value += 1
            updateHighScore(_score.value)
            _feedback.value = FeedbackType.Correct
            Log.d("GameVM", "Correct 1-back match! Score updated to ${_score.value}")
        } else {
            _feedback.value = FeedbackType.Incorrect
            Log.d("GameVM", "Incorrect match!")
        }

        // After the first event, disable the flag
        isFirstEvent = false
        previousEventValue = currentEventValue

        viewModelScope.launch {
            delay(500)
            _feedback.value = FeedbackType.None
        }
    }

    override fun resetGame() {
        _score.value = 0
        _feedback.value = FeedbackType.None
        previousEventValue = -1
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



// Class with the different game types
enum class GameType{
    Audio,
    Visual,
    AudioVisual
}

data class GameState(
    // You can use this state to push values from the VM to your UI.
    val gameType: GameType = GameType.Visual,  // Type of the game
    val eventValue: Int = -1  // The value of the array string
)

class FakeVM: GameViewModel{
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int>
        get() = MutableStateFlow(2).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(42).asStateFlow()
    override val feedback: StateFlow<GameVM.FeedbackType>
        get() = TODO("Not yet implemented")
    override val nBack: Int
        get() = 2

    override fun setGameType(gameType: GameType) {
    }

    override fun startGame() {
    }

    override fun resetGame() {
        TODO("Not yet implemented")
    }

    override fun checkMatch(selectedTile: Int) {
        TODO("Not yet implemented")
    }
}