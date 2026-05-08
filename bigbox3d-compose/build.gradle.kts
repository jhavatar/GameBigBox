import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
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

    sourceSets {
        commonMain.dependencies {
            api(project(":bigbox3d-core"))
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.ui)
            implementation(libs.material3)
            implementation(libs.coil3)
            implementation(libs.coil3.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.coil3.android)
            implementation(libs.coil3.network.okhttp)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
        jvmMain.dependencies {
            implementation(libs.lwjgl)
            implementation(libs.lwjgl.opengl)
            implementation(libs.lwjgl.glfw)
        }
    }
}

android {
    namespace = "io.chthonic.bigbox3d.compose"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    setProperty("archivesBaseName", "bigbox3d-compose")
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(
        "io.github.jhavatar",
        "bigbox3d-compose",
        project.findProperty("library.version") as? String ?: libs.versions.library.get()
    )
    pom {
        name.set("BigBox3D Compose")
        description.set("Compose Multiplatform widget rendering a 3D textured cuboid (big box) via OpenGL ES on Android, WebGL2 on web, and LWJGL on desktop.")
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
