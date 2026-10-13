# JSON 导出格式（版本 1）

导出是一份卡片可访问信息的记录文件，供查看和存档；不是门钥匙、Apple 钱包凭证或可回写的卡片镜像。App 当前没有 JSON 导入功能。

| 字段 | 含义 |
| --- | --- |
| `schemaVersion` | 当前为 `1` |
| `id` | 本次扫描记录的唯一标识，重新保存同一记录会更新它 |
| `label` | 用户填写的卡片名称，不表示已验证酒店身份 |
| `scannedAt` | 扫描时间，ISO 8601 格式，UTC |
| `uidHex` | Android 提供的 UID，以原始字节顺序显示的大写十六进制字符串；可能为空或随卡片变化 |
| `technologies` | Android 检测到的 NFC 技术类型列表 |
| `details` | 可直接获得的协议元数据；字段取决于卡片及手机支持情况 |
| `ndefStatus` | NDEF 读取结果或失败说明 |
| `ndefRecords` | 可访问的 NDEF 记录列表，没有读到记录时为空 |
| `notice` | 文件用途及限制说明 |

每条 NDEF 记录包含 `tnf`、`typeHex`、`idHex`、`payloadHex` 和 `text`。原始二进制数据通过十六进制完整保留；`text` 是解析后的 NDEF 文本或 URI，未支持的内容格式为 `null`。App 不会访问记录中的网址。

没有 NDEF 数据，并不说明卡片为空或已经完整读取。对于使用专有或受保护格式的房卡，文件通常只有 UID、技术类型和公开元数据。

如需在 iPhone 保存，可以先把 JSON 文件传到 Mac，再通过 AirDrop 保存到 iPhone 的“文件”App。查看 JSON 内容需要支持文本或 JSON 的查看程序；该文件不能导入为 Apple 钱包房卡。
