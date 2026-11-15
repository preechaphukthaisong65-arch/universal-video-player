import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystoreBase64 = providers.environmentVariable("ANDROID_KEYSTORE_BASE64").orNull
val keystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val keyAliasValue = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val keyPasswordValue = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val releaseSigningReady = listOf(keystoreBase64, keystorePassword, keyAliasValue, keyPasswordValue).all { !it.isNullOrBlank() }
val generatedKeystore = layout.buildDirectory.file("signing/release.p12")

if (releaseSigningReady) {
    val outputFile = generatedKeystore.get().asFile
    outputFile.parentFile.mkdirs()
    outputFile.writeBytes(Base64.getDecoder().decode(keystoreBase64))
}

android {
    namespace = "com.preechaphukthaisong65.universalvideoplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.preechaphukthaisong65.universalvideoplayer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = generatedKeystore.get().asFile
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core-common"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":player"))
    implementation(project(":browser"))
    implementation(project(":presentation-theme"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.6")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.6")
}
