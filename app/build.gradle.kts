plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

// Release signing comes from the environment (GitHub Actions secrets);
// local/CI builds without the variables stay debug-signed.
val etkKeystoreFile = System.getenv("ETK_KEYSTORE_FILE")

android {
    namespace = "dev.kurokawa.etecsa.toolkit"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "dev.kurokawa.etecsa.toolkit"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }
    signingConfigs {
        if (etkKeystoreFile != null) {
            create("release") {
                storeFile = file(etkKeystoreFile)
                storePassword = System.getenv("ETK_STORE_PASSWORD")
                keyAlias = System.getenv("ETK_KEY_ALIAS") ?: "etk"
                keyPassword = System.getenv("ETK_KEY_PASSWORD") ?: System.getenv("ETK_STORE_PASSWORD")
            }
        }
    }
    // Split APKs per ABI + universal: nobody on a metered connection
    // should download three architectures to use one.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (etkKeystoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
    implementation(libs.androidx.core.splashscreen)
}
