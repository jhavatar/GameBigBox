package io.chthonic.gamebigbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication

fun main() {
    Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
        System.err.println("Uncaught exception on ${thread.name}:")
        ex.printStackTrace()
    }
    singleWindowApplication(
        title = "GameBigBox",
        state = WindowState(size = DpSize(520.dp, 900.dp)),
    ) {
        MaterialTheme {
            MainScreen()
        }
    }
}
