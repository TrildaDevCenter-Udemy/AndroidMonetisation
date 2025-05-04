plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.parcelize)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.trildadevcenter.androidmonetization"
    compileSdk = 35
    defaultConfig {
        applicationId = "io.github.trildadevcenter.androidmonetization"
        minSdk = 24
        targetSdk = 35
        versionCode  = 3
        versionName = "1.3.0"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release")  {
            // Enables code shrinking, obfuscation, and optimization for only
            // your project's release build type. Make sure to use a build
            // variant with `isDebuggable=false`.
            isMinifyEnabled = true

            // Enables resource shrinking, which is performed by the
            // Android Gradle plugin.
            isShrinkResources = true

            // Includes the default ProGuard rules files that are packaged with
            // the Android Gradle plugin. To learn more, go to the section about
            // R8 configuration files.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("debug") {
            //applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    var javaVersion = JavaVersion.VERSION_17

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlin {
        jvmToolchain(javaVersion.majorVersion.toInt())
    }

    buildFeatures {
        aidl = false
        buildConfig = true
        compose = false
        prefab = false
        renderScript = false
        resValues = true
        shaders = true
        viewBinding = true
        dataBinding = false
    }

    // Specifies one flavor dimension.
    flavorDimensions += "pricing"

    productFlavors {

        create("free") {
            dimension = "pricing"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
        }

        create("paid") {
            dimension = "pricing"
            applicationIdSuffix = ".paid"
            versionNameSuffix = "-paid"
        }
    }

    packagingOptions.resources {
        // The Rome library JARs embed some internal utils libraries in nested JARs.
        // We don't need them so we exclude them in the final package.
        //excludes += "/*.jar"

        // Multiple dependency bring these files in. Exclude them to enable
        // our test APK to build (has no effect on our AARs)
        excludes.add("/META-INF/AL2.0")
        excludes.add("/META-INF/LGPL2.1")
    }
}

dependencies {

    // work manager (workers)
    // this solves on recent Android  the crash signaled in Logcat with
    // Targeting S+ (version 31 and above) requires that one of FLAG_IMMUTABLE or FLAG_MUTABLE be specified
    // when creating a PendingIntent. Strongly consider using FLAG_IMMUTABLE, only use FLAG_MUTABLE
    // if some functionality depends on the PendingIntent being mutable, e.g. if it needs to be used
    // with inline replies or bubbles.
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.test.ext)
    implementation(libs.androidx.tools.core)
    implementation(libs.androidx.ui.viewbinding)

    implementation (libs.androidx.lifecycle.viewmodel.ktx)
    implementation (libs.androidx.lifecycle.reactivestreams.ktx)
    implementation (libs.androidx.lifecycle.viewmodel.savedstate)
    implementation (libs.androidx.lifecycle.process)

    androidTestImplementation(libs.androidx.work.testing)
    implementation (libs.androidx.work.mutiprocess)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // compose layouts
    // compose constraints layout
    implementation (libs.androidx.constraintlayout.core)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.swippe.refresh.layout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.recyclerview.selection)

    // Retrofit
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson.converter)

    // GSON
    implementation (libs.gson)

    // Logging
    implementation (libs.timber)

    // RxJava RxKotlin RxAndroid
    implementation(libs.rx.java)
    implementation(libs.rx.android)
    implementation(libs.rx.kotlin)

    // Retrofit
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson.converter)
    implementation (libs.retrofit.rx.java3.adapter)

    // Glide
    implementation(libs.glide.core)
    implementation(libs.glide.recyclerview.integration)

    // AdMobs
    implementation(libs.play.services.ads)

    // User messaging platform , to get user  consent
    implementation(libs.user.messaging.platform)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Billing
    implementation(libs.billing.client)
    implementation(libs.billing.ktx.client)

    // coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)


    testImplementation(libs.junit4)
    androidTestImplementation (libs.androidx.test.runner)
    androidTestImplementation (libs.androidx.test.espresso.core)
}
