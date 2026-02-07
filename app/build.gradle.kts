plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ugelcorongo.edugestin360"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ugelcorongo.edugestin360"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
    implementation(libs.okhttp)
    implementation(libs.volley)
    implementation(libs.fragment)

    implementation(libs.gson)

    annotationProcessor(libs.room.compiler)
    implementation(libs.room.runtime)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}