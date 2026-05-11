# BigBox3d

Kotlin Compose UI widget that renders the big box of a PC game in 3D, e.g. 
<video src="https://github.com/user-attachments/assets/f829a1c2-ae13-4440-8c33-60e931a3c7bb" controls></video>

KMP library with targets (currently): Android, Web, JVM/Desktop. See more info in CLAUDE.md.

## KMP library usage

```kotlin
implementation("io.github.jhavatar:bigbox3d-compose:1.0.1")
```

```kotlin
BigBox3D(
    textureUrls = BoxTextureUrls(
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
)
```

`edgeAverageColor()` is also available as a `RawImage` extension if you want to derive a color manually after loading an image.

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


