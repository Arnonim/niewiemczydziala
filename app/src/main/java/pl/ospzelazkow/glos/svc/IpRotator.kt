package pl.ospzelazkow.glos.svc

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import kotlinx.coroutines.delay

object IpRotator {

    suspend fun rotate(ctx: Context): Boolean {
        if (root("cmd connectivity airplane-mode enable")) {
            delay(4000)
            root("cmd connectivity airplane-mode disable")
            return waitNet(ctx)
        }
        ctx.startActivity(
            Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return false
    }

    private fun root(cmd: String): Boolean = runCatching {
        ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start().waitFor() == 0
    }.getOrDefault(false)

    private suspend fun waitNet(ctx: Context): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return false
        repeat(15) {
            delay(1000)
            val c = cm.getNetworkCapabilities(cm.activeNetwork)
            if (c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) return true
        }
        return false
    }
}
