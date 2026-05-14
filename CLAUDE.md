# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GameBigBox is a Kotlin Multiplatform project that provides a `BigBox3D` Compose widget rendering a 3D textured cuboid (a physical PC game "big box") via OpenGL ES 3.0 on Android and WebGL2 on web. Touch/mouse gestures support rotation and scroll/pinch-to-zoom.

- **Language:** Kotlin 2.3.21 | **minSdk:** 26 | **compileSdk:** 36
- **Build:** KMP (`kotlin("multiplatform")`) — `androidTarget()` + `wasmJs { browser() }` on library/app modules; `jvm()` on `:bigbox3d-core`, `:bigbox3d-compose`, and `:app`
- **UI:** Compose Multiplatform 1.10.3 + Material3; GL surface embedded via `AndroidView` on Android, WebGL canvas overlay on web
- **Rendering:** OpenGL ES 3.0 (`GLSurfaceView`) on Android; WebGL2 on web. All rendering goes through the platform-agnostic `GlApi` interface using VBOs.
- **Image loading:** Coil 3.4.0 on Android (OkHttp network fetcher); Skiko `Image.makeFromEncoded` on JVM (handles WebP); browser `fetch` → `createImageBitmap` → `OffscreenCanvas` on web

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
|--------|------|---------|
| `:bigbox3d-core` | KMP library (Android + wasmJs + JVM) | All 3D logic — GL abstraction, geometry, atlas building, rendering |
| `:bigbox3d-compose` | KMP Compose library (Android + wasmJs + JVM) | `BigBox3D` Compose widget; image loading; platform GL surface |
| `:opengl3` | Android library (legacy) | Original self-contained Android implementation; still published to JitPack |
| `:app` | KMP app (Android + wasmJs + JVM) | Demo app showing multiple `BigBox3D` widgets with a live `SettingsPanel` |

## Common Commands

### Launch the demo app

| Target | Command | Notes |
|--------|---------|-------|
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

## Publishing

`:bigbox3d-core` and `:bigbox3d-compose` are configured for Maven Central under the group `io.github.jhavatar`.

### One-time setup

