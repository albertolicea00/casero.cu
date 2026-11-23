import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// Release signing lives in keys/release.properties (git-ignored; copy keys/release.properties.example).
// Read as UTF-8; storeFile is relative to keys/.
val releaseSigning = rootProject.file("keys/release.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.reader(Charsets.UTF_8).use { load(it) } }
}
if (releaseSigning == null && gradle.startParameter.taskNames.any { it.contains("Release") }) {
    throw GradleException("Release signing needs keys/release.properties. Copy keys/release.properties.example and fill it in.")
}

android {
    namespace = "com.casero.cu.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.casero.cu"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 3
        versionName = "2.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = rootProject.file("keys").resolve(releaseSigning.getProperty("storeFile"))
                storePassword = releaseSigning.getProperty("storePassword")
                keyAlias = releaseSigning.getProperty("keyAlias")
                keyPassword = releaseSigning.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 shrinks and obfuscates code and drops unused resources; check a release build on a
            // device after adding a library that uses reflection.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

// Outputs read casero-v<versionName>-<versionCode>-<debug|release>.apk (and .aab); AGP appends the build type.
base {
    archivesName.set(android.defaultConfig.let { "casero-v${it.versionName}-${it.versionCode}" })
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(kotlin("test"))
}
