group = "io.github.kubrainy.sysmondax_widgets"
version = "1.0-SNAPSHOT"

buildscript {
    val kotlinVersion = "2.4.0"
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:9.1.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.kubrainy.sysmondax_widgets"

    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            java.srcDirs("src/test/kotlin")
        }
    }

    defaultConfig {
        minSdk = 24
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()

                it.outputs.upToDateWhen { false }

                it.testLogging {
                    events("passed", "skipped", "failed", "standardOut", "standardError")
                    showStandardStreams = true
                }
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.mockito:mockito-core:5.0.0")

    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // home_widget'ın native (RemoteViews köprüsü) sınıflarını kullanıyoruz
    // (HomeWidgetProvider, HomeWidgetPlugin, HomeWidgetBackgroundIntent).
    // Bu proje bir Flutter uygulamasına dahil edildiğinde, uygulama zaten
    // `home_widget` paketine bağımlı olacağından (pubspec.yaml), Flutter'ın
    // plugin loader'ı ":home_widget" alt projesini otomatik olarak dahil
    // eder; burada ona referans veriyoruz.
    rootProject.findProject(":home_widget")?.let { implementation(it) }
}
