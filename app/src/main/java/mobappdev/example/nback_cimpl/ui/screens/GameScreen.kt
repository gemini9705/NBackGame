package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
fun GameScreen(vm: GameViewModel) {
    // Observing the game state, score, and feedback from the ViewModel
    val gameState by vm.gameState.collectAsState()
    val score by vm.score.collectAsState()
    val feedback by vm.feedback.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Displaying the current game type
        Text(text = "Game Type: ${gameState.gameType}")

        // Display the appropriate UI based on the selected game type
        when (gameState.gameType) {
            GameType.Visual -> VisualGameGrid(eventValue = gameState.eventValue)
            GameType.Audio -> {
                // Placeholder for audio cue
                Text("Audio cue: ${gameState.eventValue}")
            }
            else -> Text("Dual N-back (not implemented for basic requirement)")
        }

        // Feedback-based styling for score
        Text(
            text = "Score: $score",
            modifier = Modifier.padding(top = 16.dp),
            color = when (feedback) {
                GameVM.FeedbackType.Correct -> Color.Green
                GameVM.FeedbackType.Incorrect -> Color.Red
                GameVM.FeedbackType.None -> Color.Black
            }
        )

        // Button to check for an N-back match
        Button(onClick = vm::checkMatch, modifier = Modifier.padding(top = 16.dp)) {
            Text("Check Match")
        }
    }
}

/**
 * VisualGameGrid shows a 3x3 grid for visual stimuli.
 */
@Composable
fun VisualGameGrid(eventValue: Int) {
    Column {
        for (i in 0..2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                for (j in 0..2) {
                    // Each cell of the grid
                    Box(
                        modifier = Modifier
                            .size(80.dp) // Set the size of each cell
                            .padding(4.dp)
                            .background(if ((i * 3 + j + 1) == eventValue) Color.Red else Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        // Display a text or an indicator for the event
                        Text(
                            text = if ((i * 3 + j + 1) == eventValue) "X" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

