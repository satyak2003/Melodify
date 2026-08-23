import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(libs.kotlinx.coroutines.swing)

                // vlcj
                implementation(libs.vlcj)

                // Koin
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                // Coil
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor)

                // Icons
                implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.7.3")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.melodify.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "Melodify"
            packageVersion = "1.0.0"
            description = "Ad-free music player with Spotify import"
            copyright = "Ac 2025 Melodify"
            vendor = "Melodify"

            windows {
                menuGroup = "Melodify"
                upgradeUuid = "4c5f6a7b-8c9d-10e1-a2b3-c4d5e6f70001"
                iconFile.set(project.file("src/main/resources/icon.ico"))
                dirChooser = true
                shortcut = true
                menu = true
            }

            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }

        buildTypes.release.proguard {
            isEnabled = false
        }
    }
}


tasks.withType<AbstractJPackageTask>().configureEach {
    if (targetFormat.name.contains("Msi") || targetFormat.name.contains("Exe")) {
        freeArgs.add("--resource-dir")
        freeArgs.add(project.file("installer-resources").absolutePath)
    }
}
