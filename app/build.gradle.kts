import java.io.File
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

val defaultVersionCode = providers.gradleProperty("VERSION_CODE").orNull?.toIntOrNull() ?: 1
val defaultVersionName = providers.gradleProperty("VERSION_NAME").orNull ?: "1.0.0"

android {
  namespace = "com.karyar.app"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.karyar.app"
    minSdk = 24
    targetSdk = 36
    versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: defaultVersionCode
    versionName = System.getenv("VERSION_NAME") ?: defaultVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables.useSupportLibrary = true
  }

  // Restore fixed project debug.keystore from base64 if present and not on disk
  val debugKeystoreFile = file("${rootDir}/debug.keystore")
  val debugKeystoreBase64 = file("${rootDir}/debug.keystore.base64")
  if (!debugKeystoreFile.exists() && debugKeystoreBase64.exists()) {
    try {
      val decoded = Base64.getDecoder().decode(debugKeystoreBase64.readText().trim())
      debugKeystoreFile.writeBytes(decoded)
    } catch (_: Exception) {}
  }

  // Check for release keystore
  val envKeystorePath = System.getenv("KEYSTORE_PATH")
  val releaseKeystoreCandidate = when {
    !envKeystorePath.isNullOrBlank() -> file(envKeystorePath)
    file("${rootDir}/my-upload-key.jks").exists() -> file("${rootDir}/my-upload-key.jks")
    file("${rootDir}/release.keystore").exists() -> file("${rootDir}/release.keystore")
    else -> null
  }
  val hasReleaseKeystore = releaseKeystoreCandidate != null && releaseKeystoreCandidate.exists()

  signingConfigs {
    if (hasReleaseKeystore) {
      create("release") {
        storeFile = releaseKeystoreCandidate
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
        enableV1Signing = true
        enableV2Signing = true
      }
    }
    if (debugKeystoreFile.exists()) {
      create("debugConfig") {
        storeFile = debugKeystoreFile
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
        enableV1Signing = true
        enableV2Signing = true
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (hasReleaseKeystore) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      val customDebug = signingConfigs.findByName("debugConfig")
      if (customDebug?.storeFile != null && customDebug.storeFile?.exists() == true) {
        signingConfig = customDebug
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }

  sourceSets {
    getByName("test").assets.srcDirs("$projectDir/schemas", "$projectDir/src/test/assets")
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

// Fail assembleRelease with clear message if no release signing keystore is configured
tasks.configureEach {
  if (name == "validateSigningRelease") {
    doFirst {
      val hasReleaseConfig = android.buildTypes.getByName("release").signingConfig != null
      if (!hasReleaseConfig) {
        throw org.gradle.api.GradleException(
          "کلید امضای ریلیز (Release Keystore) یافت نشد! برای ساخت assembleRelease معتبر، لطفاً متغیرهای KEYSTORE_PATH، STORE_PASSWORD، KEY_ALIAS و KEY_PASSWORD را تنظیم کنید."
        )
      }
    }
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)

  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)

  "ksp"(libs.androidx.room.compiler)
}
