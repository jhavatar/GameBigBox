import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    id("maven-publish")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            // GLES30 comes from the Android SDK — no extra dependency needed
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
        jvmMain.dependencies {
            implementation(libs.lwjgl)
            implementation(libs.lwjgl.opengl)
        }
    }
}

android {
    namespace = "io.chthonic.bigbox3d.core"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    setProperty("archivesBaseName", "bigbox3d-core")
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}
