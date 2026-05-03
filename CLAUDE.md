# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GameBigBox is a Kotlin Multiplatform project that provides a `BigBox3D` Compose widget rendering a 3D textured cuboid (a physical PC game "big box") via OpenGL ES 3.0 on Android and WebGL2 on web. Touch/mouse gestures support rotation and scroll/pinch-to-zoom.

- **Language:** Kotlin 2.0.21 | **minSdk:** 26 | **compileSdk:** 36
- **Build:** KMP (`kotlin("multiplatform")`) — `androidTarget()` + `wasmJs { browser() }` on library/app modules; `jvm()` on `:bigbox3d-core`, `:bigbox3d-compose`, and `:app`
- **UI:** Compose Multiplatform 1.7.1 + Material3; GL surface embedded via `AndroidView` on Android, DOM canvas overlay on web
- **Rendering:** OpenGL ES 3.0 (`GLSurfaceView`) on Android; WebGL2 (`OffscreenCanvas` / `HTMLCanvasElement`) on web. All rendering goes through the platform-agnostic `GlApi` interface using VBOs.
- **Image loading:** Coil 3.0.4 on Android (OkHttp network fetcher); browser `fetch` → `createImageBitmap` → `OffscreenCanvas` on web

## Source Layout

All modules follow KMP conventions:

```
src/
  commonMain/kotlin/         ← shared platform-agnostic code
  androidMain/kotlin/        ← Android-specific implementations
  androidMain/AndroidManifest.xml
  androidMain/res/           ← Android resources (app module only)
  wasmJsMain/kotlin/         ← Web (Kotlin/Wasm) implementations
  wasmJsMain/resources/      ← Web resources (index.html in app module)
  androidUnitTest/kotlin/
  androidInstrumentedTest/kotlin/
```

## Modules

| Module | Type | Purpose |
|---|---|---|
| `:bigbox3d-core` | KMP library (Android + wasmJs + JVM) | All 3D logic — GL abstraction, geometry, atlas building, rendering |
| `:bigbox3d-compose` | KMP Compose library (Android + wasmJs + JVM) | `BigBox3D` Compose widget; image loading; platform GL surface |
| `:opengl3` | Android library (legacy) | Original self-contained Android implementation; still published to JitPack |
| `:app` | KMP app (Android + wasmJs + JVM) | Demo app showing multiple `BigBox3D` widgets with a live `SettingsPanel` |

## Common Commands

### Launch the demo app

| Target | Command | Notes |
|---|---|---|
| **Android** | `./gradlew :app:installDebug` then `adb shell am start -n io.chthonic.gamebigbox/.MainActivity` | Requires a connected device or running emulator |
| **Web** | `./gradlew :app:wasmJsBrowserDevelopmentRun` | Starts a dev server and opens `http://localhost:8080` automatically |
| **Desktop (JVM)** | `./gradlew :app:run` | Opens a 520×900 dp native window via Compose Desktop |

### Build

```bash
# Android
./gradlew :app:assembleDebug                     # build Android demo APK
./gradlew :app:installDebug                      # install on device/emulator (does not launch)
./gradlew :bigbox3d-core:assembleRelease         # build core AAR
./gradlew :bigbox3d-compose:assembleRelease      # build compose AAR
./gradlew :opengl3:assembleRelease               # build legacy AAR

# Web
./gradlew :app:wasmJsBrowserProductionWebpack    # production web bundle → app/build/dist/wasmJs/productionExecutable/

# Tests
./gradlew test                                   # unit tests
./gradlew connectedAndroidTest                   # instrumented tests
```

## Architecture

### `:bigbox3d-core`

Pure KMP — zero platform imports in `commonMain`.

