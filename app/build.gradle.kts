plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.thangoghd.thapcamtv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.thangoghd.thapcamtv"
        minSdk = 24
        targetSdk = 34
        versionCode = 16
        versionName = "1.2.7"

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
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.11.0")
    // https://mvnrepository.com/artifact/com.google.android.exoplayer/exoplayer
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    // https://mvnrepository.com/artifact/com.google.android.exoplayer/exoplayer-ui
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // https://mvnrepository.com/artifact/androidx.tvprovider/tvprovider
    implementation("androidx.tvprovider:tvprovider:1.0.0")
    // https://mvnrepository.com/artifact/com.google.firebase/firebase-config
    implementation("com.google.firebase:firebase-config:22.0.1")
    implementation(libs.video)
    // Add Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics")
}