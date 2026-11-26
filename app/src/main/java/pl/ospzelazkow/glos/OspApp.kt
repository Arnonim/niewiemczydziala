package pl.ospzelazkow.glos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import pl.ospzelazkow.glos.util.FileLog

class OspApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        FileLog.init(this)
        FileLog.i("APP", "start v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) sdk=${Build.VERSION.SDK_INT}")
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            val ch = NotificationChannel(CHANNEL, "Głosowanie OSP", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            nm.createNotificationChannel(ch)
        }
    }

    companion object {
        const val CHANNEL = "osp_vote"

        @Volatile
        lateinit var instance: OspApp
            private set
    }
}
