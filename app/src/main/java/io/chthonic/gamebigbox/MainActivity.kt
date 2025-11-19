package io.chthonic.gamebigbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.chthonic.gamebigbox.opengl3.BigBox3D
import io.chthonic.gamebigbox.opengl3.FullBoxTextureUrls
import io.chthonic.gamebigbox.opengl3.GlossLevel
import io.chthonic.gamebigbox.opengl3.ShadowFade
import io.chthonic.gamebigbox.opengl3.ShadowOpacity
import io.chthonic.gamebigbox.ui.theme.GameBigBoxTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )
    val coroutineScope = rememberCoroutineScope()
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    BackHandler {
        if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
            coroutineScope.launch {
                scaffoldState.bottomSheetState.partialExpand()
            }
        } else {
            dispatcher?.onBackPressed()
        }
    }

    var glossLevel by remember { mutableStateOf(GlossLevel.SEMI_GLOSS) }
    var shadowOpacity by remember { mutableStateOf(ShadowOpacity.STRONG) }
    var shadowFade by remember { mutableStateOf(ShadowFade.REALISTIC) }
    var shadowX by remember { mutableFloatStateOf(0f) }
    var shadowY by remember { mutableFloatStateOf(0f) }
    var autoRotate by remember { mutableStateOf(true) }

    BottomSheetScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetPeekHeight = 64.dp,
        sheetContent = {
            SettingsPanel(
                glossLevel = glossLevel,
                onGlossLevelChange = { newValue -> glossLevel = newValue },
                shadowOpacity = shadowOpacity,
                onShadowOpacityChange = { newValue -> shadowOpacity = newValue },
                shadowFade = shadowFade,
                onShadowFadeChange = { newValue -> shadowFade = newValue },
                shadowX = shadowX,
                onShadowXChange = { newValue -> shadowX = newValue },
                shadowY = shadowY,
                onShadowYChange = { newValue -> shadowY = newValue },
                autoRotate = autoRotate,
                onAutoRotateChange = { newValue -> autoRotate = newValue }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .systemBarsPadding()
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Green),
                contentAlignment = Alignment.Center,
            ) {
                BigBox3D(
                    FullBoxTextureUrls(
                        front = "https://bigboxcollection.com/images/textures/front/Doom2.webp",
                        back = "https://bigboxcollection.com/images/textures/back/Doom2.webp",
                        top = "https://bigboxcollection.com/images/textures/top/Doom2.webp",
                        bottom = "https://bigboxcollection.com/images/textures/bottom/Doom2.webp",
                        left = "https://bigboxcollection.com/images/textures/left/Doom2.webp",
                        right = "https://bigboxcollection.com/images/textures/right/Doom2.webp",
                    ),
//                            FullBoxTextureUrls(
//                                front = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
//                                back = "https://rickandmortyapi.com/api/character/avatar/2.jpeg",
//                                top = "https://rickandmortyapi.com/api/character/avatar/3.jpeg",
//                                bottom = "https://rickandmortyapi.com/api/character/avatar/4.jpeg",
//                                left = "https://rickandmortyapi.com/api/character/avatar/5.jpeg",
//                                right = "https://rickandmortyapi.com/api/character/avatar/6.jpeg"
//                            )
                    autoRotate = autoRotate,
                    glossLevel = glossLevel,
                    shadowOpacity = shadowOpacity,
                    shadowFade = shadowFade,
                    shadowXOffsetRatio = shadowX,
                    shadowYOffsetRatio = shadowY,
                    modifier = Modifier,
                )
            }
        }
    }
}

@Composable
fun SettingsPanel(
    glossLevel: GlossLevel,
    onGlossLevelChange: (GlossLevel) -> Unit,
    shadowOpacity: ShadowOpacity,
    onShadowOpacityChange: (ShadowOpacity) -> Unit,
    shadowFade: ShadowFade,
    onShadowFadeChange: (ShadowFade) -> Unit,
    shadowX: Float,
    onShadowXChange: (Float) -> Unit,
    shadowY: Float,
    onShadowYChange: (Float) -> Unit,
    autoRotate: Boolean,
    onAutoRotateChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 30.dp)
    ) {
        Row {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.Top),
            )
            Spacer(Modifier.weight(1f))
            CheckBox(
                text = "Rotating",
                value = autoRotate,
                modifier = Modifier
                    .align(Alignment.Bottom),
                onValueChange = onAutoRotateChange,
            )
        }
        SettingEnum(
            "Gloss Level",
            GlossLevel.entries.size,
            glossLevel.ordinal,
        ) {
            onGlossLevelChange(GlossLevel.entries[it])
        }

        SettingEnum(
            text = "Shadow Opacity",
            enumCount = ShadowOpacity.entries.size,
            selectedIndex = shadowOpacity.ordinal,
        ) {
            onShadowOpacityChange(ShadowOpacity.entries[it])
        }

        SettingEnum(
            text = "Shadow Fade",
            enumCount = ShadowFade.entries.size,
            selectedIndex = shadowFade.ordinal,
        ) {
            onShadowFadeChange(ShadowFade.entries[it])
        }

        SettingFloat(
            text = "Shadow X",
            minValue = -1f,
            maxValue = 1f,
            selectedValue = shadowX,
            steps = 19,
        ) { onShadowXChange(it) }

        SettingFloat(
            text = "Shadow Y",
            minValue = -1f,
            maxValue = 1f,
            selectedValue = shadowY,
            steps = 19,
        ) { onShadowYChange(it) }
    }
}


@Composable
private fun CheckBox(
    text: String,
    value: Boolean,
    modifier: Modifier = Modifier,
    onValueChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(text = text)
        Checkbox(
            modifier = Modifier,
            checked = value,
            onCheckedChange = onValueChange,
        )
    }
}

@Composable
private fun SettingEnum(
    text: String,
    enumCount: Int,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        val lastIndex = enumCount - 1
        Text(text = text)
        Slider(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            value = selectedIndex.toFloat(),
            onValueChange = { value ->
                val newIndex = value.roundToInt().coerceIn(0, lastIndex)
                onSelectedChange(newIndex)
            },
            valueRange = 0f..lastIndex.toFloat(),
            steps = (enumCount - 2).coerceAtLeast(0)  // n-1 steps
        )
    }
}

@Composable
private fun SettingFloat(
    text: String,
    minValue: Float,
    maxValue: Float,
    steps: Int,
    selectedValue: Float,
    onSelectedChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = text)
        Slider(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            value = selectedValue,
            onValueChange = { value ->
                onSelectedChange(value)
            },
            valueRange = minValue..maxValue,
            steps = steps,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSettings() {
    GameBigBoxTheme {
        SettingsPanel(
            glossLevel = GlossLevel.GLOSSY,
            onGlossLevelChange = { },
            shadowOpacity = ShadowOpacity.STRONG,
            onShadowOpacityChange = { },
            shadowFade = ShadowFade.REALISTIC,
            onShadowFadeChange = { },
            shadowX = 0f,
            onShadowXChange = { },
            shadowY = 0f,
            onShadowYChange = { },
            autoRotate = true,
            onAutoRotateChange = { }
        )
    }
}