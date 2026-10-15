import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.aquagrow"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aquagrow"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Each UI test starts from a freshly installed app (empty database, no onboarding).
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        // Assistant backend URL: local.properties (assistant.baseUrl) wins over assistant.properties (baseUrl).
        fun props(name: String) = Properties().apply {
            providers.fileContents(rootProject.layout.projectDirectory.file(name)).asText.orNull?.let { load(it.reader()) }
        }
        val assistantBaseUrl = props("local.properties").getProperty("assistant.baseUrl")
            ?: props("assistant.properties").getProperty("baseUrl")
            ?: ""
        buildConfigField("String", "ASSISTANT_BASE_URL", "\"${assistantBaseUrl.trim()}\"")
    }

    // Release signing comes from keystore.properties in the project root, which is never committed:
    //   storeFile=/path/to/aquagrow.jks
    //   storePassword=...
    //   keyAlias=aquagrow
    //   keyPassword=...
    // Without it, bundleRelease produces an unsigned bundle (sign it before uploading).
    val keystoreProps = Properties().apply {
        providers.fileContents(rootProject.layout.projectDirectory.file("keystore.properties")).asText.orNull?.let { load(it.reader()) }
    }
    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
                // Local testing of the minified build only: ./gradlew assembleRelease -PsignReleaseWithDebugKey
                ?: if (providers.gradleProperty("signReleaseWithDebugKey").isPresent) signingConfigs.getByName("debug") else null
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    // Exported schemas are kept in git so future migrations can be written and tested.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.coil.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.rules)
    // Compose test pulls Espresso 3.5, which crashes on Android 16 (API 36); 3.7 fixes it.
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestUtil(libs.androidx.test.orchestrator)
}
