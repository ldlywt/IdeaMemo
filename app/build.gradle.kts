
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    id("kotlin-parcelize") // Add this line
}

android {
    namespace = "com.ldlywt.note"
    compileSdk = 36

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    defaultConfig {
        applicationId = "com.ldlywt.note"
        minSdk = 26
        targetSdk = 36
        versionCode = 313
        versionName = "3.1.3"
        vectorDrawables {
            useSupportLibrary = true
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {

            //加载资源
            val properties = Properties()
            val inputStream = project.rootProject.file("local.properties").inputStream()
            properties.load(inputStream)

            //读取文件
            val sdkDir = properties.getProperty("key.file")
            storeFile = file(sdkDir)

            //读取字段
            val key_keyAlias = properties.getProperty("keyAlias")
            val key_keyPassword = properties.getProperty("keyPassword")
            val key_storePassword = properties.getProperty("storePassword")

            storePassword = key_storePassword
            keyAlias = key_keyAlias
            keyPassword = key_keyPassword

            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    hilt {
        enableExperimentalClasspathAggregation = true // 此行不加会遇到下面的新问题
        enableAggregatingTask = false
    }

    buildTypes {
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isShrinkResources = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }

    }
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "IdeaMemo-${variant.baseName}-${variant.versionName}.apk"
                println("OutputFileName: $outputFileName")
                output.outputFileName = outputFileName
            }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation.layout.android)

    implementation(libs.androidx.datastore.preferences)

    // icons
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.material)

    // navigation component
    implementation(libs.androidx.navigation.compose)

    // jsoup
    implementation(libs.jsoup)

    // hilt
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.android)

    // hilt navigation
    implementation(libs.androidx.hilt.navigation.compose)

    // coil
    implementation(libs.coil.compose)

    // room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // paging compose
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.paging.compose.android)

    // splash screen
    implementation(libs.androidx.splashscreen)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.client.mock)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)

    // serialization
    implementation(libs.kotlinx.serialization.json)

    // Slf4j
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    implementation(libs.okhttp)
    implementation(libs.retrofit)

    implementation("org.zeroturnaround:zt-zip:1.15")
    implementation("com.github.thegrizzlylabs:sardine-android:0.8")

    implementation("com.kizitonwose.calendar:compose:2.6.0")
    implementation("io.github.moriafly:salt-ui:2.0.0")
    // Kotlin
    implementation("androidx.biometric:biometric:1.4.0-alpha02")
    // Kotlin
    implementation("androidx.biometric:biometric-ktx:1.4.0-alpha02")

    val markwon_version = "4.6.2"

    implementation("io.noties.markwon:core:$markwon_version")
    implementation("io.noties.markwon:ext-strikethrough:$markwon_version")
    implementation("io.noties.markwon:ext-tables:$markwon_version")
    implementation("io.noties.markwon:html:$markwon_version")
    implementation("io.noties.markwon:linkify:$markwon_version")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.github.jeziellago:Markwon:58aa5aba6a")
}
