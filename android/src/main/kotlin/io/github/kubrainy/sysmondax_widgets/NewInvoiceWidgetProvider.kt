package io.github.kubrainy.sysmondax_widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetProvider

/**
 * Sabit içerikli kısayol widget'ı: veri çekmez, periyodik yenileme yapmaz.
 * Tıklanınca ev sahibi uygulamayı `app://widget/outgoing-invoice-form` URI'siyle açar;
 * ev sahibi uygulamanın kendi Flutter tarafı bu URI'yi görünce fatura
 * oluşturma ekranını açacak şekilde kablolanmalı (bkz. plugin README'si).
 *
 * Tek layout (new_invoice_widget.xml) içinde 4 alt görünüm (tier_small,
 * tier_compact, tier_wide, tier_split) var; genişliğe göre sadece biri
 * VISIBLE yapılır. tier_split (1x4) sol tarafı tier_wide ile birebir aynı
 * ("Yeni Fatura Oluştur"), sağdaki küçük yazı alanı ise kullanıcının
 * [NewInvoiceWidgetConfigActivity] üzerinden seçtiği yöne göre "Gelen
 * Faturaları Gör" (Direction=10) veya "Giden Faturaları Gör" (Direction=20)
 * gösterir — widget'a uzun basıp Düzenle'ye tıklayınca bu ayar ekranı
 * tekrar açılır. Seçim [PREFS_NAME] içinde appWidgetId'ye özel saklanır.
 */
class NewInvoiceWidgetProvider : HomeWidgetProvider() {

    companion object {
        private val LAUNCH_URI: Uri = Uri.parse("app://widget/outgoing-invoice-form")
        private val INCOMING_INVOICES_URI: Uri = Uri.parse("app://widget/incoming-invoice-list")
        private val OUTGOING_INVOICES_URI: Uri = Uri.parse("app://widget/outgoing-invoice-list")
        private const val SMALL_WIDTH_THRESHOLD_DP = 100
        private const val COMPACT_WIDTH_THRESHOLD_DP = 160
        // Cihazda ölçüldü (logcat): 1x3 ≈ 242–258dp, 1x4 ≈ 335dp.
        private const val SPLIT_WIDTH_THRESHOLD_DP = 300

        const val PREFS_NAME = "new_invoice_widget_prefs"
        private const val DIRECTION_KEY_PREFIX = "split_direction_"
        const val DIRECTION_INCOMING = 10
        const val DIRECTION_OUTGOING = 20

        fun getSplitDirection(context: Context, widgetId: Int): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(DIRECTION_KEY_PREFIX + widgetId, DIRECTION_INCOMING)
        }

        fun setSplitDirection(context: Context, widgetId: Int, direction: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(DIRECTION_KEY_PREFIX + widgetId, direction)
                .apply()
        }

        /** Config activity, kaydettikten sonra widget'ı hemen yenilemek için çağırır. */
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -1)

            val showSmall = widthDp in 1 until SMALL_WIDTH_THRESHOLD_DP
            val showCompact = widthDp in SMALL_WIDTH_THRESHOLD_DP until COMPACT_WIDTH_THRESHOLD_DP
            val showSplit = widthDp >= SPLIT_WIDTH_THRESHOLD_DP
            val showWide = !showSmall && !showCompact && !showSplit

            val direction = getSplitDirection(context, widgetId)

            val views = RemoteViews(context.packageName, R.layout.new_invoice_widget).apply {
                setViewVisibility(R.id.new_invoice_tier_small, if (showSmall) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.new_invoice_tier_compact, if (showCompact) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.new_invoice_tier_wide, if (showWide) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.new_invoice_tier_split, if (showSplit) View.VISIBLE else View.GONE)

                if (direction == DIRECTION_OUTGOING) {
                    setTextViewText(R.id.new_invoice_split_right_text, "Giden\nFaturaları\nGör")
                } else {
                    setTextViewText(R.id.new_invoice_split_right_text, "Gelen\nFaturaları\nGör")
                }

                val newInvoiceIntent = SysmondaxWidgetsLaunch.pendingIntent(context, LAUNCH_URI)
                val directionUri = if (direction == DIRECTION_OUTGOING) OUTGOING_INVOICES_URI else INCOMING_INVOICES_URI
                val directionIntent = SysmondaxWidgetsLaunch.pendingIntent(context, directionUri)

                // Konteynerin tamamı için varsayılan hedef: "yeni fatura" (small/
                // compact/wide tier'ları kapsar). tier_split görünürken, kendi
                // içindeki iki alt view'in (split_left/split_right) ayrı
                // PendingIntent'leri konteynerinkini geçersiz kılar.
                setOnClickPendingIntent(R.id.new_invoice_widget_container, newInvoiceIntent)
                setOnClickPendingIntent(R.id.new_invoice_split_left, newInvoiceIntent)
                setOnClickPendingIntent(R.id.new_invoice_split_right, directionIntent)
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        widgetData: SharedPreferences,
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        appWidgetIds.forEach { widgetId -> editor.remove(DIRECTION_KEY_PREFIX + widgetId) }
        editor.apply()
    }
}
