package io.github.kubrainy.sysmondax_widgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import es.antonborri.home_widget.HomeWidgetLaunchIntent

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

        // `home_widget`'ın `initiallyLaunchedFromHomeWidget()` / `widgetClicked` akışı,
        // gelen Intent'in action'ının TAM OLARAK bu sabite eşit olmasını bekliyor
        // (bkz. HomeWidgetPlugin.kt) — Intent.ACTION_VIEW kullanılırsa Dart tarafı
        // widget'tan açıldığını hiç fark etmez, tıklama sessizce yok sayılır.
        launchIntent.action = HomeWidgetLaunchIntent.HOME_WIDGET_LAUNCH_ACTION
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
