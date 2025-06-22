// build.gradle.kts (Project: DatingApp)

plugins {
    // Đây là plugin Android Application, không áp dụng cho tất cả sub-project (apply false)
    alias(libs.plugins.android.application) apply false
    // Plugin Google Services, cũng không áp dụng cho tất cả sub-project (apply false)
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// Khối buildscript chỉ định các kho lưu trữ và dependencies cho chính các plugin Gradle
buildscript {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
    dependencies {
        // Có thể có các dependencies cho plugin ở đây, ví dụ: classpath("com.android.tools.build:gradle:8.x.x")
    }
}

// Khối allprojects repositories ĐÃ BỊ LOẠI BỎ vì đã được quản lý tập trung trong settings.gradle.kts
/*
allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}
*/