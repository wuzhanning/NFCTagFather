package com.wuzhanning.nfctagfather

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var result: TextView
    private lateinit var scanButton: Button
    private lateinit var saveButton: Button
    private lateinit var exportButton: Button
    private val worker = Executors.newSingleThreadExecutor()
    private val reading = AtomicBoolean(false)
    private lateinit var store: CardStore
    private var current: CardRecord? = null
    private var pendingExport: String? = null
    private var scanning = false
    private var scanGeneration = 0
    private val adapter get() = getSystemService(NfcManager::class.java)?.defaultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = CardStore(applicationContext)
        current = savedInstanceState?.getString("current")?.let { runCatching { CardRecord.parse(it) }.getOrNull() }
        pendingExport = savedInstanceState?.getString("pendingExport")
        val padding = (20 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            @Suppress("DEPRECATION")
            setOnApplyWindowInsetsListener { v, insets ->
                v.setPadding(padding + insets.systemWindowInsetLeft, padding + insets.systemWindowInsetTop,
                    padding + insets.systemWindowInsetRight, padding + insets.systemWindowInsetBottom)
                insets
            }
        }
        fun text(value: String, size: Float = 16f) = TextView(this).apply {
            text = value; textSize = size; setPadding(0, padding / 2, 0, padding / 2)
            layout.addView(this)
        }
        text("NFCTagFather", 28f)
        text("识别 · 保存 · 导出", 18f)
        text("只读取可访问的信息，不写入卡片。导出的文件不能用于开门或添加到 Apple 钱包。", 14f)
        status = text("")
        fun button(label: String, click: () -> Unit) = Button(this).apply {
            this.text = label; layout.addView(this); setOnClickListener { click() }
        }
        scanButton = button("开始扫描") {
            if (scanning) stopScanning() else {
                val a = adapter
                if (a == null) showMessage("这台手机不支持 NFC。")
                else if (!a.isEnabled) startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                else { scanning = true; scanGeneration++; enableReader(); refresh() }
            }
        }
        button("已保存记录") { showSaved() }
        saveButton = button("保存当前记录") { saveCurrent() }
        exportButton = button("导出当前记录为 JSON") { exportCurrent() }
        result = text("点击“开始扫描”，将卡片贴近手机 NFC 感应区域并保持不动。", 15f)
        result.setTextIsSelectable(true)
        result.setTextColor(Color.DKGRAY)
        setContentView(ScrollView(this).apply { addView(layout) })
        refresh()
    }

    override fun onResume() { super.onResume(); if (scanning) enableReader(); refresh() }
    override fun onPause() { stopScanning(); super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("current", current?.json()?.toString())
        outState.putString("pendingExport", pendingExport)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        worker.execute { store.close() }
        worker.shutdown()
        super.onDestroy()
    }

    private fun enableReader() {
        val generation = scanGeneration
        adapter?.enableReaderMode(this, { tag ->
            if (!reading.compareAndSet(false, true)) return@enableReaderMode
            try {
                val value = CardScanner.read(tag)
                runOnUiThread {
                    if (!isDestroyed && scanning && generation == scanGeneration) {
                        current = value; stopScanning(); refresh()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread { if (!isDestroyed && scanning && generation == scanGeneration) showMessage("扫描失败，请重新贴卡重试。") }
            } finally { reading.set(false) }
        }, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NFC_BARCODE, null)
    }

    private fun stopScanning() {
        scanning = false; scanGeneration++
        adapter?.disableReaderMode(this)
        if (::status.isInitialized) refresh()
    }

    private fun refresh() {
        val a = adapter
        status.text = when { a == null -> "设备不支持 NFC"; !a.isEnabled -> "NFC 已关闭，请启用后扫描"
            scanning -> "正在扫描：请保持卡片贴合手机…"; else -> "等待扫描" }
        scanButton.text = if (scanning) "停止扫描" else "开始扫描"
        scanButton.isEnabled = a != null
        saveButton.isEnabled = current != null && !scanning
        exportButton.isEnabled = current != null && !scanning && pendingExport == null
        current?.let { card ->
            result.text = buildString {
                appendLine(card.label); appendLine("扫描时间（UTC）：${card.scannedAt}")
                appendLine("UID：${card.uidHex.ifEmpty { "设备未提供" }}")
                appendLine("技术类型：${card.technologies.joinToString(", ")}")
                card.details.forEach { (k, v) -> appendLine("$k：$v") }
                appendLine(); appendLine(card.ndefStatus)
                card.records.forEachIndexed { index, r ->
                    appendLine(); appendLine("NDEF ${index + 1}（TNF ${r.tnf}）")
                    r.text?.let { appendLine("内容：$it") }
                    appendLine("Type：${r.typeHex}"); appendLine("ID：${r.idHex}")
                    appendLine("Payload：${r.payloadHex}")
                }
                appendLine(); append("UID 和技术类型不等于门禁凭据，也不能确定所属酒店或门锁系统。")
            }
        }
    }

    private fun saveCurrent() {
        val card = current ?: return
        val input = EditText(this).apply { hint = "例如：Pullman 我的房卡"; setText(card.label); selectAll(); isSingleLine = true }
        AlertDialog.Builder(this).setTitle("保存卡片信息").setView(input).setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val saved = card.copy(label = input.text.toString().trim().take(80).ifEmpty { "未命名卡片" })
                worker.execute {
                    val success = runCatching { store.save(saved) }.isSuccess
                    runOnUiThread { if (!isDestroyed) {
                        if (success) { current = saved; refresh() }
                        showMessage(if (success) "已保存到本机" else "保存失败，请检查存储空间。")
                    } }
                }
            }.show()
    }

    private fun showSaved() {
        stopScanning()
        worker.execute {
            val cards = runCatching { store.all() }
            runOnUiThread { if (!isDestroyed) {
                cards.onSuccess { list ->
                    if (list.isEmpty()) showMessage("还没有保存的记录。") else
                        AlertDialog.Builder(this).setTitle("已保存记录")
                            .setItems(list.map { "${it.label}\n${it.uidHex} · ${it.scannedAt}" }.toTypedArray()) { _, index ->
                                current = list[index]; refresh()
                            }.setNegativeButton("关闭", null).show()
                }.onFailure { showMessage("读取保存记录失败。") }
            } }
        }
    }

    @Suppress("DEPRECATION")
    private fun exportCurrent() {
        val card = current ?: return
        pendingExport = card.json().toString(2)
        try {
            startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "NFCTagFather-${card.id}.json")
            }, 100)
        } catch (_: Exception) { pendingExport = null; showMessage("此设备没有可用的文件保存程序。"); refresh() }
    }

    @Deprecated("Uses platform Activity document result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 100) return
        val value = pendingExport
        pendingExport = null
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null || value == null) { refresh(); return }
        worker.execute {
            val success = runCatching {
                checkNotNull(contentResolver.openOutputStream(uri, "wt")).use { it.write(value.toByteArray(Charsets.UTF_8)) }
            }.isSuccess
            runOnUiThread { if (!isDestroyed) {
                showMessage(if (success) "JSON 已导出，可发送到 iPhone 查看（不能用于开门）。" else "导出失败，请更换保存位置或检查存储空间。")
                refresh()
            } }
        }
    }

    private fun showMessage(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
