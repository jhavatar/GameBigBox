package io.chthonic.gamebigbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.chthonic.bigbox3d.core.GlossLevel
import io.chthonic.bigbox3d.core.RotationSpeed
import io.chthonic.bigbox3d.core.ShadowFade
import io.chthonic.bigbox3d.core.ShadowOpacity
import io.chthonic.gamebigbox.ui.theme.GameBigBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameBigBoxTheme {
                MainScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettings() {
    GameBigBoxTheme {
        SettingsPanel(
            glossLevel            = GlossLevel.GLOSSY,
            onGlossLevelChange    = {},
            shadowOpacity         = ShadowOpacity.STRONG,
            onShadowOpacityChange = {},
            shadowFade            = ShadowFade.REALISTIC,
            onShadowFadeChange    = {},
            shadowX               = 0f,
            onShadowXChange       = {},
            shadowY               = 0f,
            onShadowYChange       = {},
            rotationSpeed         = RotationSpeed.VERY_SLOW,
            onRotationSpeedChange = {},
        )
    }
}
