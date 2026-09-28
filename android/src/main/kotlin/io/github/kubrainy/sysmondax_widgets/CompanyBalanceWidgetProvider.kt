package io.github.kubrainy.sysmondax_widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
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

class CompanyBalanceWidgetProvider : HomeWidgetProvider() {

    companion object {
        private const val REFRESH_WORK_NAME = "company_balance_periodic_refresh"
        private val REFRESH_URI: Uri = Uri.parse("app://widget/companybalance")
        private const val ACTION_TOGGLE_VISIBILITY =
            "io.github.kubrainy.sysmondax_widgets.TOGGLE_BALANCE_VISIBILITY"
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        val request = PeriodicWorkRequestBuilder<CompanyBalanceRefreshWorker>(
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
        if (intent.action == ACTION_TOGGLE_VISIBILITY) {
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val widgetData = HomeWidgetPlugin.getData(context)
                val key = "balance_visible_$widgetId"
                val currentlyVisible = widgetData.getBoolean(key, false)
                widgetData.edit().putBoolean(key, !currentlyVisible).apply()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateWidget(context, appWidgetManager, widgetId, widgetData)
            }
            return
        }

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

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        widgetData: SharedPreferences,
    ) {
        val isVisible = widgetData.getBoolean("balance_visible_$widgetId", false)

        val views = RemoteViews(context.packageName, R.layout.company_balance_widget).apply {
            val companyName = widgetData.getString("balance_company_name", null)
            val debit = widgetData.getString("balance_debit", null)
            val credit = widgetData.getString("balance_credit", null)
            val cash = widgetData.getString("balance_cash", null)
            val symbol = widgetData.getString("balance_currency_symbol", "") ?: ""

            setTextViewText(R.id.balance_company_name, companyName ?: "")

            if (debit == null) {
                setTextViewText(R.id.balance_footer, "Veri yok, uygulamayı aç")
                setViewVisibility(R.id.balance_footer, android.view.View.VISIBLE)
            } else {
                if (isVisible) {
                    setTextViewText(R.id.balance_debit, "$debit $symbol")
                    setTextViewText(R.id.balance_credit, "$credit $symbol")
                    setTextViewText(R.id.balance_cash, "$cash $symbol")
                } else {
                    setTextViewText(R.id.balance_debit, "******")
                    setTextViewText(R.id.balance_credit, "******")
                    setTextViewText(R.id.balance_cash, "******")
                }
                setViewVisibility(R.id.balance_footer, android.view.View.GONE)
            }

            setImageViewResource(
                R.id.balance_visibility_toggle,
                if (isVisible) R.drawable.ic_eye else R.drawable.ic_eye_off,
            )

            val openAppIntent = SysmondaxWidgetsLaunch.pendingIntent(context)
            setOnClickPendingIntent(R.id.balance_widget_container, openAppIntent)

            val toggleIntent = Intent(context, CompanyBalanceWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_VISIBILITY
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse("widget://balance/toggle/$widgetId")
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            setOnClickPendingIntent(R.id.balance_visibility_toggle_hitarea, togglePendingIntent)
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
