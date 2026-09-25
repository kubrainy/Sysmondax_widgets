package io.github.kubrainy.sysmondax_widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup

/**
 * `NewInvoiceWidgetProvider`'ın 1x4 kademesindeki sağ alan (bkz.
 * `new_invoice_split_right`) için "Gelen Faturaları Gör" / "Giden
 * Faturaları Gör" seçimini yapan ayar ekranı.
 *
 * Widget ilk eklendiğinde otomatik açılır (widget_info.xml'deki
 * `android:configure`), ayrıca çoğu launcher'da widget'a uzun basıp
 * "Düzenle"ye tıklandığında da tekrar açılır — bu ikinci kullanım,
 * kullanıcının kaydettiği seçimi normal RESULT_OK akışıyla günceller.
 */
class NewInvoiceWidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Kullanıcı geri tuşuna basıp iptal ederse widget eklenmesin diye
        // varsayılan sonuç CANCELED; kaydedince RESULT_OK'a çeviriyoruz.
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
        val currentDirection = NewInvoiceWidgetProvider.getSplitDirection(this, appWidgetId)
        group.check(
            if (currentDirection == NewInvoiceWidgetProvider.DIRECTION_OUTGOING) {
                R.id.config_option_outgoing
            } else {
                R.id.config_option_incoming
            },
        )

        findViewById<Button>(R.id.config_save_button).setOnClickListener {
            val direction = if (group.checkedRadioButtonId == R.id.config_option_outgoing) {
                NewInvoiceWidgetProvider.DIRECTION_OUTGOING
            } else {
                NewInvoiceWidgetProvider.DIRECTION_INCOMING
            }
            NewInvoiceWidgetProvider.setSplitDirection(this, appWidgetId, direction)

            val appWidgetManager = AppWidgetManager.getInstance(this)
            NewInvoiceWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
