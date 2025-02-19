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
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ldlywt.note"
        minSdk = 26
        targetSdk = 35
        versionCode = 205
        versionName = "2.0.5"
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

    hilt {
        enableExperimentalClasspathAggregation = true // 此行不加会遇到下面的新问题
        enableAggregatingTask = false
    }

    signingConfigs {
        kotlin.runCatching { System.getenv("KEYSTORE_PASSWORD") }.getOrNull()?.let {
            create("release") {
                storeFile = file("../keystore.jks")
                storePassword = it
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEYSTORE_PASSWORD")
            }
        }
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
    kotlinOptions {
        jvmTarget = "17"
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
    implementation(libs.androidx.biometric.ktx)

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

    // serialization
    implementation(libs.kotlinx.serialization.json)

    // Slf4j
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    implementation(libs.okhttp)
    implementation(libs.retrofit)

//    implementation("com.google.code.gson:gson:2.8.5")
//    implementation("com.squareup.okhttp3:okhttp:4.10.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

//    implementation("com.tencent.bugly:crashreport:4.1.9.1")
    implementation("org.zeroturnaround:zt-zip:1.15")
//    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("com.github.thegrizzlylabs:sardine-android:0.8")
    implementation("com.github.ireward:compose-html:1.0.2")
//    implementation("androidx.datastore:datastore-preferences:1.0.0")

//    implementation("com.caverock:androidsvg-aar:1.4")
//    implementation("io.coil-kt:coil:2.6.0")
//    implementation("io.coil-kt:coil-compose:2.6.0")
//    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.github.SmartToolFactory:Compose-Screenshot:1.0.3")
    implementation("com.kizitonwose.calendar:compose:2.6.0")
    implementation("io.github.moriafly:salt-ui:2.0.0")
    // Kotlin
    implementation("androidx.biometric:biometric:1.4.0-alpha02")

    var markwon_version = "4.6.2"

    implementation ("io.noties.markwon:core:$markwon_version")
    implementation ("io.noties.markwon:ext-strikethrough:$markwon_version")
    implementation ("io.noties.markwon:ext-tables:$markwon_version")
    implementation ("io.noties.markwon:html:$markwon_version")
    implementation ("io.noties.markwon:linkify:$markwon_version")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation ("com.github.jeziellago:Markwon:58aa5aba6a")

}