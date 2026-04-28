# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GameBigBox is an Android library published to JitPack that provides a single Jetpack Compose widget — `BigBox3D` — rendering a 3D textured cuboid (a physical PC game "big box") via OpenGL ES 3.0. Touch gestures support rotation and pinch-to-zoom.

- **Language:** Kotlin 2.0.21 | **minSdk:** 26 | **compileSdk:** 36
- **UI:** Jetpack Compose + Material3; GL surface embedded via `AndroidView`
- **Rendering:** OpenGL ES 3.0 (`GLSurfaceView`)
- **Image loading:** Coil 2.6.0

## Project Structure

Both modules are KMP (Kotlin Multiplatform) with `androidTarget()` only for now. Source layout follows KMP conventions:

```
src/
  commonMain/kotlin/         ← future shared code
  androidMain/kotlin/        ← Android-specific Kotlin
  androidMain/AndroidManifest.xml
  androidMain/res/           ← Android resources (app module only)
  androidUnitTest/kotlin/    ← JVM unit tests
  androidInstrumentedTest/kotlin/  ← instrumented tests
```

## Modules

- `:app` — Demo app (`kotlin("multiplatform")` + `com.android.application`); shows multiple `BigBox3D` widgets with a live-tweaking `SettingsPanel`
- `:opengl3` — Publishable library (`kotlin("multiplatform")` + `com.android.library`); artifact `gamebigbox-opengl3.aar`, published as `com.github.jhavatar.gamebigbox:opengl3` via JitPack

## Common Commands

```bash
./gradlew :app:assembleDebug                     # build demo app
./gradlew :app:installDebug                      # install demo app on device/emulator
./gradlew :opengl3:assembleRelease               # build library AAR
./gradlew :opengl3:publishAndroidReleasePublicationToMavenLocal  # publish to local Maven
./gradlew test                                   # run unit tests
./gradlew connectedAndroidTest                   # run instrumented tests
```

JitPack release is triggered by a Git tag; `jitpack.yml` runs `:opengl3:assembleRelease :opengl3:publishToMavenLocal` with the tag as the version.

## Rendering Pipeline (`:opengl3`)

Data flows top-down through these layers:

1. **`BigBox3D.kt`** (public `@Composable`) — Accepts `BoxTextureUrls`, loads bitmaps async via Coil on `Dispatchers.IO`, builds atlas on `Dispatchers.Default`, hands result to the GL layer.

2. **Texture models** — Two sealed hierarchies:
   - `BoxTextureUrls` → `FullBoxTextureUrls` (6 face URLs) or `EquatorialBoxTextureUrls` (4 faces; top/bottom synthesized as black)
   - `BoxTextureBitmaps` — decoded `Bitmap` versions; calling `.toAtlas()` produces the atlas

3. **`BoxTextureBitmaps.buildAtlas2x3()`** — Packs all 6 face bitmaps into a single 2-col × 3-row `Bitmap` atlas and records normalized UV regions per face (`Map<RegionFace, AtlasRegion>`).

4. **`CuboidDimensions.kt`** — Derives `halfWidth/halfHeight/halfDepth` from face aspect ratios, normalized so the largest dimension is 1.0.

5. **`TexturedCuboidRenderer.kt`** (`GLSurfaceView.Renderer`) — Manages view/projection matrices, camera zoom, and rotation state (touch-driven + auto-rotate); delegates draw calls to `Cuboid`.

6. **`Cuboid.kt`** — Core GL object: builds vertex/UV/normal/index buffers for 24 vertices across 6 faces, compiles two inline GLSL ES 3.0 shader programs, uploads the atlas as a single `GL_TEXTURE_2D`, and draws all 6 faces in one `glDrawElements` call.
   - *Main shader*: Phong-style diffuse + specular lighting; `uMaterialGloss` controls shininess
   - *Shadow shader*: screen-space radial oval shadow using `smoothstep` fade

## Visual Config Enums

| Enum | Values |
|------|--------|
| `GlossLevel` | MATTE (0.0) → HIGH_GLOSS (1.0) |
| `ShadowOpacity` | NONE → FULL (0.0–1.0 alpha) |
| `ShadowFade` | SUPER_SOFT / SOFT / REALISTIC / DRAMATIC |

## Known Issues / Quirks

- Internal class `EquitorialBoxTextureBitmaps` contains a typo ("Equitorial") — the public `EquatorialBoxTextureUrls` is spelled correctly.
- Toggle `SHOW_DEBUG_OVERLAY = true` in `BigBox3D.kt` to draw red border overlays on each atlas region for UV debugging.
- The `:app` module references `:opengl3` as a local project dependency; the JitPack-published dependency line is commented out in `app/build.gradle.kts`.
