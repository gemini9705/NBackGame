package mobappdev.example.nback_cimpl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import mobappdev.example.nback_cimpl.ui.screens.GameScreen
import mobappdev.example.nback_cimpl.ui.screens.HomeScreen
import mobappdev.example.nback_cimpl.ui.theme.NBack_CImplTheme
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM

enum class ViewState { HOME, GAME }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NBack_CImplTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val gameViewModel: GameVM = viewModel(factory = GameVM.Factory)
                    var currentViewState by remember { mutableStateOf(ViewState.HOME) }

                    // Navigate to GameScreen
                    fun navigateToGameScreen() {
                        currentViewState = ViewState.GAME
                    }

                    // Navigate back to HomeScreen
                    fun navigateToHomeScreen() {
                        currentViewState = ViewState.HOME
                    }

                    when (currentViewState) {
                        ViewState.HOME -> {
                            HomeScreen(vm = gameViewModel, onNavigateToGameScreen = {
                                navigateToGameScreen()
                            })
                        }
                        ViewState.GAME -> {
                            GameScreen(vm = gameViewModel, onNavigateBack = {
                                navigateToHomeScreen()
                            })
                        }
                    }
                }
            }
        }
    }
}
