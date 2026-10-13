# 验证状态与真机测试

## 已通过

- Debug APK 构建（版本 `0.2.0`）及 APK 签名验证。
- 3 个单元测试：NDEF UTF-8/UTF-16 文本解析、异常输入、JSON 往返及原始字节保留。
- Android Lint：没有错误，1 条测试依赖存在更新版本的提示。
- 云环境安装脚本复跑成功。

## 尚未验证

- 真实 NFC 卡片的识别和读取。
- 保存、重新打开和文件导出的设备端完整流程。

云端安装了 API 30 模拟器，但宿主没有 KVM。软件模拟启动期间 Android 系统服务崩溃，出现 `DeadSystemException`；未完成应用界面测试。此错误发生在 Android 系统中，不能作为应用功能已通过的证据。

本云会话的 adb 无法访问连接到用户 Mac 的 USB 真机。授权使用真机后，仍需在 Mac 本地执行任务，或由用户在 Mac 安装并反馈结果。

## Mac 真机测试步骤

手机开启 USB 调试并确认电脑授权，确保 `adb devices -l` 中显示目标手机的状态为 `device`。若存在多个设备，请为 adb 命令添加 `-s 手机序列号`。

```bash
adb devices -l
adb install -r ~/Downloads/NFCTagFather-v0.2.0-debug.apk
adb shell am start -n com.wuzhanning.nfctagfather/.MainActivity
```

1. 启用 NFC，点击“开始扫描”，让卡片保持贴近 NFC 感应区域。
2. 检查 UID、技术类型和读取说明；没有 NDEF 的卡片应清楚说明未获得完整卡片内容。
3. 保存记录，命名为“Pullman 我的房卡”，确认“已保存到本机”。
4. 关闭并重新打开 App，从“已保存记录”打开该记录，确认名称、UID 和原始数据一致。
5. 导出 JSON，选择下载目录。检查文件中的 `schemaVersion` 为 `1`，`label` 和 `uidHex` 与 App 一致。
6. 将 JSON 经 Mac AirDrop 到 iPhone，用支持 JSON/文本的程序查看。文件仅用于查看和存档，不是门钥匙。
7. 再次导出时取消系统保存对话框，确认可以重新导出；停止扫描或切换到其他 App 后不应继续扫描。

如果崩溃，收集目标 App 的日志再定位问题：

```bash
adb logcat -b crash -d
```
