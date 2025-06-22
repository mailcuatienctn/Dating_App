// build.gradle.kts (Module: app)

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.datingapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.datingapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    // AndroidX UI Components - Đã cập nhật lên phiên bản mới nhất ổn định và loại bỏ trùng lặp
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    // Sử dụng phiên bản mới nhất cho activity-ktx, đảm bảo không trùng lặp với 'libs.activity'
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.13.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.github.imperiumlabs:GeoFirestore-Android:v1.5.0")

    // Image Picker
    implementation("io.github.ParkSangGwon:tedimagepicker:1.6.1") {
        exclude(group = "com.android.support")
    }

    // Image Loading - Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.activity)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.squareup.picasso:picasso:2.71828") // Phiên bản mới nhất


    // Cloudinary
    implementation("com.cloudinary:cloudinary-android:3.0.2")

    // Navigation Components
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // CardStackView - Đã chuyển sang phiên bản 2.3.0 phổ biến hơn
    implementation ("com.github.yuyakaido:CardStackView:v2.3.4")

}