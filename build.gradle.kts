plugins {
    // Cập nhật plugin Android Gradle với phiên bản mới nhất
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}
// Cấu hình cho việc sử dụng Android Gradle plugin mới nhất
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Cập nhật phiên bản Android Gradle plugin lên 8.6.0 hoặc phiên bản cao hơn
        classpath("com.android.tools.build:gradle:8.6.0")

        // Đảm bảo bạn sử dụng phiên bản Kotlin mới nhất tương thích
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
    }
}
