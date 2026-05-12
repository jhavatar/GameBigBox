package io.chthonic.gamebigbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import gamebigbox.app.generated.resources.Res
import io.chthonic.bigbox3d.compose.BigBox3D
import io.chthonic.bigbox3d.compose.BoxRawImages
import io.chthonic.bigbox3d.compose.BoxTexture
import io.chthonic.bigbox3d.compose.BoxTextureUrls
import io.chthonic.bigbox3d.compose.CapSource
import io.chthonic.bigbox3d.compose.SideSource
import io.chthonic.bigbox3d.compose.loadRawImageFromBytes
import io.chthonic.bigbox3d.core.GlossLevel
import io.chthonic.bigbox3d.core.RotationSpeed
import io.chthonic.bigbox3d.core.ShadowFade
import io.chthonic.bigbox3d.core.ShadowOpacity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    var glossLevel by remember { mutableStateOf(GlossLevel.SEMI_GLOSS) }
    var shadowOpacity by remember { mutableStateOf(ShadowOpacity.STRONG) }
    var shadowFade by remember { mutableStateOf(ShadowFade.REALISTIC) }
    var shadowX by remember { mutableFloatStateOf(0f) }
    var shadowY by remember { mutableFloatStateOf(0f) }
    var rotationSpeed by remember { mutableStateOf(RotationSpeed.VERY_SLOW) }

    BottomSheetScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetPeekHeight = 64.dp,
        sheetContent = {
            SettingsPanel(
                glossLevel = glossLevel,
                onGlossLevelChange = { glossLevel = it },
                shadowOpacity = shadowOpacity,
                onShadowOpacityChange = { shadowOpacity = it },
                shadowFade = shadowFade,
                onShadowFadeChange = { shadowFade = it },
                shadowX = shadowX,
                onShadowXChange = { shadowX = it },
                shadowY = shadowY,
                onShadowYChange = { shadowY = it },
                rotationSpeed = rotationSpeed,
                onRotationSpeedChange = { rotationSpeed = it },
            )
        }
    ) { innerPadding ->
        var tesArena by remember { mutableStateOf<BoxRawImages?>(null) }
        LaunchedEffect(Unit) {
            tesArena = BoxRawImages(
                front  = loadRawImageFromBytes(Res.readBytes("files/TESArena_front.webp")),
                back   = loadRawImageFromBytes(Res.readBytes("files/TESArena_back.webp")),
                left   = loadRawImageFromBytes(Res.readBytes("files/TESArena_left.webp")),
                right  = loadRawImageFromBytes(Res.readBytes("files/TESArena_right.webp")),
                top    = loadRawImageFromBytes(Res.readBytes("files/TESArena_top.webp")),
                bottom = loadRawImageFromBytes(Res.readBytes("files/TESArena_bottmo.webp")),
            )
        }
        val urlBoxes = remember {
            listOf<BoxTexture>(
                BoxTextureUrls(
                    front = "https://bigboxcollection.com/images/textures/front/Doom2.webp",
                    back = "https://bigboxcollection.com/images/textures/back/Doom2.webp",
                    sides = SideSource.Explicit(
                        left = "https://bigboxcollection.com/images/textures/left/Doom2.webp",
                        right = "https://bigboxcollection.com/images/textures/right/Doom2.webp",
                    ),
                    caps = CapSource.Explicit(
                        top = "https://bigboxcollection.com/images/textures/top/Doom2.webp",
                        bottom = "https://bigboxcollection.com/images/textures/bottom/Doom2.webp",
                    ),
                ),
                BoxTextureUrls(
                    front = "https://bigboxcollection.com/images/textures/front/Doom2.webp",
                    back = "https://bigboxcollection.com/images/textures/back/Doom2.webp",
                    sides = SideSource.ColorFill(),
                    caps = CapSource.ColorFill(),
                ),
                BoxTextureUrls(
                    front = "https://bigboxcollection.com/images/textures/front/StarControl.webp",
                    back = "https://bigboxcollection.com/images/textures/back/StarControl.webp",
                    sides = SideSource.Explicit(
                        left = "https://bigboxcollection.com/images/textures/left/StarControl.webp",
                        right = "https://bigboxcollection.com/images/textures/right/StarControl.webp",
                    ),
                    caps = CapSource.ColorFill(),
                ),
                BoxTextureUrls(
                    front = "https://bigboxcollection.com/images/textures/front/StarTrekTNGFinalUnityCE.webp",
                    back = "https://bigboxcollection.com/images/textures/back/StarTrekTNGFinalUnityCE.webp",
                    sides = SideSource.Explicit(
                        left = "https://bigboxcollection.com/images/textures/left/StarTrekTNGFinalUnityCE.webp",
                        right = "https://bigboxcollection.com/images/textures/right/StarTrekTNGFinalUnityCE.webp",
                    ),
                    caps = CapSource.Explicit(
                        top = "https://bigboxcollection.com/images/textures/top/StarTrekTNGFinalUnityCE.webp",
                        bottom = "https://bigboxcollection.com/images/textures/bottom/StarTrekTNGFinalUnityCE.webp",
                    ),
                ),
                BoxTextureUrls(
                    front = "https://bigboxcollection.com/images/textures/front/SimCity2000DE.webp",
                    back = "https://bigboxcollection.com/images/textures/back/SimCity2000DE.webp",
                    sides = SideSource.Explicit(
                        left = "https://bigboxcollection.com/images/textures/left/SimCity2000DE.webp",
                        right = "https://bigboxcollection.com/images/textures/right/SimCity2000DE.webp",
                    ),
                    caps = CapSource.Explicit(
                        top = "https://bigboxcollection.com/images/textures/top/SimCity2000DE.webp",
                        bottom = "https://bigboxcollection.com/images/textures/bottom/SimCity2000DE.webp",
                    ),
                ),
                BoxTextureUrls(
                    front = "https://bigboxcollection.com/images/textures/front/SimCity2000DE.webp",
                    back = "https://bigboxcollection.com/images/textures/back/SimCity2000DE.webp",
                    sides = SideSource.ColorFill(Color.Magenta),
                    caps = CapSource.Explicit(
                        top = "https://bigboxcollection.com/images/textures/top/SimCity2000DE.webp",
                        bottom = "https://bigboxcollection.com/images/textures/bottom/SimCity2000DE.webp",
                    ),
                ),
                BoxTextureUrls(
                    front = "https://bigboxcollection.com/images/textures/front/Ultima9DragonEditionPacificAsia.webp",
                    back = "https://bigboxcollection.com/images/textures/back/Ultima9DragonEditionPacificAsia.webp",
                    sides = SideSource.Explicit(
                        left = "https://bigboxcollection.com/images/textures/left/Ultima9DragonEditionPacificAsia.webp",
                        right = "https://bigboxcollection.com/images/textures/right/Ultima9DragonEditionPacificAsia.webp",
                    ),
                    caps = CapSource.Explicit(
                        top = "https://bigboxcollection.com/images/textures/top/Ultima9DragonEditionPacificAsia.webp",
                        bottom = "https://bigboxcollection.com/images/textures/bottom/Ultima9DragonEditionPacificAsia.webp",
                    ),
                ),
                BoxTextureUrls(
                    front = "https://bigboxcollection.com/images/textures/front/Ultima9DragonEditionPacificAsia.webp",
                    back = "https://bigboxcollection.com/images/textures/back/Ultima9DragonEditionPacificAsia.webp",
                    sides = SideSource.Spine(
                        "https://bigboxcollection.com/images/textures/left/Ultima9DragonEditionPacificAsia.webp",
                    ),
                    caps = CapSource.Explicit(
                        top = "https://bigboxcollection.com/images/textures/top/Ultima9DragonEditionPacificAsia.webp",
                        bottom = "https://bigboxcollection.com/images/textures/bottom/Ultima9DragonEditionPacificAsia.webp",
                    ),
                ),
            )
        }
        val boxes = remember(tesArena) { listOfNotNull(tesArena) + urlBoxes }
        val gestureStates =
            remember(boxes.size) { mutableStateListOf(*Array(boxes.size) { false }) }
        LazyColumn(
            Modifier
                .statusBarsPadding()
                .padding(top = innerPadding.calculateTopPadding())
                .background(Color.Green)
                .fillMaxSize(),
            userScrollEnabled = !gestureStates.any { it },
        ) {
            items(
                count = boxes.size,
                // Stable keys so prepending tesArena doesn't shift URL box positions
                // and restart their LaunchedEffects.
                key = { idx -> boxes[idx].boxKey() },
            ) { idx ->
                BigBox3D(
                    modifier = Modifier
                        .height(400.dp)
                        .border(1.dp, Color.Black)
                        .fillMaxWidth(),
                    textures = boxes[idx],
                    rotationSpeed = rotationSpeed,
                    glossLevel = glossLevel,
                    shadowOpacity = shadowOpacity,
                    shadowFade = shadowFade,
                    shadowXOffsetRatio = shadowX,
                    shadowYOffsetRatio = shadowY,
                    onGestureActive = { gestureStates[idx] = it },
                )
            }
            item { Spacer(Modifier.height(300.dp)) }
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
    rotationSpeed: RotationSpeed,
    onRotationSpeedChange: (RotationSpeed) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 30.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
        )
        SettingEnum("Rotation Speed", RotationSpeed.entries.size, rotationSpeed.ordinal) {
            onRotationSpeedChange(RotationSpeed.entries[it])
        }
        SettingEnum("Gloss Level", GlossLevel.entries.size, glossLevel.ordinal) {
            onGlossLevelChange(GlossLevel.entries[it])
        }
        SettingEnum("Shadow Opacity", ShadowOpacity.entries.size, shadowOpacity.ordinal) {
            onShadowOpacityChange(ShadowOpacity.entries[it])
        }
        SettingEnum("Shadow Fade", ShadowFade.entries.size, shadowFade.ordinal) {
            onShadowFadeChange(ShadowFade.entries[it])
        }
        SettingFloat("Shadow X", -1f, 1f, shadowX, steps = 19, onSelectedChange = onShadowXChange)
        SettingFloat("Shadow Y", -1f, 1f, shadowY, steps = 19, onSelectedChange = onShadowYChange)
    }
}

@Composable
private fun SettingEnum(
    text: String,
    enumCount: Int,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit
) {
    val lastIndex = enumCount - 1
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = text)
        Slider(
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            value = selectedIndex.toFloat(),
            onValueChange = { onSelectedChange(it.roundToInt().coerceIn(0, lastIndex)) },
            valueRange = 0f..lastIndex.toFloat(),
            steps = (enumCount - 2).coerceAtLeast(0),
        )
    }
}

@Composable
private fun SettingFloat(
    text: String,
    minValue: Float,
    maxValue: Float,
    selectedValue: Float,
    steps: Int,
    onSelectedChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = text)
        Slider(
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            value = selectedValue,
            onValueChange = onSelectedChange,
            valueRange = minValue..maxValue,
            steps = steps,
        )
    }
}
