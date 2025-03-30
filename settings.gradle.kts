//pluginManagement {
//    repositories {
//        google {
//            content {
//                includeGroupByRegex("com\\.android.*")
//                includeGroupByRegex("com\\.google.*")
//                includeGroupByRegex("androidx.*")
//            }
//        }
//        mavenCentral()
//        gradlePluginPortal()
//    }
//    plugins {
//        // Add plugin versions explicitly to avoid resolution issues
//        id("com.android.application") version "8.1.1" // Adjust this version based on your Gradle version
//        id("org.jetbrains.kotlin.android") version "1.9.10" // Match the Kotlin version
//    }
//}
//dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
//    repositories {
//        google()
//        mavenCentral()
//        maven("https://jitpack.io")
//    }
//}
//
//rootProject.name = "PhysioScanner"
//include(":app")


pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Add plugin versions explicitly to avoid resolution issues
        id("com.android.application") version "8.1.1" // Adjust this version based on your Gradle version
        id("org.jetbrains.kotlin.android") version "1.9.10" // Match the Kotlin version
        id("com.google.gms.google-services") version "4.4.2" apply false // 🔥 Added Firebase Plugin
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "PhysioScanner"
include(":app")
