package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler

/**
 * This is the GameScreen composable.
 *
 * It displays the game type and presents visual or audio stimuli based on the selection.
 * The screen also provides a button to check for N-back matches.
 *
 * Date: 03-11-2024
 * Version: Version 1.0
 * Author: Gemini
 *
 */

@Composable
fun GameScreen(vm: GameViewModel, onNavigateBack: () -> Unit) {
    // Intercept the Android back button press
    BackHandler {
        vm.stopAudio()  // Stop audio playback when navigating back
        onNavigateBack()
    }

    val gameState by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()
    val feedback by vm.feedback.collectAsState()
    val currentEventNumber by vm.currentEventNumber.collectAsState()
    val correctResponses by vm.correctResponses.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Game Type: ${gameState.gameType}", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        when (gameState.gameType) {
            GameType.Visual -> VisualGameGrid(
                eventValue = gameState.eventValue,
                onTileClick = { selectedTile -> vm.checkMatch(selectedTile) },
                feedback = feedback
            )
            GameType.Audio -> {
                // Display the current audio cue
                Text("Audio cue: ${gameState.eventValue}")

                // "Check Match" button for audio mode
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    // You can pass a placeholder value like -1, as `checkMatch` won't use it in audio mode
                    vm.checkMatch(-1)
                }) {
                    Text(text = "Check Match")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Score: $score", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Event: $currentEventNumber")
        Text(text = "Correct Responses: $correctResponses")
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { vm.resetGame() }) {
            Text(text = "Reset Game")
        }
    }
}






/**
 * VisualGameGrid shows a 3x3 grid for visual stimuli.
 */
@Composable
fun VisualGameGrid(
    eventValue: Int,
    onTileClick: (Int) -> Unit,
    feedback: GameVM.FeedbackType
) {
    Column {
        for (i in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (j in 0..2) {
                    val cellNumber = i * 3 + j + 1
                    val isActiveTile = cellNumber == eventValue
                    val tileColor = if (isActiveTile) Color.Red else Color.Gray

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .padding(4.dp)
                            .background(tileColor)
                            .then(shakeAnimation(feedback)) // Apply shake effect on incorrect feedback
                            .clickable { onTileClick(cellNumber) },
                        contentAlignment = Alignment.Center
                    ) {
                        // No text display needed; the red color indicates the active tile
                    }
                }
            }
        }
    }
}


@Composable
fun shakeAnimation(feedback: GameVM.FeedbackType): Modifier {
    val transition = rememberInfiniteTransition()
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    return if (feedback == GameVM.FeedbackType.Incorrect) {
        Modifier.offset { IntOffset(offset.roundToInt(), 0) }
    } else {
        Modifier
    }
}
