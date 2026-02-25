plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
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
        testInstrumentationRunner = "com.example.geminispotifyapp.HiltTestRunner"
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
    testOptions {
        unitTests {
            all {
                it.useJUnitPlatform()
            }
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
    implementation(libs.generativeai)
    implementation(libs.gson)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.auth)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.kt.coil.compose)
    implementation(libs.retrofit)
    implementation (libs.converter.gson)
    implementation(libs.okhttp)
    implementation (libs.okhttp3.logging.interceptor)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.commons.text)
    implementation(libs.hilt.android)
    implementation(libs.androidx.browser)
    implementation(libs.play.services.location)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.google.firebase.ai)
    implementation(libs.androidx.lifecycle.process)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.accompanist.permissions)
    implementation(libs.kotlin.test)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.ai)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.truth)

    testRuntimeOnly(libs.junit.jupiter.engine)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation (libs.truth)
    kspAndroidTest (libs.hilt.android.compiler)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(kotlin("test"))
}