package io.github.kubrainy.sysmondax_widgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Bu plugin, kendisini kullanan uygulamanın launcher (ana) Activity'sinin
 * sınıfını derleme zamanında bilemez (o, ev sahibi uygulamaya ait). Bu yüzden
 * `es.antonborri.home_widget`'ın `HomeWidgetLaunchIntent.getActivity(context,
 * SomeActivity::class.java, uri)` çağrısı yerine, ev sahibi uygulamanın
 * launcher Activity'sini sistemden çalışma zamanında bulup ona `uri`'yi
 * taşıyan bir Intent gönderiyoruz. Böylece widget tıklamaları hangi
 * uygulamaya entegre edilirse edilsin, o uygulamanın kendi ana ekranını açar.
 */
internal object SysmondaxWidgetsLaunch {
    fun pendingIntent(context: Context, uri: Uri? = null): PendingIntent {
        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()

        launchIntent.action = Intent.ACTION_VIEW
        launchIntent.data = uri
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        return PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
