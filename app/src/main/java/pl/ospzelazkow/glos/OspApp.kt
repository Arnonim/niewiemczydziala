package pl.ospzelazkow.glos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import pl.ospzelazkow.glos.data.OspDatabase
import pl.ospzelazkow.glos.data.OspRepository

class OspApp : Application() {
    val repo: OspRepository by lazy { OspRepository(OspDatabase.get(this).dao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_RUN, "GĹ‚osowanie", NotificationManager.IMPORTANCE_LOW))
            nm.createNotificationChannel(NotificationChannel(CH_ALERT, "Alerty", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }

    companion object {
        const val CH_RUN = "run"
        const val CH_ALERT = "alert"
        lateinit var instance: OspApp
            private set
    }
}