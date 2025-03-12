//plugins {
//    id("com.android.application")
//    id("org.jetbrains.kotlin.android") version "1.9.10"
//}
//
//android {
//    namespace = "com.example.physioscanner"
//    compileSdk = 34
//
//    defaultConfig {
//        applicationId = "com.example.physioscanner"
//        minSdk = 24
//        targetSdk = 34
//        versionCode = 1
//        versionName = "1.0"
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
//    kotlinOptions {
//        jvmTarget = "11"
//    }
//    buildFeatures {
//        viewBinding = true
//    }
//}
//
//dependencies {
//    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
//    implementation("androidx.appcompat:appcompat:1.6.1")
//    implementation("com.google.android.material:material:1.9.0")
//    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
//    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
//    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
//
//    // Existing OkHttp dependency (keep if you use it elsewhere)
//    implementation("com.squareup.okhttp3:okhttp:4.9.3")
//
//    // Add the Socket.IO client dependency.
//    implementation("io.socket:socket.io-client:2.0.1") {
//        exclude(group = "org.json", module = "json")
//    }
//
//    // Corrected SwipeRefreshLayout dependency (using stable version 1.1.0)
//    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
//
//    // Coroutine support
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
//
//    testImplementation("junit:junit:4.13.2")
//    androidTestImplementation("androidx.test.ext:junit:1.1.5")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
//}


plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // 🔥 Add Google Services Plugin for Firebase
    id("org.jetbrains.kotlin.android") version "1.9.10"
}

android {
    namespace = "com.example.physioscanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.physioscanner"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17  // 🔥 Updated to match the first code
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    // Existing OkHttp dependency
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Socket.IO client dependency
    implementation("io.socket:socket.io-client:2.0.1") {
        exclude(group = "org.json", module = "json")
    }

    // SwipeRefreshLayout dependency
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Coroutine support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    // Firebase BoM (Manages versions automatically) 🔥 Added
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))

    // Firebase Authentication 🔥 Added
    implementation("com.google.firebase:firebase-auth")

    // Firestore Database (if needed) 🔥 Added
    implementation("com.google.firebase:firebase-firestore")

    // Realtime Database (if needed) 🔥 Added
    implementation("com.google.firebase:firebase-database")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
