package io.chthonic.gamebigbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.chthonic.gamebigbox.opengl3.BigBoxCubeFromUrls
import io.chthonic.gamebigbox.ui.theme.GameBigBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameBigBoxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box {
                        Greeting(
                            name = "Android",
                            modifier = Modifier.padding(innerPadding),
                        )

                        BigBoxCubeFromUrls(
                            listOf(
                                "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
                                "https://rickandmortyapi.com/api/character/avatar/2.jpeg",
                                "https://rickandmortyapi.com/api/character/avatar/3.jpeg",
                                "https://rickandmortyapi.com/api/character/avatar/4.jpeg",
                                "https://rickandmortyapi.com/api/character/avatar/5.jpeg",
                                "https://rickandmortyapi.com/api/character/avatar/6.jpeg"
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GameBigBoxTheme {
        Greeting("Android")
    }
}