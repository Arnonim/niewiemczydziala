package pl.ospzelazkow.glos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import pl.ospzelazkow.glos.data.OspDatabase
import pl.ospzelazkow.glos.data.OspRepository
import pl.ospzelazkow.glos.util.FileLog

class OspApp : Application() {

    val repo: OspRepository by lazy { OspRepository(OspDatabase.get(this).dao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FileLog.init(this)
        FileLog.w("APP start v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) sdk=${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_RUN, "Głosowanie", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel(CH_ALERT, "Alerty", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    companion object {
        const val CH_RUN = "run"
        const val CH_ALERT = "alert"

        @Volatile
        lateinit var instance: OspApp
            private set
    }
}
