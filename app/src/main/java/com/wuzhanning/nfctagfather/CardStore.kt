package com.wuzhanning.nfctagfather

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class CardStore(context: Context) : SQLiteOpenHelper(context, "cards.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE cards (id TEXT PRIMARY KEY, label TEXT NOT NULL, scanned_at TEXT NOT NULL, record TEXT NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    fun save(record: CardRecord) {
        val values = ContentValues().apply {
            put("id", record.id); put("label", record.label); put("scanned_at", record.scannedAt)
            put("record", record.json().toString())
        }
        check(writableDatabase.insertWithOnConflict("cards", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
    }
    fun all(): List<CardRecord> = readableDatabase.rawQuery("SELECT record FROM cards ORDER BY scanned_at DESC", null).use { c ->
        buildList { while (c.moveToNext()) add(CardRecord.parse(c.getString(0))) }
    }
}
