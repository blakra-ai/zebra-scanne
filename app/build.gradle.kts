import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.ResValue
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("io.sentry.android.gradle") version "6.0.0"
}

// One clients/<name>.properties per company (see clients/_template.properties.example)
val clients: Map<String, Properties> = rootProject.file("clients")
    .listFiles { f -> f.extension == "properties" }
    .orEmpty()
    .sortedBy { it.name }
    .associate { f -> f.nameWithoutExtension to Properties().apply { f.inputStream().use { load(it) } } }
check(clients.isNotEmpty()) { "No client configs found in clients/*.properties" }

fun flavorOf(flavors: List<Pair<String, String>>, dimension: String) = flavors.first { it.first == dimension }.second

android {
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // Variants: <client><Env><BuildType>, e.g. lunaProdDebug -> assembleLunaProdDebug
    flavorDimensions += listOf("client", "env")
    productFlavors {
        clients.forEach { (name, props) ->
            create(name) {
                dimension = "client"
                applicationId = props.getProperty("applicationId")
            }
        }
        // dev and stage get their own app id so all three can be installed side by side
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("stage") {
            dimension = "env"
            applicationIdSuffix = ".stage"
            versionNameSuffix = "-stage"
        }
        create("prod") {
            dimension = "env"
        }
    }
    namespace = "com.stankovic.zebrascanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.stankovic.zebrascanner"
        minSdk = 24 // zxing-android-embedded 4.x requires API 24
        targetSdk = 36
        // CI sets these from the git tag (vX.Y.Z) and pipeline number; local builds use the defaults
        versionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 2
        versionName = System.getenv("APP_VERSION_NAME") ?: "1.1.0"

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
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.androidx.hilt.navigation.compose)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:5.5.1"))

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

androidComponents {
    // Skip environments the client has no server address for
    beforeVariants { variant ->
        val client = flavorOf(variant.productFlavors, "client")
        val env = flavorOf(variant.productFlavors, "env")
        if (clients[client]?.getProperty("url.$env").isNullOrBlank()) variant.enable = false
    }
    onVariants { variant ->
        val client = flavorOf(variant.productFlavors, "client")
        val env = flavorOf(variant.productFlavors, "env")
        val props = clients.getValue(client)
        val url = props.getProperty("url.$env").trim().trimEnd('/')
        variant.buildConfigFields?.put("API_BASE_URL", BuildConfigField("String", "\"$url\"", "Backend for this client/env"))
        variant.buildConfigFields?.put("CLIENT", BuildConfigField("String", "\"$client\"", null))
        variant.buildConfigFields?.put("ENVIRONMENT", BuildConfigField("String", "\"$env\"", null))
        // In-app updates from the client's Redmine project (update/AppUpdater.kt). The key is a
        // download-only Redmine account's, given by CI (UPDATE_API_KEY_<CLIENT> or UPDATE_API_KEY)
        // and never stored in the repository; without one the app has no update button.
        val updateProject = props.getProperty("updates.project", "").trim()
        val updateKey = (System.getenv("UPDATE_API_KEY_${client.uppercase()}") ?: System.getenv("UPDATE_API_KEY") ?: "").trim()
            .takeIf { updateProject.isNotEmpty() && it.matches(Regex("[0-9a-fA-F]{40}")) } ?: ""
        val redmine = props.getProperty("updates.redmine", "https://projects.solutions-apps.com").trim().trimEnd('/')
        variant.buildConfigFields?.put("UPDATE_REDMINE_URL", BuildConfigField("String", "\"$redmine\"", null))
        variant.buildConfigFields?.put("UPDATE_PROJECT", BuildConfigField("String", "\"$updateProject\"", null))
        variant.buildConfigFields?.put("UPDATE_KEY", BuildConfigField("String", "\"$updateKey\"", null))
        val name = props.getProperty("displayName", "Scanner") + if (env == "prod") "" else " ${env.uppercase()}"
        variant.resValues?.put(variant.makeResValueKey("string", "app_name"), ResValue(name, null))
    }
}
