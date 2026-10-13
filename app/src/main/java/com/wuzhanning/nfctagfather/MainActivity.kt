package com.wuzhanning.nfctagfather

import android.app.Activity
import android.nfc.NfcManager
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusView = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 20f
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(
                    padding + insets.systemWindowInsetLeft,
                    padding + insets.systemWindowInsetTop,
                    padding + insets.systemWindowInsetRight,
                    padding + insets.systemWindowInsetBottom,
                )
                insets
            }
        }
        setContentView(statusView)
    }

    override fun onResume() {
        super.onResume()
        val adapter = getSystemService(NfcManager::class.java)?.defaultAdapter
        val message = when {
            adapter == null -> R.string.nfc_unavailable
            adapter.isEnabled -> R.string.nfc_available
            else -> R.string.nfc_disabled
        }
        statusView.text = getString(R.string.status_message, getString(R.string.welcome), getString(message))
    }
}
