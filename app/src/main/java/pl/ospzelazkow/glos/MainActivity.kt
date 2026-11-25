package pl.ospzelazkow.glos

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as ASettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.ospzelazkow.glos.a11y.RunState
import pl.ospzelazkow.glos.data.*
import pl.ospzelazkow.glos.svc.MailChecker
import pl.ospzelazkow.glos.svc.VoteService

class MainActivity : ComponentActivity() {

    private val repo get() = (application as OspApp).repo
    private var s by mutableStateOf(Settings())

    private val notif = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val open = registerForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        u ?: return@registerForActivityResult
        lifecycleScope.launch { toast("Import: ${AliasImporter.fromUri(this@MainActivity, repo, u)}") }
    }
    private val save = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { u: Uri? ->
        u ?: return@registerForActivityResult
        lifecycleScope.launch {
            val l = runCatching { repo.votedEmails.first() }.getOrDefault(emptyList())
            contentResolver.openOutputStream(u)?.bufferedWriter()?.use { w -> l.forEach { w.appendLine(it) } }
            toast("Zapisano ${l.size}")
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) notif.launch("android.permission.POST_NOTIFICATIONS")
        lifecycleScope.launch { s = Prefs.read(this@MainActivity) }
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface(Modifier.fillMaxSize()) { Panel() } } }
    }

    private fun toast(t: String) = android.widget.Toast.makeText(this, t, android.widget.Toast.LENGTH_SHORT).show()

    @Composable
    private fun Panel() {
        val total by repo.total.collectAsStateWithLifecycle(0)
        val conf by repo.voted.collectAsStateWithLifecycle(0)
        val left by repo.remaining.collectAsStateWithLifecycle(0)
        val rate by repo.rate.collectAsStateWithLifecycle(0)
        val exp by repo.expired.collectAsStateWithLifecycle(0)
        val logs by repo.logs.collectAsStateWithLifecycle(emptyList())
        var running by remember { mutableStateOf(RunState.running) }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("OSP Ĺ»elazkĂłw ${BuildConfig.VERSION_NAME}")
            Text("pula: $total", style = MaterialTheme.typography.bodySmall)

            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("Potw.", conf); Stat("ZostaĹ‚o", left); Stat("429", rate); Stat("WygasĹ‚e", exp)
            }

            Button(
                onClick = {
                    if (running) { VoteService.stop(this@MainActivity); running = false; RunState.running = false }
                    else { VoteService.start(this@MainActivity); running = true; RunState.running = true }
                }, modifier = Modifier.fillMaxWidth()
            ) { Text(if (running) "STOP" else "START (${s.batchLimit})") }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { startActivity(Intent(ASettings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("DostÄ™pnoĹ›Ä‡") }
                TextButton(onClick = {
                    startActivity(Intent(ASettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
                }) { Text("Bateria") }
            }

            Num("Partia", s.batchLimit) { v -> s = s.copy(batchLimit = v); lifecycleScope.launch { Prefs.setBatch(this@MainActivity, v) } }
            Num("Przerwa min (s)", s.delayMinSec) { v -> s = s.copy(delayMinSec = v); lifecycleScope.launch { Prefs.setDelays(this@MainActivity, v, s.delayMaxSec) } }
            Num("Przerwa max (s)", s.delayMaxSec) { v -> s = s.copy(delayMaxSec = v); lifecycleScope.launch { Prefs.setDelays(this@MainActivity, s.delayMinSec, v) } }
            Num("Zmiana IP co N", s.rotateEvery) { v -> s = s.copy(rotateEvery = v); lifecycleScope.launch { Prefs.setRotate(this@MainActivity, v) } }
            Field("ID jednostki", s.voteId) { v -> s = s.copy(voteId = v); lifecycleScope.launch { Prefs.setVoteId(this@MainActivity, v) } }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(s.mailEnabled, { v -> s = s.copy(mailEnabled = v); lifecycleScope.launch { Prefs.setMailEnabled(this@MainActivity, v) } })
                Text("  Potwierdzanie mailem")
            }
            Field("Login IMAP", s.mailUser) { v -> s = s.copy(mailUser = v); lifecycleScope.launch { Prefs.setMailUser(this@MainActivity, v) } }
            Field("HasĹ‚o", s.mailPass) { v -> s = s.copy(mailPass = v); lifecycleScope.launch { Prefs.setMailPass(this@MainActivity, v) } }
            Field("Host IMAP", s.mailHost) { v -> s = s.copy(mailHost = v); lifecycleScope.launch { Prefs.setMailHost(this@MainActivity, v) } }
            OutlinedButton(onClick = {
                lifecycleScope.launch {
                    toast(MailChecker.test(MailChecker.Cfg(s.mailHost, s.mailPort, s.mailUser, s.mailPass)))
                }
            }) { Text("Test IMAP") }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    lifecycleScope.launch { toast("Z assets: ${AliasImporter.fromAssets(this@MainActivity, repo)}") }
                }) { Text("Import assets") }
                OutlinedButton(onClick = { open.launch(arrayOf("text/plain", "*/*")) }) { Text("Import plik") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { save.launch("zaglosowane.txt") }) { Text("Eksport") }
                OutlinedButton(onClick = { lifecycleScope.launch { repo.requeueFailed() } }) { Text("PowtĂłrz bĹ‚Ä™dy") }
                OutlinedButton(onClick = { lifecycleScope.launch { repo.resetAll() } }) { Text("Reset") }
            }

            Spacer(Modifier.height(12.dp))
            logs.take(100).forEach {
                Text("${it.email.substringBefore('@')} Â· ${it.result} ${it.note ?: ""}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    @Composable
    private fun RowScope.Stat(label: String, v: Int) {
        Column(Modifier.weight(1f)) {
            Text("$v", style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }

    @Composable
    private fun Num(label: String, v: Int, on: (Int) -> Unit) = OutlinedTextField(
        value = v.toString(), onValueChange = { it.toIntOrNull()?.let(on) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )

    @Composable
    private fun Field(label: String, v: String, on: (String) -> Unit) = OutlinedTextField(
        value = v, onValueChange = on, label = { Text(label) }, singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
