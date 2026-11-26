package pl.ospzelazkow.glos.svc

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import kotlinx.coroutines.delay
import pl.ospzelazkow.glos.a11y.RunState
import pl.ospzelazkow.glos.web.VoteActivity

object IpRotator {

    suspend fun rotate(ctx: Context): Boolean {
        if (root("cmd connectivity airplane-mode enable")) {
            delay(5000)
            root("cmd connectivity airplane-mode disable")
            return waitNet(ctx, 12)
        }
        if (!RunState.a11yConnected) {
            ctx.startActivity(
                Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }

        RunState.rotateRequest++
        val target = RunState.rotateRequest
        ctx.startActivity(
            Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        repeat(45) {
            delay(1000)
            if (RunState.rotateDone >= target) {
                ctx.startActivity(
                    Intent(ctx, VoteActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                )
                return waitNet(ctx, 10)
            }
        }
        return false
    }

    private fun root(cmd: String): Boolean = runCatching {
        ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start().waitFor() == 0
    }.getOrDefault(false)

    private suspend fun waitNet(ctx: Context, sec: Int): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return false
        repeat(sec) {
            delay(1000)
            val c = cm.getNetworkCapabilities(cm.activeNetwork)
            if (c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) return true
        }
        return false
    }
}
