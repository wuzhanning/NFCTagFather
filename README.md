# NFCTagFather

使用 Kotlin 开发的原生 Android NFC 应用。

## 当前状态

当前版本支持扫描识别、命名保存、查看历史记录和 JSON 导出。可读取 Android 提供的 UID、NFC 技术类型、卡片公开的协议元数据，以及可访问的 NDEF 记录（保留原始十六进制数据，并解析文本和 URI）。

本应用不写入卡片、不尝试受保护扇区的认证或读取，也不保存完整门禁凭据。UID 和公开数据不能用于复制房卡开门；导出的 JSON 不能添加为 Apple 钱包酒店钥匙。酒店名称由用户命名，不会通过 UID 判断。

## 使用

1. 安装 App，在手机设置中启用 NFC，点击“开始扫描”。
2. 将房卡贴近手机的 NFC 感应区域，保持不动。成功后自动停止扫描并显示结果。
3. 点击“保存当前记录”，为卡片命名，例如“Pullman 我的房卡”。记录保存在应用私有 SQLite 数据库中，不自动上传，卸载会删除。
4. 点击“已保存记录”重新打开保存的数据。
5. 点击“导出当前记录为 JSON”，通过系统文件选择器选择保存位置。可以将文件经 USB 传到 Mac，再通过 AirDrop 发送至 iPhone 的“文件”App，供查看和存档。

如只读到 UID 和技术类型、没有 NDEF，这通常表示卡片未公开 NDEF 数据，或使用专有、受保护格式，不代表获得了开门凭据。Android NFC 无法读取 125 kHz 等不支持的卡片。首次贴卡没有反应时，可调整感应位置，或停止再重新开始扫描。

## 开发

默认分支为 `main`，应用 ID 为 `com.wuzhanning.nfctagfather`，最低支持 Android 8.0（API 26）。

所需工具：JDK 17 或 21、Android SDK Platform 36、Build Tools 36.0.0。项目使用 Gradle 8.13、Android Gradle Plugin 8.11.1、Kotlin 2.1.21；Gradle Wrapper 已固定下载校验值。

在 Android Studio 中打开项目，或设置 `ANDROID_HOME` 指向本机 SDK 后运行：

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`，Lint 报告位于 `app/build/reports/lint-results-debug.html`。

连接 Android 设备并启用 USB 调试后，可以安装并启动：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.wuzhanning.nfctagfather/.MainActivity
```

单元测试覆盖 NDEF 文本编码、异常数据、JSON 往返和原始字节保留。模拟器可以验证界面、保存和文件导出，但真实房卡识别和读取必须在支持 NFC 的真机上测试。本云环境不能直接访问连接到用户 Mac 的 USB 手机。

## Codex 云环境

在此云环境的 Bash 终端中运行：

```bash
source /workspace/toolchains/android-env.sh
cd /workspace/NFCTagFather
./gradlew --no-daemon --console=plain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

SDK、依赖缓存和机器配置位于仓库外，不提交到 Git。可复用安装和启动说明保存在云环境配置中。
