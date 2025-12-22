# 字节 2025 工程训练营

因为含有子模块 submodule（llama.cpp 直接嵌入项目中）拉取项目请执行:

```
git clone --recurse-submodules https://github.com/SweetFlower2468/ByteDance.git
```

### Homework01

第一次作业，创建好第一个 Android 项目

### Homework02

第二次作业，完成两个页面（登录页和用户页面）
主要功能实现：

1. 登录页
   - 用户名和密码输入框
   - 登录功能
   - 注册功能
   - 找回密码
2. 用户页面
   - 显示用户信息（如用户名、头像等）
   - 退出登录按钮

### Homework03

第三次作业，完成高德天气 API 的调用
调用高德天气 API，获取当前城市的天气信息和未来几天的天气信息
获取当前位置并根据位置初始化天气信息

### FinalHomework

第四次作业，使用端侧模型、远端模型实现一个 AI 对话功能
并且能够实现左右手识别优化用户体验

#### 启动方式

##### 环境要求

- Android Studio (推荐最新版本)
- Android SDK API 36 (Android 15)
- 建议使用 JDK 17
- NDK (仅 FinalHomework 需要，用于 C++代码编译)
- CMake (仅 FinalHomework 需要，用于构建 C++代码)

##### 启动步骤

###### 方法一：使用 Android Studio 打开（推荐）

1. 启动 Android Studio
2. 选择"Open an existing Android Studio project"
3. 导航到项目根目录，选择要打开的具体作业目录：
   - `TrainingCamp/Homework01` - 第一次作业
   - `TrainingCamp/Homework02` - 第二次作业
   - `TrainingCamp/Homework03` - 第三次作业
   - `TrainingCamp/FinalHomework` - 最终大作业
4. 等待 Gradle 同步完成
   - 请注意，最后一次的大作业需要配置 Cmake 和 Vulkan SDK，如果实在编译不了，请将`/FinalHomework/app/src/main/cpp/CMakeLists.txt`的 option(GGML_VULKAN "llama: use Vulkan" ON)，将 ON 改为 OFF，调用手机 CPU 进行推理
5. 连接 Android 设备或启动模拟器
6. 点击绿色的"Run"按钮或按`Shift+F10`运行应用

###### 方法二：命令行构建

1. 打开终端或命令提示符
2. 导航到具体作业目录（例如 FinalHomework）：
   ```
   cd TrainingCamp\FinalHomework
   ```
3. 在 Windows 上执行构建命令：
   ```
   .\gradlew assembleDebug
   ```
   或在 Mac/Linux 上：
   ```
   ./gradlew assembleDebug
   ```
4. 构建完成后，APK 文件将在以下路径生成：
   ```
   app\build\outputs\apk\debug\app-debug.apk
   ```
5. 将 APK 安装到设备上进行测试

###### FinalHomework 特殊注意事项

由于 FinalHomework 集成了 C++代码和机器学习模型，可能需要额外配置：

1. 确保已经安装了 NDK 和 CMake：

   - 在 Android Studio 中打开 SDK Manager
   - 转到 SDK Tools 标签页
   - 确保安装了 NDK (Side by side) 和 CMake

2. 本地模型文件：

   - 应用需要 gguf 格式的 LLaMA 模型文件才能进行本地推理
   - 模型文件应放置在设备的`files/models`目录下
   - 可以通过应用内的模型管理界面导入模型文件
   - 也可以打开模型管理页面输入 huggingface 模型地址下载模型

3. 权限要求：
   - 网络访问权限（用于远程 API 调用）
   - 存储读写权限（用于模型文件管理）
   - 位置权限（用于获取当前位置）
   - 传感器权限（用于左手/右手识别）
