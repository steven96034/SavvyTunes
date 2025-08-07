plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.geminispotifyapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.geminispotifyapp"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField(
                "String",
                "SPOTIFY_SDK_API_KEY",
                "\"${System.getenv("SPOTIFY_SDK_API_KEY") ?: project.properties["SPOTIFY_SDK_API_KEY"]}\""
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx.v1131)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    //implementation("com.google.android.material:material:1.12.0")
    implementation(libs.generativeai)
    implementation(libs.adamratzman.spotify.api.kotlin.core)
    implementation(files("../spotify-app-remote-release-0.8.0.aar"))
    implementation(libs.gson)
    // Spotify
    implementation(libs.auth)
    implementation(libs.spotify.api.kotlin.core)
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.kt.coil.compose)
    // Network
    implementation(libs.retrofit)
    implementation (libs.converter.gson)
    implementation(libs.okhttp)
    implementation (libs.okhttp3.logging.interceptor)
    implementation(libs.converter.gson)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.commons.text)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.accompanist.navigation.animation)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}