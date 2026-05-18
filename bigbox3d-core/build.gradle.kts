import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
}

group = "io.github.jhavatar"
version = project.findProperty("library.version") as? String ?: libs.versions.library.get()

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

    iosArm64()
    iosX64()
    iosSimulatorArm64()

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
        // iosMain depends on nothing extra — Metal is part of the iOS SDK
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

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(
        "io.github.jhavatar",
        "bigbox3d-core",
        project.findProperty("library.version") as? String ?: libs.versions.library.get()
    )
    pom {
        name.set("BigBox3D Core")
        description.set("3D GL abstraction, geometry, atlas building, and rendering logic for the BigBox3D Compose widget.")
        url.set("https://github.com/jhavatar/GameBigBox")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("jhavatar")
                name.set("jhavatar")
                email.set("jhavatar@users.noreply.github.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/jhavatar/GameBigBox.git")
            developerConnection.set("scm:git:ssh://github.com/jhavatar/GameBigBox.git")
            url.set("https://github.com/jhavatar/GameBigBox")
        }
    }
}
