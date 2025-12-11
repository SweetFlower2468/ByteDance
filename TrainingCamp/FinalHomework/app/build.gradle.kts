plugins {
    alias(libs.plugins.android.application)
    id("com.google.dagger.hilt.android") version "2.57.1"
}

android {
    namespace = "com.edu.neu.finalhomework"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.edu.neu.finalhomework"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Restrict to 64-bit to avoid 32-bit FP16 issues in llama.cpp
        ndk {
            abiFilters.add("arm64-v8a")
        }

        // 用于 Room 数据库 Schema 导出
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    // 忽略 Kotlin metadata 错误
                    "dagger.fastInit" to "enabled"
                )
            }
        }
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 配置编译选项以处理 Kotlin metadata 兼容性
        multiDexEnabled = true
        
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
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
    
    // 配置 Java 编译器选项，处理 Kotlin metadata 兼容性问题
    // 这些错误通常来自 Kotlin 库的 metadata，不影响实际编译
    // 注意：这些是注解处理器产生的警告，不应该阻止编译
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf(
            "-Xmaxerrs", "1000",
            "-Xmaxwarns", "1000",
            "-parameters"
        ))
        // 启用增量编译
        options.isIncremental = true
    }

    // 开启 ViewBinding (企业级开发必备，替代 findViewById)
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

hilt {
    enableAggregatingTask = false
}

configurations.all {
    resolutionStrategy {
        // 强制使用兼容的 Kotlin 版本
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24")
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 1. 基础 UI 与架构组件
    implementation ("androidx.core:core:1.12.0") // 用于 WindowInsetsCompat
    implementation ("androidx.appcompat:appcompat:1.6.1")
    implementation ("com.google.android.material:material:1.10.0") // Material 3 扁平化设计核心
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")

    // Lifecycle & ViewModel (MVVM 核心)
    implementation ("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation ("androidx.lifecycle:lifecycle-livedata:2.6.2")
    // Java 17 支持需开启
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:2.0.3")
    
    // 添加 Kotlin 标准库以支持 Kotlin metadata（即使项目是纯 Java，某些依赖需要它）
    // 使用与 Android Gradle Plugin 和 Hilt 2.51.1 兼容的 Kotlin 版本
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24")

    // 2. 本地数据库 (Room) - 更新到 2.7.0 以解决 javapoet 兼容性问题
    val roomVersion = "2.7.0"
    implementation ("androidx.room:room-runtime:$roomVersion")
    annotationProcessor ("androidx.room:room-compiler:$roomVersion")
    implementation ("androidx.room:room-rxjava3:$roomVersion")
    
    // 依赖注入 (Hilt) - 更新到 2.51.1 以解决 javapoet 兼容性问题
    val hiltVersion = "2.57.1"
    implementation ("com.google.dagger:hilt-android:$hiltVersion")
    annotationProcessor ("com.google.dagger:hilt-compiler:$hiltVersion")

    // 3. 网络与流式处理 (Retrofit + OkHttp + RxJava)
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.retrofit2:adapter-rxjava3:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.11.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation ("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation ("io.reactivex.rxjava3:rxjava:3.1.8")

    // 4. 前端渲染增强
    // Markdown 渲染 AI 回复
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:image:4.6.2")
    implementation("io.noties.markwon:image-glide:4.6.2") // Glide 图片插件
    implementation("io.noties.markwon:linkify:4.6.2")
    implementation("io.noties.markwon:ext-latex:4.6.2") // 公式渲染
    implementation("io.noties.markwon:inline-parser:4.6.2") // 支持 $...$ 内联解析
    implementation("ru.noties:jlatexmath-android:0.2.0") // LaTeX 渲染引擎
    implementation ("com.github.bumptech.glide:glide:4.16.0") // 图片加载

    // 5. 进阶功能支持
    implementation ("org.tensorflow:tensorflow-lite:2.14.0") // 左右手识别模型运行
    // 动画库 (用于发送按钮平滑移动)
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    // 字节跳动火山引擎 SDK (需自行导入 jar/aar)
    implementation("com.volcengine:volcengine-java-sdk-ark-runtime:0.2.50")
    // PDF text extraction
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}