plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.copytext.floating"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.copytext.floating"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    // ML Kit Text Recognition - Latin + Indonesian support (v2)
    implementation("com.google.mlkit:text-recognition:16.0.0")
    // For Chinese/Japanese/Korean uncomment:
    // implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
}
