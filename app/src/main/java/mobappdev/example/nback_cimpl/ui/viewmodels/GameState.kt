package mobappdev.example.nback_cimpl.ui.viewmodels

data class GameState(
    val gameType: GameType = GameType.Visual,
    val eventValue: Int = -1
)