import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Compose
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                api(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                // Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Serialization
                implementation(libs.kotlinx.serialization.json)

                // Ktor Networking
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)

                // Koin DI
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                // SQLDelight
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)

                // Coil Image Loading
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)

                // Lifecycle ViewModel (KMP)
                implementation(libs.androidx.lifecycle.viewmodel)

                // Lottie Compose (Compottie)
                api(libs.compottie)
            }
        }

        val androidMain by getting {
            dependencies {
                // Ktor OkHttp engine for Android
                implementation(libs.ktor.client.okhttp)

                // Coroutines
                implementation(libs.kotlinx.coroutines.android)

                // SQLDelight Android driver
                implementation(libs.sqldelight.android.driver)

                // Media3 / ExoPlayer
                implementation(libs.media3.exoplayer)
                implementation(libs.media3.exoplayer.dash)
                implementation(libs.media3.session)
                implementation(libs.media3.datasource.okhttp)

                // DataStore
                implementation(libs.datastore.preferences)

                // Google Sign In
                implementation(libs.play.services.auth)

                // Firebase Auth
                implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
                implementation("com.google.firebase:firebase-auth-ktx")
            }
        }

        val desktopMain by getting {
            dependencies {
                // Ktor CIO engine for Desktop JVM
                implementation(libs.ktor.client.cio)

                // Coroutines Swing dispatcher
                implementation(libs.kotlinx.coroutines.swing)

                // SQLDelight SQLite driver for Desktop
                implementation(libs.sqldelight.sqlite.driver)

                // vlcj for audio playback
                implementation(libs.vlcj)

                // JavaFX Media for native desktop audio playback
                val osName = System.getProperty("os.name").lowercase()
                val jfxPlatform = when {
                    osName.contains("win") -> "win"
                    osName.contains("mac") -> "mac"
                    else -> "linux"
                }
                implementation("org.openjfx:javafx-media:21.0.2:$jfxPlatform")
                implementation("org.openjfx:javafx-graphics:21.0.2:$jfxPlatform")
                implementation("org.openjfx:javafx-base:21.0.2:$jfxPlatform")
                implementation("org.openjfx:javafx-swing:21.0.2:$jfxPlatform")

                // DataStore
                implementation(libs.datastore.preferences.core)
            }
        }


        val desktopTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
    }
}

android {
    namespace = "com.melodify.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("MelodifyDatabase") {
            packageName.set("com.melodify.db")
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.melodify.shared.resources"
    generateResClass = always
}
