package com.wuzhanning.nfctagfather

import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.*
import java.time.Instant

internal object CardScanner {
    fun read(tag: Tag): CardRecord {
        val details = linkedMapOf<String, String>()
        val technologies = tag.techList.map { it.substringAfterLast('.') }
        details["识别类型"] = when {
            "MifareClassic" in technologies -> "MIFARE Classic（具体用途未验证）"
            "MifareUltralight" in technologies -> "MIFARE Ultralight 系列"
            "IsoDep" in technologies -> "ISO 14443-4 智能卡（具体产品未验证）"
            else -> technologies.joinToString(" / ")
        }
        NfcA.get(tag)?.let { details["ATQA"] = it.atqa.hex(); details["SAK"] = "%04X".format(it.sak.toInt() and 0xffff) }
        NfcB.get(tag)?.let { details["应用数据"] = it.applicationData.hex(); details["协议信息"] = it.protocolInfo.hex() }
        IsoDep.get(tag)?.let { iso ->
            iso.historicalBytes?.let { details["Historical bytes"] = it.hex() }
            iso.hiLayerResponse?.let { details["Higher layer response"] = it.hex() }
        }
        MifareClassic.get(tag)?.let { details["容量（字节）"] = it.size.toString(); details["扇区数"] = it.sectorCount.toString() }
        val ndef = Ndef.get(tag)
        var status = "Android 未检测到 NDEF。该卡可能使用专有或受保护的数据格式；没有读取门禁凭据。"
        var records = emptyList<PublicNdefRecord>()
        if (ndef != null) {
            try {
                ndef.connect()
                details["NDEF 类型"] = ndef.type
                details["NDEF 容量（字节）"] = ndef.maxSize.toString()
                details["NDEF 可写"] = if (ndef.isWritable) "是（本应用只读）" else "否"
                val message = ndef.ndefMessage
                records = message?.records?.map { r ->
                    val text = if (r.tnf == NdefRecord.TNF_WELL_KNOWN && r.type.contentEquals(NdefRecord.RTD_TEXT)) {
                        NdefText.decode(r.payload)
                    } else r.toUri()?.toString()
                    PublicNdefRecord(r.tnf.toInt(), r.type.hex(), r.id.hex(), r.payload.hex(), text)
                }.orEmpty()
                status = if (records.isEmpty()) "检测到 NDEF，但没有记录。" else "已读取 ${records.size} 条公开 NDEF 记录。"
            } catch (e: Exception) {
                status = "NDEF 读取未完成（${e.javaClass.simpleName}）。请保持卡片贴合后重试；元数据已保留。"
            } finally {
                try { ndef.close() } catch (_: Exception) { }
            }
        }
        return CardRecord(scannedAt = Instant.now().toString(), uidHex = tag.id.hex(), technologies = technologies,
            details = details, ndefStatus = status, records = records)
    }
}
