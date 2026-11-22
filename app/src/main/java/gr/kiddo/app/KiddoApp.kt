package gr.kiddo.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppState = staticCompositionLocalOf<AppState> { error("AppState missing") }

@Composable
fun KiddoApp(appState: AppState) {
    var screen by rememberSaveable { mutableStateOf(Screen.Intro) }

    // Background music plays everywhere except during the intro video and the xylophone.
    LaunchedEffect(screen, appState.musicOn) {
        BgMusic.setWanted(appState.musicOn && screen != Screen.Intro && screen != Screen.Music)
    }

    BackHandler(enabled = screen != Screen.Home && screen != Screen.Intro) {
        Speaker.stop()
        screen = Screen.Home
    }

    val goHome = {
        Speaker.stop()
        screen = Screen.Home
    }

    CompositionLocalProvider(LocalAppState provides appState) {
        KiddoTheme {
            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen"
            ) { s ->
                when (s) {
                    Screen.Intro -> IntroScreen(onDone = { screen = Screen.Home })
                    Screen.Home -> HomeScreen(onOpen = { screen = it })
                    Screen.Draw -> DrawScreen(onBack = goHome)
                    Screen.Music -> MusicScreen(onBack = goHome)
                    Screen.Quiz -> QuizScreen(onBack = goHome)
                    Screen.Puzzle -> PuzzleScreen(onBack = goHome)
                    Screen.Memory -> MemoryScreen(onBack = goHome)
                    Screen.Numbers -> NumbersScreen(onBack = goHome)
                    Screen.Owl3D -> Owl3DScreen(onBack = goHome)
                }
            }
        }
    }
}
