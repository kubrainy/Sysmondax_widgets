package io.github.kubrainy.sysmondax_widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import es.antonborri.home_widget.HomeWidgetBackgroundIntent
import es.antonborri.home_widget.HomeWidgetPlugin
import es.antonborri.home_widget.HomeWidgetProvider
import java.util.concurrent.TimeUnit

class RecentTransactionsWidgetProvider : HomeWidgetProvider() {

    companion object {
        private const val REFRESH_WORK_NAME = "recent_transactions_periodic_refresh"
        private val REFRESH_URI: Uri = Uri.parse("app://widget/recenttransactions")

        // son_islemler_widget.xml'deki son_item1_row..son_item6_row ile eşleşir;
        // Dart tarafındaki updater fonksiyonuyla aynı kalmalı.
        private const val MAX_ROWS = 6
        private const val DEFAULT_VISIBLE_ROWS = 2

        // due_invoice ile aynı mantık (bkz. DueInvoicesWidgetProvider): widget'ın
        // sabit "chrome"u (başlık + şirket adı + divider + padding'ler) yaklaşık
        // dp yüksekliği; kalan alan satır sayısını belirlemek için kullanılıyor.
        // Her satır iki satırlı (isim + durum rozeti/tarih) olduğundan due_invoice
        // widget'ından biraz daha yüksek.
        private const val CHROME_HEIGHT_DP = 95
        private const val ROW_HEIGHT_DP = 34

        private val ROW_IDS = intArrayOf(
            R.id.son_item1_row, R.id.son_item2_row, R.id.son_item3_row,
            R.id.son_item4_row, R.id.son_item5_row, R.id.son_item6_row,
        )
        private val NAME_IDS = intArrayOf(
            R.id.son_item1_name, R.id.son_item2_name, R.id.son_item3_name,
            R.id.son_item4_name, R.id.son_item5_name, R.id.son_item6_name,
        )
        private val AMOUNT_IDS = intArrayOf(
            R.id.son_item1_amount, R.id.son_item2_amount, R.id.son_item3_amount,
            R.id.son_item4_amount, R.id.son_item5_amount, R.id.son_item6_amount,
        )
        private val STATUS_PILL_IDS = intArrayOf(
            R.id.son_item1_status_pill, R.id.son_item2_status_pill, R.id.son_item3_status_pill,
            R.id.son_item4_status_pill, R.id.son_item5_status_pill, R.id.son_item6_status_pill,
        )
        private val DATE_IDS = intArrayOf(
            R.id.son_item1_date, R.id.son_item2_date, R.id.son_item3_date,
            R.id.son_item4_date, R.id.son_item5_date, R.id.son_item6_date,
        )
        private val ARROW_BADGE_IDS = intArrayOf(
            R.id.son_item1_arrow_badge, R.id.son_item2_arrow_badge, R.id.son_item3_arrow_badge,
            R.id.son_item4_arrow_badge, R.id.son_item5_arrow_badge, R.id.son_item6_arrow_badge,
        )
        private val ARROW_TEXT_IDS = intArrayOf(
            R.id.son_item1_arrow, R.id.son_item2_arrow, R.id.son_item3_arrow,
            R.id.son_item4_arrow, R.id.son_item5_arrow, R.id.son_item6_arrow,
        )
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val request = PeriodicWorkRequestBuilder<RecentTransactionsRefreshWorker>(
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

        val views = RemoteViews(context.packageName, R.layout.son_islemler_widget).apply {
            val companyName = widgetData.getString("son_company_name", null)

            if (companyName == null) {
                setTextViewText(R.id.son_company_name, "Veri yok, uygulamayı aç")
                ROW_IDS.forEach { setViewVisibility(it, android.view.View.GONE) }
                setViewVisibility(R.id.son_empty_message, android.view.View.GONE)
            } else {
                setTextViewText(R.id.son_company_name, companyName)

                var hasAnyItem = false
                for (i in 0 until MAX_ROWS) {
                    val name = widgetData.getString("son_item${i + 1}_name", null)
                    val show = i < visibleRows && !name.isNullOrEmpty()
                    if (show) hasAnyItem = true
                    bindItemRow(
                        context,
                        this,
                        i,
                        if (show) name else null,
                        widgetData.getString("son_item${i + 1}_amount", null),
                        widgetData.getString("son_item${i + 1}_date", null),
                        widgetData.getString("son_item${i + 1}_status", null),
                        widgetData.getString("son_item${i + 1}_direction", null),
                    )
                }

                setViewVisibility(
                    R.id.son_empty_message,
                    if (hasAnyItem) android.view.View.GONE else android.view.View.VISIBLE,
                )
            }

            val pendingIntent = SysmondaxWidgetsLaunch.pendingIntent(context)
            setOnClickPendingIntent(R.id.son_widget_container, pendingIntent)
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    /**
     * Widget'ın o anki yerleşim yüksekliğine (dp) göre kaç işlem satırının
     * sığdığını hesaplar. Boyut bilgisi okunamazsa eski sabit davranışa
     * (2 satır) düşer. Widget artık `minResizeHeight` ile 4x2'nin altına
     * hiç küçültülemediği için (bkz. son_islemler_widget_info.xml), alt sınır
     * hep DEFAULT_VISIBLE_ROWS (2) — CHROME/ROW dp tahminleri cihaza göre
     * biraz sapsa bile en küçük boyutta asla 2'den az satır görünmeyecek.
     */
    private fun visibleRowCount(appWidgetManager: AppWidgetManager, widgetId: Int): Int {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1)
        if (heightDp <= 0) return DEFAULT_VISIBLE_ROWS

        val fit = (heightDp - CHROME_HEIGHT_DP) / ROW_HEIGHT_DP
        return fit.coerceIn(DEFAULT_VISIBLE_ROWS, MAX_ROWS)
    }

    private fun bindItemRow(
        context: Context,
        views: RemoteViews,
        index: Int,
        name: String?,
        amount: String?,
        date: String?,
        status: String?,
        direction: String?,
    ) {
        val rowId = ROW_IDS[index]
        if (name.isNullOrEmpty()) {
            views.setViewVisibility(rowId, android.view.View.GONE)
            return
        }
        views.setViewVisibility(rowId, android.view.View.VISIBLE)
        views.setTextViewText(NAME_IDS[index], name)
        views.setTextViewText(AMOUNT_IDS[index], amount ?: "")
        views.setTextViewText(DATE_IDS[index], date ?: "")

        val statusPillId = STATUS_PILL_IDS[index]
        views.setTextViewText(statusPillId, status ?: "")
        val (pillBgRes, pillTextColorRes) = statusStyle(status)
        views.setInt(statusPillId, "setBackgroundResource", pillBgRes)
        views.setTextColor(statusPillId, ContextCompat.getColor(context, pillTextColorRes))

        val isOutgoing = direction == "20"
        val arrowBadgeId = ARROW_BADGE_IDS[index]
        val arrowTextId = ARROW_TEXT_IDS[index]
        views.setTextViewText(arrowTextId, if (isOutgoing) "↑" else "↓")
        views.setInt(
            arrowBadgeId,
            "setBackgroundResource",
            if (isOutgoing) R.drawable.widget_arrow_badge_out else R.drawable.widget_arrow_badge_in,
        )
        views.setTextColor(
            arrowTextId,
            ContextCompat.getColor(
                context,
                if (isOutgoing) R.color.widget_positive else R.color.widget_negative,
            ),
        )
    }

    /**
     * Durum adına göre rozetin arka planı + yazı rengi. Sysmondax'ın gerçek
     * durum renkleriyle aynı aile: taslak/içe aktarma bekleniyor = mor
     * (widget_accent), gönderildi = yeşil (widget_positive), reddedildi =
     * turuncu (widget_warning). Bilinmeyen bir durum gelirse nötr
     * (widget_muted) renkte gösterilir.
     */
    private fun statusStyle(status: String?): Pair<Int, Int> {
        return when (status) {
            "Taslak", "İçe Aktarma Bekleniyor" ->
                Pair(R.drawable.widget_status_pill_purple, R.color.widget_accent)
            "Gönderildi" ->
                Pair(R.drawable.widget_status_pill_green, R.color.widget_positive)
            "Reddedildi" ->
                Pair(R.drawable.widget_status_pill_orange, R.color.widget_warning)
            else ->
                Pair(R.drawable.widget_status_pill_purple, R.color.widget_muted)
        }
    }
}
