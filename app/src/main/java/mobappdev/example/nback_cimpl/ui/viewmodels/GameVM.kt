package mobappdev.example.nback_cimpl.ui.viewmodels

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
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
import java.util.*

interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val feedback: StateFlow<GameVM.FeedbackType>
    val nBack: Int
    val currentEventNumber: StateFlow<Int>
    val correctResponses: StateFlow<Int>
    val roundSize: Int

    fun setGameType(gameType: GameType)
    fun startGame()
    fun resetGame()
    fun checkMatch(selectedTile: Int)
    fun stopAudio()
}

class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository,
    application: Application  // Needed for accessing resources
) : AndroidViewModel(application), GameViewModel, TextToSpeech.OnInitListener {

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int> = _score.asStateFlow()

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int> = _highscore.asStateFlow()

    private val _feedback = MutableStateFlow(FeedbackType.None)
    override val feedback: StateFlow<FeedbackType> = _feedback.asStateFlow()

    override val roundSize = 20

    enum class FeedbackType { Correct, Incorrect, None }

    override val nBack: Int = 2

    private var gameLoopJob: Job? = null
    private val eventInterval: Long = 2000L
    private val nBackHelper = NBackHelper()
    private var events = emptyArray<Int>()
    private var eventHistory = mutableListOf<Int>()

    private val _currentEventNumber = MutableStateFlow(1)
    override val currentEventNumber: StateFlow<Int> = _currentEventNumber.asStateFlow()

    private val _correctResponses = MutableStateFlow(0)
    override val correctResponses: StateFlow<Int> = _correctResponses.asStateFlow()

    // Initialize TextToSpeech
    private var textToSpeech: TextToSpeech? = TextToSpeech(application, this)

    // Letters array from A to I
    private val letters = arrayOf("A", "B", "C", "D", "E", "F", "G", "H", "I")

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            Log.d("GameVM", "TextToSpeech initialized successfully.")
        } else {
            Log.e("GameVM", "TextToSpeech initialization failed.")
        }
    }

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

        events = nBackHelper.generateNBackString(
            size = roundSize,
            combinations = letters.size,
            percentMatch = 30,
            nBack = nBack
        ).toTypedArray()

        Log.d("GameVM", "New N-back sequence generated: ${events.contentToString()}")

        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            runGameLoop(events)
        }
    }

    private suspend fun runGameLoop(events: Array<Int>) {
        for ((index, event) in events.withIndex()) {
            _currentEventNumber.value = index + 1
            _gameState.value = _gameState.value.copy(eventValue = -1)
            delay(300)

            eventHistory.add(event)
            if (eventHistory.size > nBack + 1) {
                eventHistory.removeAt(0)
            }

            _gameState.value = _gameState.value.copy(eventValue = event)
            Log.d("GameVM", "Current eventValue: $event, Event history: $eventHistory")

            if (_gameState.value.gameType == GameType.Audio) {
                playAudioForEvent(event)
            }

            delay(eventInterval)
        }
        _currentEventNumber.value = 1
    }

    override fun stopAudio() {
        gameLoopJob?.cancel()
        gameLoopJob = null

        textToSpeech?.stop()
        Log.d("GameVM", "Text-to-Speech stopped.")
    }

    private fun playAudioForEvent(eventValue: Int) {
        val letter = letters.getOrNull(eventValue - 1)  // Get the letter based on the event value
        letter?.let {
            textToSpeech?.speak(it, TextToSpeech.QUEUE_FLUSH, null, null)
            Log.d("GameVM", "Speaking letter: $it")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
        textToSpeech?.shutdown()  // Release Text-to-Speech resources
    }

    override fun checkMatch(selectedTile: Int) {
        val currentEventValue = gameState.value.eventValue
        Log.d("GameVM", "Selected Tile: $selectedTile, Current Event Value: $currentEventValue, Event History: $eventHistory")

        val isMatch = if (_gameState.value.gameType == GameType.Audio) {
            eventHistory.size > nBack && eventHistory[eventHistory.size - 1] == eventHistory[eventHistory.size - (nBack + 1)]
        } else {
            eventHistory.size > nBack && selectedTile == eventHistory[eventHistory.size - (nBack + 1)]
        }

        if (isMatch) {
            _score.value += 1
            _correctResponses.value += 1
            updateHighScore(_score.value)
            _feedback.value = FeedbackType.Correct
            Log.d("GameVM", "Correct n-back match! Score updated to ${_score.value}")
        } else {
            _feedback.value = FeedbackType.Incorrect
            Log.d("GameVM", "Incorrect match!")
        }

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
                GameVM(application.userPreferencesRespository, application)
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
