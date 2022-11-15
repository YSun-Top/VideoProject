plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    val androidBuildTools: Map<*, *> = rootProject.ext["androidBuildTools"] as Map<*, *>
    compileSdk = androidBuildTools["compileSdk"] as Int

    defaultConfig {
        minSdk = androidBuildTools["minSdk"] as Int
        targetSdk = androidBuildTools["targetSdk"] as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    val androidx: Map<*, *> = rootProject.ext["androidx"] as Map<*, *>
    val googleCode: Map<*, *> = rootProject.ext["googleCode"] as Map<*, *>
    api("androidx.core:core-ktx:${androidx["core_ktx"]}")
    api("androidx.appcompat:appcompat:${androidx["appcompat"]}")
    api("androidx.test:monitor:${androidx["monitor"]}")
    api("androidx.test.ext:junit-ktx:${androidx["ext_junit_ktx"]}")
    api("androidx.test.ext:junit:${androidx["ext_junit"]}")
    api("androidx.test.espresso:espresso-core:${androidx["espresso_core"]}")
    api("junit:junit:${androidx["test_junit"]}")
    api("com.google.android.material:material:${googleCode["material"]}")
}