import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("io.sentry.android.gradle") version "6.0.0"
}

android {
    buildFeatures {
        viewBinding = true
    }
    namespace = "com.stankovic.zebrascanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.stankovic.zebrascanner"
        minSdk = 24 // zxing-android-embedded 4.x requires API 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // One shared key so every build (local or CI) can update the previous install.
    // Put the debug.keystore used for the build already on devices at app/keystore/debug.keystore.
    val sharedKeystore = file("keystore/debug.keystore")
    signingConfigs {
        if (sharedKeystore.exists()) {
            create("shared") {
                storeFile = sharedKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        if (sharedKeystore.exists()) {
            getByName("debug") { signingConfig = signingConfigs.getByName("shared") }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
    buildFeatures {
        compose = true
    }
}

sentry {
    org.set("your-sentry-org")
    projectName.set("zebra-scanner")
    authToken.set(System.getenv("SENTRY_AUTH_TOKEN"))
    // For automatic error reporting
    tracingInstrumentation {
        enabled.set(true)
    }
}

dependencies {
    implementation("androidx.core:core:1.17.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.hilt.android)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation(libs.androidx.hilt.navigation.compose)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:5.3.0"))

    // define any required OkHttp artifacts without version
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    ksp(libs.hilt.compiler)

}