| Source set | Contents |
|---|---|
| `commonMain` | `GlApi` interface + GL constants; `RawImage` (RGBA `ByteArray`); `CuboidDimensions`; `AtlasBuilder` (pure-Kotlin nearest-neighbour scale + blit); `Matrix4` (pure-Kotlin port of `android.opengl.Matrix`); `Cuboid` (VBO-based GL rendering via `GlApi`); `CuboidRenderer` (rotation/zoom state, drives `Cuboid`); visual config enums |
| `androidMain` | `GlApiImpl` — thin delegation of every `GlApi` call to `GLES30.*` |
| `wasmJsMain` | `WebGl2Ctx` external interface (WebGL2 method declarations); `GlApiImpl(gl: WebGl2Ctx)` — maps OpenGL integer handles to WebGL JS objects via internal maps |
| `jvmMain` | `GlApiImpl` — delegates to LWJGL3 (`org.lwjgl.opengl.GL11`/`GL15`/`GL20`); uses `MemoryStack` for zero-GC buffer allocation on VBO uploads and uniform calls |

**Key design decisions:**
- `RawImage(width, height, pixels: ByteArray)` replaces `android.graphics.Bitmap` — no platform types in common code
- `GlApi` is passed per-call (not stored in `Cuboid`) so GL context recreation is safe
- `Matrix4` is a pure-Kotlin port of `android.opengl.Matrix` — same column-major `FloatArray` API
- Atlas building scales and blits directly into the atlas buffer (no intermediate `ByteArray` per face)
- All geometry is uploaded via VBOs (`glGenBuffers`/`glBindBuffer`/`glBufferData`) — no `java.nio` types in the `GlApi` interface. This is required by WebGL2 (which mandates VBOs) and works equally on Android GLES 3.0.
- `WebGl2Ctx` is a custom `external interface : JsAny` rather than the stdlib's `WebGL2RenderingContext` because the stdlib binding is incomplete. Callers cast `canvas.getContext("webgl2")` to `WebGl2Ctx`.
- OpenGL uses integer handles for GL objects; WebGL uses JS object references. The wasmJs `GlApiImpl` maintains internal `MutableMap<Int, JsAny>` tables to bridge them. Uniform locations are cached per `(programId, name)` pair so per-frame `glGetUniformLocation` calls don't leak.

### `:bigbox3d-compose`

KMP Compose widget layer. Depends on `:bigbox3d-core` via `api()` (so core types are re-exported to consumers).

| Source set | Contents |
|---|---|
| `commonMain` | `BigBox3D` composable (public API); `BoxTextureUrls` sealed interface (`FullBoxTextureUrls` / `EquatorialBoxTextureUrls`); `expect BigBox3DGlSurface`; `expect loadRawImageFromUrl`; `expect val ioDispatcher` |
| `androidMain` | `actual BigBox3DGlSurface` — `GLSurfaceView` in `AndroidView`, bridges `Renderer` callbacks to `CuboidRenderer`, handles pinch/rotate gestures; `actual loadRawImageFromUrl` — Coil 3 → `BitmapImage` → ARGB→RGBA extraction; `actual ioDispatcher = Dispatchers.IO`; internet permission in manifest |
| `wasmJsMain` | `actual BigBox3DGlSurface` — creates an HTML `<canvas>` appended to `<body>` as `position:fixed`, sized/positioned via `onGloballyPositioned`, render loop driven by `withFrameNanos`; `actual loadRawImageFromUrl` — browser `fetch` → `createImageBitmap` → `OffscreenCanvas` → `getImageData` pixels; `actual ioDispatcher = Dispatchers.Default` |
| `jvmMain` | `actual BigBox3DGlSurface` — CGL headless context (macOS) or GLFW hidden window (Linux/Windows) → FBO → `glReadPixels` → Y-flip → `BufferedImage.toComposeImageBitmap()`, displayed via Compose `Image`; render loop via `withFrameNanos`; drag/scroll gestures via `pointerInput`; VAO bound before `onSurfaceCreated`; `actual loadRawImageFromUrl` — Skiko `Image.makeFromEncoded` (handles WebP) → `Surface` → pixel readback; `actual ioDispatcher = Dispatchers.IO` |

**Data flow in `BigBox3D`:**
1. `LaunchedEffect` loads URLs → `List<RawImage>` on `ioDispatcher` (capped at 1024 px)
2. Builds `BoxTextureAtlas` on `Dispatchers.Default` (`buildAtlas2x3` + `cuboidDimensions`)
3. Passes atlas to `BigBox3DGlSurface` (expect/actual); shows `CircularProgressIndicator` while loading

