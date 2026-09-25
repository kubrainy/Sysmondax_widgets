package io.github.kubrainy.sysmondax_widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup

/**
 * `NewDispatchWidgetProvider`'ın 1x4 kademesindeki sağ alan (bkz.
 * `new_dispatch_split_right`) için "Gelen Faturaları Gör" / "Giden
 * Faturaları Gör" seçimini yapan ayar ekranı — [NewInvoiceWidgetConfigActivity]
 * ile birebir aynı mantık, sadece [NewDispatchWidgetProvider] üzerinde
 * çalışıyor (bu widget'ın tercihi Yeni Fatura Oluştur widget'ından bağımsız).
 */
class NewDispatchWidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.widget_split_config)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val group = findViewById<RadioGroup>(R.id.config_direction_group)
        val currentDirection = NewDispatchWidgetProvider.getSplitDirection(this, appWidgetId)
        group.check(
            if (currentDirection == NewDispatchWidgetProvider.DIRECTION_OUTGOING) {
                R.id.config_option_outgoing
            } else {
                R.id.config_option_incoming
            },
        )

        findViewById<Button>(R.id.config_save_button).setOnClickListener {
            val direction = if (group.checkedRadioButtonId == R.id.config_option_outgoing) {
                NewDispatchWidgetProvider.DIRECTION_OUTGOING
            } else {
                NewDispatchWidgetProvider.DIRECTION_INCOMING
            }
            NewDispatchWidgetProvider.setSplitDirection(this, appWidgetId, direction)

            val appWidgetManager = AppWidgetManager.getInstance(this)
            NewDispatchWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
