import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
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
    publishing {
        singleVariant("release")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    // 👇 This changes the actual output AAR file name
    setProperty("archivesBaseName", "gamebigbox-opengl3")
}

// --- 🔧 Auto-detect version from Git tag ---
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

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.jhavatar.gamebigbox"
                artifactId = "opengl3"
                version = gitTagVersion // ✅ automatically uses the latest tag

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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.material3)

    // image
    api(libs.coil)
    api(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}