**Web GL surface (`BigBox3DGlSurface.wasmJs.kt`):**
- An HTML `<canvas>` is created and appended to `<body>` as `position: fixed; z-index: 1`
- `onGloballyPositioned` syncs its CSS `left/top/width/height` to the Compose layout position and sets canvas `width`/`height` = CSS size × `devicePixelRatio` for HiDPI rendering
- Mouse (drag/wheel) and single-touch events are wired via `canvas.onmousedown` etc. (property assignment rather than `addEventListener`, so cleanup is a null-assign)
- The render loop runs inside a `LaunchedEffect` using `withFrameNanos { }` which backs onto `requestAnimationFrame`
- All `js("...")` calls use the Kotlin/Wasm `js()` intrinsic; Kotlin function parameters are directly accessible inside the JS string

**Web image loading (`ImageLoading.wasmJs.kt`):**
- Entirely browser-native: `fetch` → `.blob()` → `createImageBitmap` → `OffscreenCanvas` → `getImageData`
- Promise-to-coroutine bridge via `suspendCancellableCoroutine` + `js("promise.then(onFulfilled, onRejected)")`
- `Uint8ClampedArray` pixel values (0–255) extracted as `Int` via `js("data.data[i]")` and reinterpreted as signed `Byte` (same bit pattern) for `RawImage`
- No Coil dependency on web

**`ioDispatcher` expect/actual:** `Dispatchers.IO` does not exist on Kotlin/wasmJs (JS is single-threaded). The `expect val ioDispatcher` resolves to `Dispatchers.IO` on Android and JVM, and `Dispatchers.Default` on web.

### `:app`

| Source set | Contents |
|---|---|
| `commonMain` | `MainScreen`, `SettingsPanel`, and all UI composables — shared between Android, web, and desktop |
| `androidMain` | `MainActivity` (`ComponentActivity` entry point, wraps `MainScreen` in `GameBigBoxTheme`); `@Preview` composable; `GameBigBoxTheme` with Android dynamic colors |
| `wasmJsMain` | `main()` — web entry point using `ComposeViewport(document.body!!)` wrapped in `MaterialTheme` |
| `wasmJsMain/resources` | `index.html` — loads `skiko.js` and `app.js` |
| `jvmMain` | `main()` — desktop entry point using `singleWindowApplication` (520×900 dp) wrapped in `MaterialTheme`; LWJGL native jars for all platforms added as `runtimeOnly`; `compose.desktop.currentOs` provides the Compose Desktop runtime |

### `:opengl3` (legacy)

Self-contained Android-only implementation using `android.graphics.Bitmap` and `GLUtils.texImage2D`. Still publishable to JitPack as `com.github.jhavatar.gamebigbox:opengl3`. Not used by `:app` any more.

## Visual Config Enums (in `:bigbox3d-core`)

| Enum | Values |
|---|---|
| `GlossLevel` | MATTE (0.0) → HIGH_GLOSS (1.0) |
| `ShadowOpacity` | NONE → FULL (0.0–1.0 alpha) |
| `ShadowFade` | SUPER_SOFT / SOFT / REALISTIC / DRAMATIC |

## Adding a New Platform Target

To add desktop or another platform, these files are needed:

1. `bigbox3d-core/src/<platform>Main/…/GlApiImpl.kt` — implement `GlApi` delegating to the platform GL API (LWJGL3 for desktop)
2. `bigbox3d-compose/src/<platform>Main/…/BigBox3DGlSurface.<platform>.kt` — `actual fun BigBox3DGlSurface` embedding a GL surface in the platform's Compose interop
3. `bigbox3d-compose/src/<platform>Main/…/ImageLoading.<platform>.kt` — `actual fun loadRawImageFromUrl` using the platform image loader
4. `bigbox3d-compose/src/<platform>Main/…/Dispatchers.kt` — `actual val ioDispatcher`

### Desktop/JVM status

All four steps are complete. `:bigbox3d-core`, `:bigbox3d-compose`, and `:app` all have `jvm()` targets. Run the demo with `./gradlew :app:run`.

