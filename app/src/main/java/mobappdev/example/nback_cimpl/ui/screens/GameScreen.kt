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
        Text(text = "Game Type: ${gameState.gameType}", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        when (gameState.gameType) {
            GameType.Visual -> VisualGameGrid(
                eventValue = gameState.eventValue,
                onTileClick = { selectedTile ->
                    vm.checkMatch(selectedTile) // Call checkMatch with selected tile
                }
            )
            GameType.Audio -> Text("Audio cue: ${gameState.eventValue}")
            //GameType.AudioVisual -> Text("Audio-Visual mode not implemented") // Placeholder
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display score with feedback colors
        Text(
            text = "Score: $score",
            modifier = Modifier.padding(top = 16.dp),
            color = when (feedback) {
                GameVM.FeedbackType.Correct -> Color.Green
                GameVM.FeedbackType.Incorrect -> Color.Red
                GameVM.FeedbackType.None -> Color.Black
            },
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Reset Button
        Button(onClick = { vm.resetGame() }) {
            Text(text = "Reset Game")
        }
    }
}

/**
 * VisualGameGrid shows a 3x3 grid for visual stimuli.
 */
@Composable
fun VisualGameGrid(eventValue: Int, onTileClick: (Int) -> Unit) {
    Column {
        for (i in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (j in 0..2) {
                    val cellNumber = i * 3 + j + 1

                    // Color the tile red if it matches the current eventValue, and hide if eventValue is -1
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .padding(4.dp)
                            .background(
                                color = if (cellNumber == eventValue && eventValue != -1) Color.Red else Color.Gray
                            )
                            .clickable { onTileClick(cellNumber) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (cellNumber == eventValue && eventValue != -1) "X" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}



