package io.github.kubrainy.sysmondax_widgets

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import es.antonborri.home_widget.HomeWidgetBackgroundIntent

class CompanyBalanceRefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            HomeWidgetBackgroundIntent.getBroadcast(
                applicationContext,
                Uri.parse("app://widget/companybalance"),
            ).send()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
