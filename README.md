UI widget (Composable) that renders the big box of a PC game in 3D, e.g. 
<video src="https://github.com/user-attachments/assets/f829a1c2-ae13-4440-8c33-60e931a3c7bb" controls></video>


## How to get it in your build
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


