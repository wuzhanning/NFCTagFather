package com.wuzhanning.nfctagfather

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

internal fun ByteArray.hex(): String = joinToString("") { "%02X".format(it.toInt() and 0xff) }

internal data class PublicNdefRecord(
    val tnf: Int, val typeHex: String, val idHex: String, val payloadHex: String, val text: String?,
) {
    fun json() = JSONObject().put("tnf", tnf).put("typeHex", typeHex).put("idHex", idHex)
        .put("payloadHex", payloadHex).put("text", text ?: JSONObject.NULL)
}

internal data class CardRecord(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "未命名卡片",
    val scannedAt: String,
    val uidHex: String,
    val technologies: List<String>,
    val details: Map<String, String>,
    val ndefStatus: String,
    val records: List<PublicNdefRecord>,
) {
    fun json() = JSONObject().put("schemaVersion", 1).put("id", id).put("label", label)
        .put("scannedAt", scannedAt).put("uidHex", uidHex).put("technologies", JSONArray(technologies))
        .put("details", JSONObject(details)).put("ndefStatus", ndefStatus)
        .put("ndefRecords", JSONArray().also { a -> records.forEach { a.put(it.json()) } })
        .put("notice", "仅包含可访问的卡片信息，不包含完整门禁凭据，不能用于模拟房卡或开门。")

    companion object {
        fun parse(value: String): CardRecord {
            val j = JSONObject(value)
            require(j.getInt("schemaVersion") == 1) { "不支持的记录版本" }
            val tech = j.getJSONArray("technologies")
            val d = j.getJSONObject("details")
            val a = j.getJSONArray("ndefRecords")
            return CardRecord(j.getString("id"), j.getString("label"), j.getString("scannedAt"),
                j.getString("uidHex"), List(tech.length()) { tech.getString(it) },
                d.keys().asSequence().associateWith { d.getString(it) }, j.getString("ndefStatus"),
                List(a.length()) { index ->
                    val r = a.getJSONObject(index)
                    PublicNdefRecord(r.getInt("tnf"), r.getString("typeHex"), r.getString("idHex"),
                        r.getString("payloadHex"), if (r.isNull("text")) null else r.getString("text"))
                })
        }
    }
}

internal object NdefText {
    fun decode(payload: ByteArray): String? {
        if (payload.isEmpty()) return null
        val status = payload[0].toInt() and 0xff
        if (status and 0x40 != 0) return null // Reserved bit must be zero.
        val start = 1 + (status and 0x3f)
        if (start > payload.size) return null
        val charset = if (status and 0x80 != 0) StandardCharsets.UTF_16 else StandardCharsets.UTF_8
        return String(payload, start, payload.size - start, charset)
    }
}