1. Register at [central.sonatype.com](https://central.sonatype.com) — the `io.github.jhavatar` namespace is auto-approved for GitHub users
2. Verify the `io.github.jhavatar` namespace: add it in View Account → Namespaces, create a public GitHub repo named after the verification token, click Verify
3. Generate a GPG key — **must be RSA** (BouncyCastle, used by Gradle signing, does not support the default Ed25519/ECDH key type):
   ```bash
   gpg --full-generate-key   # choose RSA and RSA, 4096 bits
   gpg --list-secret-keys --keyid-format SHORT   # note the key ID
   ```
4. Upload the public key to a keyserver:
   ```bash
   gpg --export --armor YOUR_KEY_ID > public_key.asc
   # upload public_key.asc at keys.openpgp.org → Upload
   ```
5. Write the private key to `~/.gradle/gradle.properties` (the key is too long to paste in an editor — use this instead):
   ```bash
   python3 -c "import subprocess; result = subprocess.run(['gpg','--export-secret-keys','--armor','YOUR_KEY_ID'], capture_output=True, text=True); key = result.stdout.replace('\n','\\\\n'); open('/Users/jhdevaal/.gradle/gradle.properties', 'a').write('signingInMemoryKey=' + key + '\n')"
   ```
   Then add the passphrase line manually:
   ```bash
   echo "signingInMemoryKeyPassword=your-gpg-passphrase" >> ~/.gradle/gradle.properties
   ```
6. Generate a user token: central.sonatype.com → View Account → Generate User Token
7. Add the token credentials as **environment variables** in `~/.zshrc` — **do not use `~/.gradle/gradle.properties` for these** (Gradle build services that perform the upload cannot read project properties, but env vars are always available):
   ```bash
   echo 'export ORG_GRADLE_PROJECT_mavenCentralUsername="your-token-username"' >> ~/.zshrc
   echo 'export ORG_GRADLE_PROJECT_mavenCentralPassword="your-token-password"' >> ~/.zshrc
   source ~/.zshrc
   ```

### Release commands

```bash
# Publish and release both libraries to Maven Central
./gradlew publishAndReleaseToMavenCentral

# Override the version (default: library.version in libs.versions.toml, currently 1.0.0)
./gradlew publishAndReleaseToMavenCentral -Plibrary.version=1.1.0
```

Maven Central is **immutable** — once a version is published it cannot be overwritten. Always bump `library.version` before republishing.

### Troubleshooting

- **`cannot recognise keyAlgorithm: 18`** — GPG key uses Ed25519/ECDH which BouncyCastle doesn't support. Regenerate with `gpg --full-generate-key` selecting RSA and RSA.
- **`Invalid token`** — Maven Central credentials are being rejected. The `mavenCentralUsername`/`mavenCentralPassword` properties in `~/.gradle/gradle.properties` are NOT read by the upload build service — use the `ORG_GRADLE_PROJECT_` env vars in `~/.zshrc` instead.
- **Deployment stuck in "Publishing"** — wait up to 30 minutes; if still stuck contact Sonatype support to drop it. Deployments in "Publishing" state cannot be dropped by users.
- **"currently being published in another deployment"** — a previous failed attempt left a pending deployment. Drop it on central.sonatype.com → Deployments, then re-run.

### Consumer dependency

```kotlin
// No extra repo URL needed — Maven Central is in the default search path
implementation("io.github.jhavatar:bigbox3d-compose:1.0.1")
// bigbox3d-core is resolved automatically as a transitive dependency
```

---

## Architecture

### `:bigbox3d-core`

Pure KMP — zero platform imports in `commonMain`.

| Source set | Contents |
|------------|----------|
| `commonMain` | `GlApi` interface + GL constants (including `isGlEs(): Boolean` for per-platform GLSL preamble selection); `RawImage` (RGBA `ByteArray`); `CuboidDimensions` + three factory functions: `cuboidDimensions(front, side)`, `cuboidDimensionsFromTop(front, top)`, `cuboidDimensions(front, depthRatio)`; `AtlasBuilder` (pure-Kotlin nearest-neighbour scale + blit); `Matrix4` (pure-Kotlin port of `android.opengl.Matrix`); `Cuboid` (VBO-based GL rendering via `GlApi`); `CuboidRenderer` (rotation/zoom state, drives `Cuboid`); visual config enums including `RotationSpeed` |
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
|------------|----------|
| `commonMain` | \`BigBox3D\` composable (public API — takes \`textures: BoxTexture\`, \`paused: Boolean = false\`); \`BigBox3DProgress\` composable (loading-indicator wrapper — see below); `BoxTexture` sealed interface with `boxKey(): String` (stable LazyColumn key); `BoxTextureUrls : BoxTexture` (URL-based, supports `SideSource`/`CapSource`); `BoxRawImages : BoxTexture` (pre-loaded faces, for bundled resources — see `loadRawImageFromBytes`); `SideSource` sealed interface (`Explicit`, `Spine`, `ColorFill`); `CapSource` sealed interface (`Explicit`, `ColorFill`); `RawImageExt.kt` (`edgeAverageColor()` — averages edge pixels of a `RawImage` to infer background color); `expect BigBox3DGlSurface`; `expect loadRawImageFromUrl`; `expect loadRawImageFromBytes`; `expect val ioDispatcher` |
| `androidMain` | `actual BigBox3DGlSurface` — `GLSurfaceView` in `AndroidView`, bridges `Renderer` callbacks to `CuboidRenderer`; gestures handled via `Modifier.pointerInput` (horizontal drag = rotate, vertical passes to `LazyColumn` for scroll, pinch = zoom); `actual loadRawImageFromUrl` — Coil 3 → `BitmapImage` → ARGB→RGBA extraction; `actual ioDispatcher = Dispatchers.IO`; internet permission in manifest |
| `wasmJsMain` | `actual BigBox3DGlSurface` — creates a WebGL `<canvas>` appended to `<html>` (not `<body>`) with `position:fixed; pointer-events:none; z-index:1`; gestures handled via `Modifier.pointerInput` on the Box (drag = rotate; scroll = LazyColumn; scroll over stationary box = zoom via 300 ms debounce); `onSurfaceCreated` called after first `jsResizeCanvas` due to WebGL context-reset behaviour; `localToWindow(Offset.Zero)` + `coords.size` gives full composable dimensions during scroll; `actual loadRawImageFromUrl` — browser `fetch` → `createImageBitmap` → `OffscreenCanvas` → `getImageData` pixels; `actual ioDispatcher = Dispatchers.Default` |
| `jvmMain` | `actual BigBox3DGlSurface` — CGL headless context (macOS) or GLFW hidden window (Linux/Windows) → FBO → `glReadPixels` → Y-flip → `BufferedImage.toComposeImageBitmap()`, displayed via Compose `Image`; render loop via `withFrameNanos`; drag/scroll gestures via `pointerInput`; VAO bound before `onSurfaceCreated`; `actual loadRawImageFromUrl` — Skiko `Image.makeFromEncoded` (handles WebP) → `Surface` → pixel readback; `actual ioDispatcher = Dispatchers.IO` |

**Data flow in `BigBox3D`:**
1. `LaunchedEffect(textures)` resolves face images into `List<RawImage>`: for `BoxTextureUrls`, all URL fetches are fired concurrently (`async`/`coroutineScope`) on `ioDispatcher`; for `BoxRawImages`, the pre-loaded images are used directly (no network). `SideSource.Spine` generates the right face by flipping the spine image horizontally. `SideSource.ColorFill` / `CapSource.ColorFill` generate solid-color 1×1 images (auto-derived from the front image's edge average when no color is supplied — computed lazily so `edgeAverageColor()` runs at most once per load).
2. Builds `BoxTextureAtlas` on `Dispatchers.Default` (`buildAtlas2x3` + dimension derivation). Depth inferred in priority order: side image aspect ratio → top image aspect ratio → hardcoded fallback `0.18f`. `CancellationException` is re-thrown (not swallowed) so Compose can restart the effect on key change.
3. Passes atlas to `BigBox3DGlSurface` (expect/actual); shows `loadingContent` composable (default: `CircularProgressIndicator`) while loading. `supportsFullXAxisRotation` is always `true`.

**Using bundled resources with `BoxRawImages`:** Place images in `src/commonMain/composeResources/files/` and add `compose.components.resources` to `commonMain` dependencies. Then:
```kotlin
BoxRawImages(
    front = loadRawImageFromBytes(Res.readBytes("files/front.webp")),
    ...
)
```
`Res` is the generated per-module object in `<packageName>.generated.resources`; import it explicitly (it is NOT in `org.jetbrains.compose.resources`). `loadRawImageFromBytes` decodes on all platforms: Android via `BitmapFactory`, JVM via Skiko, wasmJs via `data:` URL + browser `createImageBitmap`.

**Web GL surface (`BigBox3DGlSurface.wasmJs.kt`):**
- A WebGL2 `<canvas>` is appended to `<html>` (not `<body>`) with `position:fixed; pointer-events:none; z-index:1`. It must go to `<html>` because Compose MP 1.10.x sets `position:relative; overflow:hidden` on `<body>`, which causes a Firefox bug where `position:fixed` children have `offsetWidth=0` and are invisible.
- `pointer-events:none` on the canvas lets all pointer events pass through to Compose, so the `LazyColumn` can scroll freely and `Modifier.pointerInput` on the placeholder `Box` handles gestures.
- Positioning uses `coords.localToWindow(Offset.Zero)` (true origin, can be negative when scrolled off-screen) and `coords.size` (full composable dimensions, not clipped by viewport). This keeps the canvas full-size as it scrolls partially off-screen.
- `onSurfaceCreated` is called inside `onGloballyPositioned` — AFTER `jsResizeCanvas` — because `canvas.width/height` changes reset the WebGL context (wiping all GL state). In Compose MP 1.10.x, `DisposableEffect` runs before `onGloballyPositioned`, so initialising GL in `DisposableEffect` and then resizing would lose all GL resources.
- The render loop is a `LaunchedEffect` using `withFrameNanos { }` (backs onto `requestAnimationFrame`). It only draws when `glReady` is true (after the first resize + `onSurfaceCreated`).
- Scroll-wheel zoom uses a 300 ms debounce: if scroll events arrive faster than 300 ms apart the box is deemed "in list-scroll motion" and zoom is suppressed; once quiescent, the next wheel tick zooms.

**Web image loading (`ImageLoading.wasmJs.kt`):**
- Entirely browser-native: `fetch` → `.blob()` → `createImageBitmap` → `OffscreenCanvas` → `getImageData`. Also supports `data:` URLs (used by `loadRawImageFromBytes` on wasmJs).
- Promise-to-coroutine bridge via `suspendCancellableCoroutine` + `js("promise.then(onFulfilled, onRejected)")`
- Pixel extraction packs 4 RGBA bytes into one `Int` per Wasm→JS bridge crossing (`jsGetPixelRgba`) — 1M crossings for a 1024×1024 image instead of 4M. JS `<<` operates on signed 32-bit ints; `ushr` in Kotlin extracts unsigned bytes correctly.
- No Coil dependency on web

**`ioDispatcher` expect/actual:** `Dispatchers.IO` does not exist on Kotlin/wasmJs (JS is single-threaded). The `expect val ioDispatcher` resolves to `Dispatchers.IO` on Android and JVM, and `Dispatchers.Default` on web.

### `:app`

| Source set | Contents |
|------------|----------|
| `commonMain` | `MainScreen`, `SettingsPanel`, and all UI composables — shared between Android, web, and desktop. Uses `compose.components.resources` for bundled images in `composeResources/files/`. `LazyColumn` items use `BoxTexture.boxKey()` as stable keys to prevent state reuse when the list is dynamically prepended. |
| `androidMain` | `MainActivity` (`ComponentActivity` entry point, wraps `MainScreen` in `GameBigBoxTheme`); `@Preview` composable; `GameBigBoxTheme` with Android dynamic colors |
| `wasmJsMain` | `main()` — web entry point using `ComposeViewport(document.body!!)` wrapped in `MaterialTheme` |
| `wasmJsMain/resources` | `index.html` — loads `app.js` (Skiko is bundled into `app.js` by webpack in Compose MP 1.10.x; the old separate `skiko.js` tag was removed) |
| `jvmMain` | `main()` — desktop entry point using `singleWindowApplication` (520×900 dp) wrapped in `MaterialTheme`; LWJGL native jars for all platforms added as `runtimeOnly`; `compose.desktop.currentOs` provides the Compose Desktop runtime |

### `:opengl3` (legacy)

Self-contained Android-only implementation using `android.graphics.Bitmap` and `GLUtils.texImage2D`. Still publishable to JitPack as `com.github.jhavatar.gamebigbox:opengl3`. Not used by `:app` any more.

## BigBox3DProgress — loading indicator composable

`BigBox3DProgress` wraps `BigBox3D` for use as a reusable loading spinner. It stays permanently in the composition so the GL state and texture atlas survive show/hide cycles with no reload.

**Key behaviours:**
- `paused = !visible` — render loop stops immediately when hidden (zero GPU cost)
- Alpha fades in/out over `fadeDurationMs` (default 300 ms) using `updateTransition`
- Size collapses to `0.dp` only after the fade-out completes (`transition.currentState != transition.targetState` guards the size) — no layout space or touch interception when invisible
- Defaults: `RotationSpeed.VERY_FAST`, `ShadowOpacity.STRONG`, `ShadowFade.REALISTIC`, `size = 200.dp`

**`movableContentOf` — reusing the same instance across screens:**

Compose identifies composables by their position in the tree. Navigating between screens normally destroys and recreates `BigBox3DProgress`, reloading the atlas on every appearance. `movableContentOf` tells Compose to carry the existing composition subtree (GL context, atlas, coroutines) to the new location instead of recreating it:

```kotlin
val spinner = remember {
    movableContentOf {
        BigBox3DProgress(textures = spinnerTextures, visible = isLoading)
    }
}
// Place spinner() wherever needed — one atlas load, zero reloads on navigation
```

`movableContentOf` must be created at the call site (e.g. in the root navigation composable). If it were hidden inside `BigBox3DProgress` itself, the wrapper node would be the fixed point in the tree and the move benefit would be lost.

## Visual Config Enums (in `:bigbox3d-core`)

| Enum | Values |
|------|--------|
| `RotationSpeed` | NONE (0°/s) / VERY_SLOW / SLOW / NORMAL / FAST / VERY_FAST (~240°/s). Default in `BigBox3D` is `VERY_SLOW`. Each value's `deltaX` is passed directly to `CuboidRenderer.rotate()`, which multiplies by `ROTATION_SENSITIVITY (0.4f)`. `NONE` skips the `rotate()` call entirely. Replaces the old `autoRotate: Boolean` + `rotationSpeed: Float` parameters. |
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

**GLSL shaders:** `Cuboid.kt` calls `gl.isGlEs()` at `onSurfaceCreated` to select the shader preamble. `isGlEs()` is a method on the `GlApi` interface, implemented as `true` on Android and wasmJs, `false` on JVM. ES/WebGL contexts get `#version 300 es` + `precision mediump float;`; desktop gets `#version 330 core` (no precision qualifier). This is the only GLSL difference — the rest of the shader source is identical.

**VAO:** OpenGL core-profile contexts (mandatory on macOS 10.9+) require a VAO bound before any vertex attribute calls. The JVM `BigBox3DGlSurface` creates and binds a default VAO immediately after `GL.createCapabilities()` and before `CuboidRenderer.onSurfaceCreated`.

## Known Issues / Quirks

- `expect @Composable fun BigBox3DGlSurface` triggers an IDE warning ("has no corresponding expected declaration") — this is a false positive caused by the Compose compiler transforming `@Composable` signatures at the IR level. The build succeeds.
- Internal class `EquitorialBoxTextureBitmaps` in `:opengl3` contains a typo ("Equitorial") — the public `EquatorialBoxTextureUrls` in that module is spelled correctly. Note: `:bigbox3d-compose` has replaced its own `FullBoxTextureUrls`/`EquatorialBoxTextureUrls` with the composable `BoxTextureUrls` + `SideSource` + `CapSource` types; `:opengl3` retains its own independent hierarchy.
- The Kotlin/Wasm stdlib's `WebGL2RenderingContext` binding is incomplete (many methods missing). The `WebGl2Ctx` custom `external interface` in `bigbox3d-core:wasmJsMain` works around this.
- The web `BigBox3DGlSurface` overlays a `position:fixed; pointer-events:none` canvas on `<html>`. Multiple `BigBox3D` widgets produce independent canvases at `z-index:1`. Because `pointer-events:none`, all gestures are routed through Compose's pointer system on the Box placeholder beneath.
- Android gesture handling uses `Modifier.pointerInput` on the `AndroidView` (not a native `OnTouchListener`). Horizontal-dominant drags rotate the box; vertical-dominant drags are released to the `LazyColumn` for scroll; pinch zooms. The old `requestDisallowInterceptTouchEvent` approach broke in Compose MP 1.10.x because Compose's pointer-input scroll system no longer honours it from a native child view.
- `BackHandler` (collapse bottom sheet on Android back press) was removed from `MainScreen` when it moved to `commonMain`. It can be re-added via an `expect`/`actual` if needed.
- Pinch-to-zoom on web touch screens is not yet implemented. Mouse-wheel zoom works when the list is stationary (suppressed during list scroll via a 300 ms debounce).
- Desktop/JVM platform consumers must add LWJGL3 native jars as `runtimeOnly` dependencies — the libraries ship only the binding JARs. Required natives: `org.lwjgl:lwjgl:3.4.1:natives-<platform>`, `org.lwjgl:lwjgl-opengl:3.4.1:natives-<platform>`, and `org.lwjgl:lwjgl-glfw:3.4.1:natives-<platform>` (GLFW natives are only needed on Linux/Windows; macOS uses CGL).
- On macOS, `glfwInit()` crashes with `SIGILL` in `libdispatch.dylib` (`_dispatch_assert_queue_fail`) when called from any non-main thread, including the AWT EDT, because GLFW's macOS path calls HIToolbox's Text Services Manager which asserts the GCD main queue. The JVM implementation bypasses this entirely by using CGL on macOS.
