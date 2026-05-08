import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(path = ":bigbox3d-compose"))
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.ui)
            implementation(libs.material3)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.ui.tooling.preview)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
                implementation(libs.androidx.ui.test.junit4)
            }
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            val lwjglVersion = libs.versions.lwjgl.get()
            listOf("lwjgl", "lwjgl-opengl", "lwjgl-glfw").forEach { lib ->
                listOf(
                    "natives-macos", "natives-macos-arm64",
                    "natives-linux", "natives-linux-arm64",
                    "natives-windows", "natives-windows-x86",
                ).forEach { native ->
                    runtimeOnly("org.lwjgl:$lib:$lwjglVersion:$native")
                }
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.chthonic.gamebigbox.MainKt"
    }
}

android {
    namespace = "io.chthonic.gamebigbox"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.chthonic.gamebigbox"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
    }
}

// platform() for BOMs is not supported inside kotlin { sourceSets {} } in KMP
dependencies {
    add("androidMainImplementation", platform(libs.androidx.compose.bom))
    add("androidInstrumentedTestImplementation", platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
