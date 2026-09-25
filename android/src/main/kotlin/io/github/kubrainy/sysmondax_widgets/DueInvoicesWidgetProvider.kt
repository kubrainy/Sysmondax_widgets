package io.github.kubrainy.sysmondax_widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import es.antonborri.home_widget.HomeWidgetBackgroundIntent
import es.antonborri.home_widget.HomeWidgetPlugin
import es.antonborri.home_widget.HomeWidgetProvider
import java.util.concurrent.TimeUnit

class DueInvoicesWidgetProvider : HomeWidgetProvider() {

    companion object {
        private const val REFRESH_WORK_NAME = "due_invoices_periodic_refresh"
        private val REFRESH_URI: Uri = Uri.parse("app://widget/dueinvoices")

        // due_invoice_widget.xml'deki due_item1_row..due_item6_row ile eşleşir;
        // Dart tarafındaki updater fonksiyonu ve ilgili sabitlerle aynı kalmalı.
        private const val MAX_ROWS = 6
        private const val DEFAULT_VISIBLE_ROWS = 2

        // Widget'ın sabit "chrome"u (başlık + şirket adı + divider + footer + padding'ler)
        // yaklaşık dp yüksekliği; kalan alan satır sayısını belirlemek için kullanılıyor.
        private const val CHROME_HEIGHT_DP = 95
        private const val ROW_HEIGHT_DP = 32

        private val ROW_IDS = intArrayOf(
            R.id.due_item1_row, R.id.due_item2_row, R.id.due_item3_row,
            R.id.due_item4_row, R.id.due_item5_row, R.id.due_item6_row,
        )
        private val NAME_IDS = intArrayOf(
            R.id.due_item1_name, R.id.due_item2_name, R.id.due_item3_name,
            R.id.due_item4_name, R.id.due_item5_name, R.id.due_item6_name,
        )
        private val AMOUNT_IDS = intArrayOf(
            R.id.due_item1_amount, R.id.due_item2_amount, R.id.due_item3_amount,
            R.id.due_item4_amount, R.id.due_item5_amount, R.id.due_item6_amount,
        )
        private val DATE_IDS = intArrayOf(
            R.id.due_item1_date, R.id.due_item2_date, R.id.due_item3_date,
            R.id.due_item4_date, R.id.due_item5_date, R.id.due_item6_date,
        )
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val request = PeriodicWorkRequestBuilder<DueInvoicesRefreshWorker>(
            15, TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            REFRESH_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(REFRESH_WORK_NAME)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isSystemTriggered = intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE &&
            !intent.getBooleanExtra(HomeWidgetPlugin.TRIGGERED_FROM_HOME_WIDGET, false)

        if (isSystemTriggered) {
            try {
                HomeWidgetBackgroundIntent.getBroadcast(context, REFRESH_URI).send()
            } catch (e: PendingIntent.CanceledException) {
            }
        }

        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        widgetData: SharedPreferences,
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId, widgetData)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        // Kullanıcı widget'ı sürükleyip büyüttüğünde/küçülttüğünde tetiklenir;
        // aynı verilerle görünür satır sayısını yeni boyuta göre yeniden hesaplar.
        updateWidget(context, appWidgetManager, appWidgetId, HomeWidgetPlugin.getData(context))
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        widgetData: SharedPreferences,
    ) {
        val visibleRows = visibleRowCount(appWidgetManager, widgetId)

        val views = RemoteViews(context.packageName, R.layout.due_invoice_widget).apply {
            val companyName = widgetData.getString("due_company_name", null)

            if (companyName == null) {
                setTextViewText(R.id.due_company_name, "Veri yok, uygulamayı aç")
                ROW_IDS.forEach { setViewVisibility(it, android.view.View.GONE) }
                setViewVisibility(R.id.due_empty_message, android.view.View.GONE)
            } else {
                setTextViewText(R.id.due_company_name, companyName)

                var hasAnyItem = false
                for (i in 0 until MAX_ROWS) {
                    val name = widgetData.getString("due_item${i + 1}_name", null)
                    val show = i < visibleRows && !name.isNullOrEmpty()
                    if (show) hasAnyItem = true
                    bindItemRow(
                        this, ROW_IDS[i], NAME_IDS[i], AMOUNT_IDS[i], DATE_IDS[i],
                        if (show) name else null,
                        widgetData.getString("due_item${i + 1}_amount", null),
                        widgetData.getString("due_item${i + 1}_date", null),
                    )
                }

                setViewVisibility(
                    R.id.due_empty_message,
                    if (hasAnyItem) android.view.View.GONE else android.view.View.VISIBLE,
                )
            }

            val pendingIntent = SysmondaxWidgetsLaunch.pendingIntent(context)
            setOnClickPendingIntent(R.id.due_widget_container, pendingIntent)
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    /**
     * Widget'ın o anki yerleşim yüksekliğine (dp) göre kaç satır faturanın
     * sığdığını hesaplar. Boyut bilgisi okunamazsa eski sabit davranışa
     * (2 satır) düşer.
     */
    private fun visibleRowCount(appWidgetManager: AppWidgetManager, widgetId: Int): Int {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1)
        if (heightDp <= 0) return DEFAULT_VISIBLE_ROWS

        val fit = (heightDp - CHROME_HEIGHT_DP) / ROW_HEIGHT_DP
        return fit.coerceIn(1, MAX_ROWS)
    }

    private fun bindItemRow(
        views: RemoteViews,
        rowId: Int,
        nameId: Int,
        amountId: Int,
        dateId: Int,
        name: String?,
        amount: String?,
        date: String?,
    ) {
        if (name.isNullOrEmpty()) {
            views.setViewVisibility(rowId, android.view.View.GONE)
            return
        }
        views.setViewVisibility(rowId, android.view.View.VISIBLE)
        views.setTextViewText(nameId, name)
        views.setTextViewText(amountId, amount ?: "")
        views.setTextViewText(dateId, if (date.isNullOrEmpty()) "" else "Vade: $date")
    }
}