**Rendering approach:** The JVM `BigBox3DGlSurface` obtains an OpenGL core-profile context (see GL context below), renders into an FBO sized to the Compose layout bounds, reads back pixels with `glReadPixels`, flips the Y-axis (OpenGL is bottom-up), converts to a `BufferedImage` → `ImageBitmap`, and displays via a Compose `Image` composable. The render loop is driven by `withFrameNanos`. Each widget instance has its own dedicated single-threaded coroutine dispatcher and GL context.

**GL context creation (platform-specific):**
- **macOS:** Uses CGL (`org.lwjgl.opengl.CGL`) directly — `CGLChoosePixelFormat` + `CGLCreateContext` with `kCGLOGLPVersion_3_2_Core`. CGL is fully thread-safe and has no AppKit/HIToolbox involvement, unlike GLFW on macOS which triggers `dispatch_assert_queue` crashes via HIToolbox's Text Services Manager.
- **Linux / Windows:** Uses GLFW (`glfwCreateWindow` with `GLFW_VISIBLE = false`). `GlfwManager` (singleton) handles one-time `glfwInit()`. GLFW does not enforce the macOS main-thread requirement on these platforms.

**Image loading:** Uses Skiko's `Image.makeFromEncoded(bytes)` to decode downloaded bytes — this handles WebP natively via Skia's bundled libwebp, which `javax.imageio.ImageIO` (no WebP support) cannot. Decoded pixels are extracted via `Surface.readPixels` into RGBA `ByteArray` for `RawImage`.

**GLSL shaders:** `Cuboid.kt` calls `gl.glGetString(GlApi.GL_VERSION)` at `onSurfaceCreated` to detect ES vs desktop. ES/WebGL contexts get `#version 300 es` + `precision mediump float;`; desktop gets `#version 330 core` (no precision qualifier). This is the only GLSL difference — the rest of the shader source is identical.

**VAO:** OpenGL core-profile contexts (mandatory on macOS 10.9+) require a VAO bound before any vertex attribute calls. The JVM `BigBox3DGlSurface` creates and binds a default VAO immediately after `GL.createCapabilities()` and before `CuboidRenderer.onSurfaceCreated`.

## Known Issues / Quirks

- `expect @Composable fun BigBox3DGlSurface` triggers an IDE warning ("has no corresponding expected declaration") — this is a false positive caused by the Compose compiler transforming `@Composable` signatures at the IR level. The build succeeds.
- Internal class `EquitorialBoxTextureBitmaps` in `:opengl3` contains a typo ("Equitorial") — the public `EquatorialBoxTextureUrls` is spelled correctly.
- The Kotlin/Wasm stdlib's `WebGL2RenderingContext` binding is incomplete (many methods missing). The `WebGl2Ctx` custom `external interface` in `bigbox3d-core:wasmJsMain` works around this.
- The web `BigBox3DGlSurface` overlays a `position:fixed` canvas on top of the Compose canvas. If multiple `BigBox3D` widgets are visible simultaneously on web, their WebGL canvases are independent DOM elements stacked at `z-index:1` above the Compose Skia canvas.
- `BackHandler` (collapse bottom sheet on Android back press) was removed from `MainScreen` when it moved to `commonMain`. It can be re-added via an `expect`/`actual` if needed.
- Pinch-to-zoom on web touch screens is not yet implemented (single-finger drag works; wheel zoom works for mouse).
- Desktop/JVM platform consumers must add LWJGL3 native jars as `runtimeOnly` dependencies — the libraries ship only the binding JARs. Required natives: `org.lwjgl:lwjgl:3.3.4:natives-<platform>`, `org.lwjgl:lwjgl-opengl:3.3.4:natives-<platform>`, and `org.lwjgl:lwjgl-glfw:3.3.4:natives-<platform>` (GLFW natives are only needed on Linux/Windows; macOS uses CGL).
- On macOS, `glfwInit()` crashes with `SIGILL` in `libdispatch.dylib` (`_dispatch_assert_queue_fail`) when called from any non-main thread, including the AWT EDT, because GLFW's macOS path calls HIToolbox's Text Services Manager which asserts the GCD main queue. The JVM implementation bypasses this entirely by using CGL on macOS.
