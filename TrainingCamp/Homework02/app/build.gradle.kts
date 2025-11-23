plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.edu.neu.homework02"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.edu.neu.homework02"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // 基础 AndroidX 组件 (对应 PDF 课程内容)
    implementation(libs.recyclerview)
    // Gson: 用于将对象转为 JSON 字符串存入 SP (比单纯存散乱字段更高效)
    implementation(libs.gson)
    // Glide: 如果需要加载 Base64 图片或处理圆角头像
    implementation(libs.glide)
}