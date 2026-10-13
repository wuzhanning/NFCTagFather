package com.wuzhanning.nfctagfather

import org.junit.Assert.*
import org.junit.Test

class CardRecordTest {
    @Test fun textRecordsRespectLanguageAndEncoding() {
        assertEquals("房卡", NdefText.decode(byteArrayOf(2) + "zh".toByteArray() + "房卡".toByteArray()))
        assertEquals("Room 42", NdefText.decode(byteArrayOf(0x82.toByte()) + "en".toByteArray() + "Room 42".toByteArray(Charsets.UTF_16)))
        assertNull(NdefText.decode(byteArrayOf()))
        assertNull(NdefText.decode(byteArrayOf(5, 1)))
        assertNull(NdefText.decode(byteArrayOf(0x40)))
    }

    @Test fun exportRoundTripPreservesBinaryDataAndUnicodeLabels() {
        val card = CardRecord(id = "test", label = "我的房卡", scannedAt = "2026-10-13T00:00:00Z",
            uidHex = byteArrayOf(0, 0x80.toByte(), 0xff.toByte()).hex(),
            technologies = listOf("NfcA", "Ndef"), details = mapOf("ATQA" to "0400"), ndefStatus = "已读取",
            records = listOf(PublicNdefRecord(1, "54", "", "02656E6869", "hi"), PublicNdefRecord(2, "00", "", "FF", null)))
        assertEquals("0080FF", card.uidHex)
        assertEquals(card, CardRecord.parse(card.json().toString()))
        assertTrue(card.json().getString("notice").contains("不能用于"))
    }

    @Test(expected = IllegalArgumentException::class) fun unknownSchemaIsRejected() {
        CardRecord.parse("{\"schemaVersion\":2}")
    }
}
