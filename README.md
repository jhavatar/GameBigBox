# BigBox3d

Kotlin Compose UI widget that renders the big box of a PC game in 3D, e.g. 
<video src="https://github.com/user-attachments/assets/f829a1c2-ae13-4440-8c33-60e931a3c7bb" controls></video>

KMP library with targets (currently): Android, Web, JVM/Desktop, iOS. See more info in CLAUDE.md.

## KMP library usage

```kotlin
implementation("io.github.jhavatar:bigbox3d-compose:1.0.3")
```

**From URLs** (loaded at runtime):
```kotlin
BigBox3D(
    textures = BoxTextureUrls(
        front = "https://…/front.webp",
        back  = "https://…/back.webp",
        // Explicit left + right:
        sides = SideSource.Explicit(left = "https://…/left.webp", right = "https://…/right.webp"),
        // Or a spine image — right face is auto-generated as its horizontal mirror:
        // sides = SideSource.Spine("https://…/spine.webp"),
        // Or solid color — pass null to auto-derive color from the front image's edges:
        // sides = SideSource.ColorFill(),
        caps = CapSource.Explicit(top = "https://…/top.webp", bottom = "https://…/bottom.webp"),
        // Or solid color (auto-derived from front edge average when color not supplied):
        // caps = CapSource.ColorFill(),
    ),
    rotationSpeed = RotationSpeed.VERY_SLOW, // NONE / VERY_SLOW / SLOW / NORMAL / FAST / VERY_FAST
    onLoadingChange = { isLoading -> /* fires true while atlas loads, false when ready */ },
)
```

**From bundled resources** (Compose Multiplatform `composeResources/files/`):
```kotlin
// In a coroutine / LaunchedEffect
BigBox3D(
    textures = BoxRawImages(
        front  = loadRawImageFromBytes(Res.readBytes("files/front.webp")),
        back   = loadRawImageFromBytes(Res.readBytes("files/back.webp")),
        left   = loadRawImageFromBytes(Res.readBytes("files/left.webp")),
        right  = loadRawImageFromBytes(Res.readBytes("files/right.webp")),
        top    = loadRawImageFromBytes(Res.readBytes("files/top.webp")),
        bottom = loadRawImageFromBytes(Res.readBytes("files/bottom.webp")),
    ),
)
```

`edgeAverageColor()` is also available as a `RawImage` extension if you want to derive a color manually after loading an image.

## BigBox3DProgress — loading indicator

`BigBox3DProgress` is a convenience wrapper that uses `BigBox3D` as a loading spinner. It stays permanently in the composition so its GL state and texture atlas survive show/hide cycles — no reload on every appearance.

```kotlin
BigBox3DProgress(
    textures = spinnerTextures,
    visible = isLoading,
)
```

When `visible` becomes `false` the render loop pauses immediately (zero GPU cost), the last frame fades out, and the composable collapses to zero size once the fade finishes.

`BigBox3D` shows an empty sized box while loading (no built-in spinner). Wire `onLoadingChange` to know when each item is loading and overlay your own indicator — or use a `BigBox3DProgress` pool (see `BigBox3DProgressPool` in the demo app) so the same pre-loaded atlas is shared across loading items.

### Reusing across screens with `movableContentOf`

Because `BigBox3DProgress` stays permanently composed, its GL state (loaded textures, atlas) is preserved as long as it remains in the composition tree. To reuse the **same instance** — and therefore the same already-loaded atlas — across multiple screens without reloading, wrap it in `movableContentOf` at the call site:

```kotlin
// Create once at the root of your navigation
val progressIndicator = remember {
    movableContentOf {
        BigBox3DProgress(textures = spinnerTextures, visible = isLoading)
    }
}

// Place it in whichever screen is active — state is carried across, not recreated
when (currentScreen) {
    Screen.Library    -> { LibraryScreen(...);    progressIndicator() }
    Screen.GameDetail -> { GameDetailScreen(...); progressIndicator() }
}
```

`movableContentOf` tells Compose to **move** the existing composition subtree (including GL context and atlas) rather than destroy and recreate it when it appears at a different location in the tree. Without it, navigating between screens would recreate `BigBox3DProgress` from scratch and reload the textures each time.

> [!NOTE]
> Images used were scraped from [Big Box Collection](https://bigboxcollection.com).

## Original legacy Android only library
### How to get it in your build
Step 1. Add it in your settings.gradle.kts at the end of repositories:
```
	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url = uri("https://jitpack.io") }
		}
	}
```
Step 2. Add the dependency
```
	dependencies {
	        implementation("com.github.jhavatar:gamebigbox:v1.0.1")
	}
```


