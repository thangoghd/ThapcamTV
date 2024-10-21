plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.thangoghd.thapcamtv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.thangoghd.thapcamtv"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.androidx.leanback)
    implementation(libs.glide)
}