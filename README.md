# NFCTagFather

使用 Kotlin 开发的原生 Android NFC 应用。

## 当前状态

初始版本包含启动页面和 NFC 状态提示（已启用、已关闭、不支持）。NFC 标签读写功能尚未实现。

## 开发

默认分支为 `main`，应用 ID 为 `com.wuzhanning.nfctagfather`，最低支持 Android 8.0（API 26）。

所需工具：JDK 17 或 21、Android SDK Platform 36、Build Tools 36.0.0。项目使用 Gradle 8.13、Android Gradle Plugin 8.11.1、Kotlin 2.1.21；Gradle Wrapper 已固定下载校验值。

在 Android Studio 中打开项目，或设置 `ANDROID_HOME` 指向本机 SDK 后运行：

```bash
./gradlew :app:assembleDebug :app:lintDebug
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`，Lint 报告位于 `app/build/reports/lint-results-debug.html`。

连接 Android 设备并启用 USB 调试后，可以安装并启动：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.wuzhanning.nfctagfather/.MainActivity
```

项目目前未包含单元测试或设备测试。构建和 Lint 检查不能替代设备上的运行验证；后续 NFC 标签读写必须使用支持 NFC 的真机测试。

## Codex 云环境

在此云环境的 Bash 终端中运行：

```bash
source /workspace/toolchains/android-env.sh
cd /workspace/NFCTagFather
./gradlew --no-daemon --console=plain :app:assembleDebug :app:lintDebug
```

SDK、依赖缓存和机器配置位于仓库外，不提交到 Git。可复用安装和启动说明保存在云环境配置中。
