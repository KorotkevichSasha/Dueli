import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-parcelize")
}

val buildingRelease = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }
val apiBaseUrl = providers.gradleProperty("API_BASE_URL")
    .orElse(if (buildingRelease) "" else "http://127.0.0.1:8082")
    .get()
val applicationIdValue = providers.gradleProperty("APPLICATION_ID")
    .orElse("com.duelrush.app")
    .get()
val privacyPolicyUrl = providers.gradleProperty("PRIVACY_POLICY_URL")
    .orElse(if (buildingRelease) "" else "http://127.0.0.1:5173/privacy")
    .get()
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

if (buildingRelease) {
    require(apiBaseUrl.startsWith("https://") && !apiBaseUrl.contains("example.com")) {
        "Release builds require -PAPI_BASE_URL=https://your-production-api"
    }
    require(!applicationIdValue.startsWith("com.example.")) {
        "Release builds require the permanent package id com.duelrush.app"
    }
    require(keystorePropertiesFile.exists()) {
        "Release builds require keystore.properties; copy keystore.properties.example and add the upload key"
    }
    require(privacyPolicyUrl.startsWith("https://")) {
        "Release builds require -PPRIVACY_POLICY_URL=https://your-public-site/privacy"
    }
}

android {
    namespace = "com.example.duelingo"
    compileSdk = 36

    defaultConfig {
        applicationId = applicationIdValue
        minSdk = 24
        targetSdk = 36
        versionCode = providers.gradleProperty("VERSION_CODE").orElse("6").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").orElse("1.0.0").get()
        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.trimEnd('/')}\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"$privacyPolicyUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions.jvmTarget = "17"

    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // Glide includes NotificationTarget, but this app never posts notifications.
        disable += "NotificationPermission"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.material)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("com.airbnb.android:lottie:6.6.2")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
