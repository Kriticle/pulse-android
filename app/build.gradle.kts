import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

fun configValue(propertyName: String, envName: String, defaultValue: String = ""): String =
    localProperties.getProperty(propertyName)
        ?: providers.gradleProperty(propertyName).orNull
        ?: providers.environmentVariable(envName).orNull
        ?: defaultValue

android {
    namespace = "com.pulse.app"
    compileSdk = 35

    val supabaseUrl = configValue("supabase.url", "SUPABASE_URL")
    val supabaseKey = configValue("supabase.key", "SUPABASE_KEY")
    val googleWebClientId = configValue("google.web.client.id", "GOOGLE_WEB_CLIENT_ID")
    val apiBaseUrl = configValue("api.base.url", "API_BASE_URL", "http://10.0.2.2:8080/")

    defaultConfig {
        applicationId = "com.pulse.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        manifestPlaceholders["usesCleartextTraffic"] = "true"
    }

    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }

        release {
            isMinifyEnabled = false
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)
    implementation(libs.google.signin)
    implementation(libs.androidx.tracing)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.pose.detection)
}

val validateReleaseConfig by tasks.registering {
    group = "verification"
    description = "Validates production configuration before building a release APK or bundle."

    doLast {
        val requiredValues = mapOf(
            "supabase.url / SUPABASE_URL" to configValue("supabase.url", "SUPABASE_URL"),
            "supabase.key / SUPABASE_KEY" to configValue("supabase.key", "SUPABASE_KEY"),
            "google.web.client.id / GOOGLE_WEB_CLIENT_ID" to configValue("google.web.client.id", "GOOGLE_WEB_CLIENT_ID"),
            "api.base.url / API_BASE_URL" to configValue("api.base.url", "API_BASE_URL")
        )

        val missing = requiredValues.filterValues { it.isBlank() }.keys
        if (missing.isNotEmpty()) {
            throw GradleException("Missing release config: ${missing.joinToString()}")
        }

        val apiBaseUrl = requiredValues.getValue("api.base.url / API_BASE_URL")
        val localHosts = listOf("localhost", "127.0.0.1", "10.0.2.2", "192.168.")
        if (!apiBaseUrl.startsWith("https://") || localHosts.any { apiBaseUrl.contains(it) }) {
            throw GradleException("Release API_BASE_URL must be a public HTTPS URL, got: $apiBaseUrl")
        }
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(validateReleaseConfig)
}
