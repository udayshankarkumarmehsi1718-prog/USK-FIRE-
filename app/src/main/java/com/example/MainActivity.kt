package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.engine.GameEngineViewModel
import com.example.game.engine.GameState
import com.example.game.ui.GameErrorHandler
import com.example.game.ui.GameOverScreen
import com.example.game.ui.GamePlayScreen
import com.example.game.ui.LoadingScreen
import com.example.game.ui.LobbyScreen
import com.example.game.ui.MatchmakingScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Initialize the game engine
                    val engineViewModel: GameEngineViewModel = viewModel()
                    val activeState by engineViewModel.gameState.collectAsState()
                    val profile by engineViewModel.playerProfile.collectAsState()
                    val mmPlayers by engineViewModel.matchmakingPlayers.collectAsState()
                    val mmStatus by engineViewModel.matchmakingStatus.collectAsState()
                    val systemError by engineViewModel.systemError.collectAsState()

                    GameErrorHandler(
                        systemError = systemError,
                        onReset = { engineViewModel.clearSystemError() }
                    ) {
                        // Router depending on active state
                        when (activeState) {
                            GameState.LOADING -> {
                                LoadingScreen(
                                    viewModel = engineViewModel,
                                    onLoadingComplete = {
                                        engineViewModel.completeLoading()
                                    }
                                )
                            }
                            GameState.LOBBY -> {
                                LobbyScreen(
                                    viewModel = engineViewModel,
                                    profile = profile
                                )
                            }
                            GameState.MATCHMAKING -> {
                                MatchmakingScreen(
                                    viewModel = engineViewModel,
                                    playersCount = mmPlayers,
                                    statusText = mmStatus
                                )
                            }
                            GameState.MATCH -> {
                                GamePlayScreen(viewModel = engineViewModel)
                            }
                            GameState.GAME_OVER -> {
                                GameOverScreen(viewModel = engineViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
