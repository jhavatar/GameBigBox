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
import io.chthonic.gamebigbox.opengl3.BigBoxFromUrls
import io.chthonic.gamebigbox.opengl3.FullBoxTextureUrls
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

                        BigBoxFromUrls(
                            FullBoxTextureUrls(
                                front = "https://bigboxcollection.com/images/textures/front/Doom2.webp",
                                back = "https://bigboxcollection.com/images/textures/back/Doom2.webp",
                                top = "https://bigboxcollection.com/images/textures/top/Doom2.webp",
                                bottom = "https://bigboxcollection.com/images/textures/bottom/Doom2.webp",
                                left = "https://bigboxcollection.com/images/textures/left/Doom2.webp",
                                right = "https://bigboxcollection.com/images/textures/right/Doom2.webp",
                            )
//                            FullBoxTextureUrls(
//                                front = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
//                                back = "https://rickandmortyapi.com/api/character/avatar/2.jpeg",
//                                top = "https://rickandmortyapi.com/api/character/avatar/3.jpeg",
//                                bottom = "https://rickandmortyapi.com/api/character/avatar/4.jpeg",
//                                left = "https://rickandmortyapi.com/api/character/avatar/5.jpeg",
//                                right = "https://rickandmortyapi.com/api/character/avatar/6.jpeg"
//                            )
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