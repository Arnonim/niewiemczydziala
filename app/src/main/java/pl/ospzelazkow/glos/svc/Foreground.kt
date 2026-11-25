package pl.ospzelazkow.glos.svc

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import pl.ospzelazkow.glos.OspApp
import pl.ospzelazkow.glos.R
import pl.ospzelazkow.glos.web.VoteActivity

object Foreground {
    private var seq = 1000

    fun build(ctx: Context, text: String, sub: String = ""): Notification {
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, VoteActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(ctx, OspApp.CH_RUN)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(ctx.getString(R.string.svc_title))
            .setContentText(text)
            .setSubText(sub.ifEmpty { null })
            .setOngoing(true).setOnlyAlertOnce(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun nextId() = seq++
}