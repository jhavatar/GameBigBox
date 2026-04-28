import java.io.ByteArrayOutputStream
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.material)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.material3)
            api(libs.coil)
            api(libs.coil.compose)
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
            }
        }
    }
}

android {
    namespace = "io.chthonic.gamebigbox.opengl3"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
    setProperty("archivesBaseName", "gamebigbox-opengl3")
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

val gitTagVersion: String by lazy {
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine = listOf("git", "describe", "--tags", "--abbrev=0")
            standardOutput = stdout
        }
        stdout.toString().trim()
    } catch (e: Exception) {
        "unspecified"
    }
}

// platform() for BOMs is not supported inside kotlin { sourceSets {} } in KMP
dependencies {
    add("androidMainImplementation", platform(libs.androidx.compose.bom))
}

afterEvaluate {
    publishing {
        publications.withType<MavenPublication>().configureEach {
            version = gitTagVersion
            pom {
                name.set("GameBigBox opengl3")
                description.set("3D renderer for PC game big boxes for Android (Compose + OpenGL ES 3.0)")
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
                        name.set("JH de Vaal")
                        email.set("jhdevaal+github@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:github.com/jhavatar/GameBigBox.git")
                    developerConnection.set("scm:git:ssh://github.com/jhavatar/GameBigBox.git")
                    url.set("https://github.com/jhavatar/GameBigBox")
                }
            }
        }
    }
}
