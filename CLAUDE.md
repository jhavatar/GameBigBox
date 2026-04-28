# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GameBigBox is a Kotlin Multiplatform project that provides a `BigBox3D` Compose widget rendering a 3D textured cuboid (a physical PC game "big box") via OpenGL ES 3.0. Touch gestures support rotation and pinch-to-zoom.

- **Language:** Kotlin 2.0.21 | **minSdk:** 26 | **compileSdk:** 36
- **Build:** KMP (`kotlin("multiplatform")`) — all modules use `androidTarget()` only for now
- **UI:** Compose Multiplatform 1.7.1 + Material3; GL surface embedded via `AndroidView` on Android
- **Rendering:** OpenGL ES 3.0 (`GLSurfaceView`) on Android
- **Image loading:** Coil 3.0.4 (KMP)

## Source Layout

All modules follow KMP conventions:

```
src/
  commonMain/kotlin/         ← shared platform-agnostic code
  androidMain/kotlin/        ← Android-specific implementations
  androidMain/AndroidManifest.xml
  androidMain/res/           ← Android resources (app module only)
  androidUnitTest/kotlin/
  androidInstrumentedTest/kotlin/
```

## Modules

| Module | Type | Purpose |
|---|---|---|
| `:bigbox3d-core` | KMP library | All 3D logic — GL abstraction, geometry, atlas building, rendering |
| `:bigbox3d-compose` | KMP Compose library | `BigBox3D` Compose widget; image loading via Coil 3 |
| `:opengl3` | Android library (legacy) | Original self-contained Android implementation; still published to JitPack |
| `:app` | Android demo app | Shows multiple `BigBox3D` widgets with a live `SettingsPanel` |

## Common Commands

```bash
./gradlew :app:assembleDebug                     # build demo app
./gradlew :app:installDebug                      # install on device/emulator
./gradlew :bigbox3d-core:assembleRelease         # build core AAR
./gradlew :bigbox3d-compose:assembleRelease      # build compose AAR
./gradlew :opengl3:assembleRelease               # build legacy AAR
./gradlew test                                   # unit tests
./gradlew connectedAndroidTest                   # instrumented tests
```

## Architecture

### `:bigbox3d-core`

Pure KMP — zero platform imports in `commonMain`.

| Source set | Contents |
|---|---|
| `commonMain` | `GlApi` interface + GL constants; `RawImage` (RGBA `ByteArray`); `CuboidDimensions`; `AtlasBuilder` (pure-Kotlin nearest-neighbour scale + blit); `Matrix4` (pure-Kotlin port of `android.opengl.Matrix`); `Cuboid` (GL rendering via `GlApi`); `CuboidRenderer` (rotation/zoom state, drives `Cuboid`); visual config enums |
| `androidMain` | `GlApiImpl` — thin delegation of every `GlApi` call to `GLES30.*` |

**Key design decisions:**
- `RawImage(width, height, pixels: ByteArray)` replaces `android.graphics.Bitmap` — no platform types in common code
- `GlApi` is passed per-call (not stored in `Cuboid`) so GL context recreation is safe
- `Matrix4` is a pure-Kotlin port of `android.opengl.Matrix` — same column-major `FloatArray` API
- Atlas building scales and blits directly into the atlas buffer (no intermediate `ByteArray` per face)
- `java.nio.FloatBuffer`/`ShortBuffer` used in `commonMain` for now; will need platform abstraction when jsMain/wasmMain targets are added

### `:bigbox3d-compose`

KMP Compose widget layer. Depends on `:bigbox3d-core` via `api()` (so core types are re-exported to consumers).

| Source set | Contents |
|---|---|
| `commonMain` | `BigBox3D` composable (public API); `BoxTextureUrls` sealed interface (`FullBoxTextureUrls` / `EquatorialBoxTextureUrls`); `expect BigBox3DGlSurface`; `expect loadRawImageFromUrl` |
| `androidMain` | `actual BigBox3DGlSurface` — `GLSurfaceView` in `AndroidView`, bridges `Renderer` callbacks to `CuboidRenderer`, handles pinch/rotate gestures; `actual loadRawImageFromUrl` — Coil 3 → `BitmapImage` → ARGB→RGBA extraction; internet permission in manifest |

**Data flow in `BigBox3D`:**
1. `LaunchedEffect` loads URLs → `List<RawImage>` on `Dispatchers.IO` (capped at 1024 px via Coil)
2. Builds `BoxTextureAtlas` on `Dispatchers.Default` (`buildAtlas2x3` + `cuboidDimensions`)
3. Passes atlas to `BigBox3DGlSurface` (expect/actual); shows `CircularProgressIndicator` while loading

**Image size cap:** `loadRawImageFromUrl` requests `Size(1024, 1024)` from Coil to avoid large heap allocations — raw RGBA pixel data lives on the JVM heap unlike the native-memory `Bitmap` used in `:opengl3`.

### `:opengl3` (legacy)

Self-contained Android-only implementation using `android.graphics.Bitmap` and `GLUtils.texImage2D`. Still publishable to JitPack as `com.github.jhavatar.gamebigbox:opengl3`. Not used by `:app` any more.

## Visual Config Enums (in `:bigbox3d-core`)

| Enum | Values |
|---|---|
| `GlossLevel` | MATTE (0.0) → HIGH_GLOSS (1.0) |
| `ShadowOpacity` | NONE → FULL (0.0–1.0 alpha) |
| `ShadowFade` | SUPER_SOFT / SOFT / REALISTIC / DRAMATIC |

## Adding a New Platform Target

To add desktop/JS/Wasm support, two files are needed per platform:

1. `bigbox3d-core/src/<platform>Main/…/GlApiImpl.kt` — `actual class GlApiImpl` delegating to the platform GL API (LWJGL3 for desktop, WebGL2 for JS/Wasm)
2. `bigbox3d-compose/src/<platform>Main/…/BigBox3DGlSurface.<platform>.kt` — `actual fun BigBox3DGlSurface` embedding a GL surface in the platform's Compose interop
3. `bigbox3d-compose/src/<platform>Main/…/ImageLoading.<platform>.kt` — `actual fun loadRawImageFromUrl` using the platform image loader

## Known Issues / Quirks

- `expect @Composable fun BigBox3DGlSurface` triggers an IDE warning ("has no corresponding expected declaration") — this is a false positive caused by the Compose compiler transforming `@Composable` signatures at the IR level. The build succeeds; the warning disappears when additional platform targets are added.
- The `expect`/`actual` warning "declared in the same module" fires because `:bigbox3d-compose` currently has only one target (`androidTarget`). It resolves naturally when more targets are added.
- Internal class `EquitorialBoxTextureBitmaps` in `:opengl3` contains a typo ("Equitorial") — the public `EquatorialBoxTextureUrls` is spelled correctly.
