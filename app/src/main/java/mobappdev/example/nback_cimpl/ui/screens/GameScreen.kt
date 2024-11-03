package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
        Text(text = "Game Type: ${gameState.gameType}")

        when (gameState.gameType) {
            GameType.Visual -> VisualGameGrid(
                eventValue = gameState.eventValue,
                onTileClick = { selectedTile ->
                    // Call checkMatch with the selected tile
                    vm.checkMatch(selectedTile)
                }
            )
            GameType.Audio -> Text("Audio cue: ${gameState.eventValue}")
            GameType.AudioVisual -> Text("Audio-Visual mode not implemented") // Placeholder for AudioVisual case
        }

        Text(
            text = "Score: $score",
            modifier = Modifier.padding(top = 16.dp),
            color = when (feedback) {
                GameVM.FeedbackType.Correct -> Color.Green
                GameVM.FeedbackType.Incorrect -> Color.Red
                GameVM.FeedbackType.None -> Color.Black
            }
        )
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

                    // Color the tile red if it matches the current eventValue
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .padding(4.dp)
                            .background(
                                color = if (cellNumber == eventValue) Color.Red else Color.Gray
                            )
                            .clickable { onTileClick(cellNumber) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (cellNumber == eventValue) "X" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}



