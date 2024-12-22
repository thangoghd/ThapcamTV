plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.thangoghd.thapcamtv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.thangoghd.thapcamtv"
        minSdk = 24
        targetSdk = 34
        versionCode = 5
        versionName = "1.0.4"

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
    // https://mvnrepository.com/artifact/com.squareup.retrofit2/retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // https://mvnrepository.com/artifact/com.squareup.retrofit2/converter-gson
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // https://mvnrepository.com/artifact/com.squareup.okhttp3/logging-interceptor
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.14")
    // AndroidX Leanback for TV UI components
    implementation ("androidx.leanback:leanback:1.0.0")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.11.0")
    // https://mvnrepository.com/artifact/com.google.android.exoplayer/exoplayer
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // https://mvnrepository.com/artifact/androidx.tvprovider/tvprovider
    implementation("androidx.tvprovider:tvprovider:1.0.0")
    implementation(libs.video)
}