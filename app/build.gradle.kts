import java.util.Properties

// --- Cargar local.properties desde la raíz del proyecto ---
val localProperties = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) {
    localFile.inputStream().use { localProperties.load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.chifuzgames.catibox"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    defaultConfig {
        minSdk = 23
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // --- Flavors ---
    flavorDimensions += listOf("environment")
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationId = "com.chifuzgames.catibox.dev"
            versionCode = 1
            versionName = "1.0-dev"

            // IDs desde local.properties (modo pruebas)
            val bannerId = localProperties.getProperty("BANNER_ID", "")
            val admobAppId = localProperties.getProperty("ADMOB_APP_ID", "")
            val interstitialId = localProperties.getProperty("INTERSTITIAL_ID", "")
            val rewardedId = localProperties.getProperty("REWARDED_ID", "")

            // Exportar a recursos y BuildConfig
            resValue("string", "admob_app_id", admobAppId)
            buildConfigField("String", "BANNER_ID", "\"$bannerId\"")
            buildConfigField("String", "INTERSTITIAL_ID", "\"$interstitialId\"")
            buildConfigField("String", "REWARDED_ID", "\"$rewardedId\"")
        }

        create("prod") {
            dimension = "environment"
            applicationId = "com.chifuzgames.catibox"
            versionCode = 22
            versionName = "2.2"

            // IDs desde local.properties (modo producción)
            val bannerId = localProperties.getProperty("BANNER_ID", "")
            val admobAppId = localProperties.getProperty("ADMOB_APP_ID", "")
            val interstitialId = localProperties.getProperty("INTERSTITIAL_ID", "")
            val rewardedId = localProperties.getProperty("REWARDED_ID", "")

            resValue("string", "admob_app_id", admobAppId)
            buildConfigField("String", "BANNER_ID", "\"$bannerId\"")
            buildConfigField("String", "INTERSTITIAL_ID", "\"$interstitialId\"")
            buildConfigField("String", "REWARDED_ID", "\"$rewardedId\"")
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
        debug {
            versionNameSuffix = "-dev"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.google.android.gms:play-services-ads:24.5.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
}
