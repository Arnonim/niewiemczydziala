package pl.ospzelazkow.glos

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings as ASettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.ospzelazkow.glos.a11y.RunState
import pl.ospzelazkow.glos.data.AliasImporter
import pl.ospzelazkow.glos.data.Prefs
import pl.ospzelazkow.glos.data.Settings
import pl.ospzelazkow.glos.svc.MailChecker
import pl.ospzelazkow.glos.svc.VoteService
import pl.ospzelazkow.glos.util.FileLog

class MainActivity : ComponentActivity() {

    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val pickTxt = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            importMsg = "Import…"
            val n = runCatching { AliasImporter.fromUri(applicationContext, OspApp.instance.repo, uri) }
                .getOrDefault(-1)
            importMsg = if (n >= 0) "Zaimportowano: $n" else "Błąd importu"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) { Panel() }
            }
        }
    }

    @Composable
    private fun Panel() {
        val ctx = LocalContext.current
        val scope = rememberCoroutineScope()
        val repo = OspApp.instance.repo

        var s by remember { mutableStateOf(Settings()) }
        var mailMsg by remember { mutableStateOf("") }
        var running by remember { mutableStateOf(RunState.running) }

        val remaining by repo.remaining.collectAsState(initial = 0)
        val voted by repo.voted.collectAsState(initial = 0)
        val failed by repo.failed.collectAsState(initial = 0)
        val rate by repo.rate.collectAsState(initial = 0)
        val total by repo.total.collectAsState(initial = 0)

        LaunchedEffect(Unit) {
            s = Prefs.read(ctx)
            if (repo.total.first() == 0) {
                importMsg = "Ładowanie wbudowanej listy…"
                val n = runCatching { AliasImporter.fromAssets(ctx, repo) }.getOrDefault(0)
                importMsg = "Wbudowana lista: $n"
            }
            while (true) {
                running = RunState.running
                delay(500)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("OSP Głosowanie v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge)
            Text("Aliasy: $total   Pozostało: $remaining   Potwierdzone: $voted   Błędy: $failed   Limit IP: $rate")
            Text(if (RunState.a11yConnected) "Dostępność: podłączona" else "Dostępność: BRAK")
            if (importMsg.isNotEmpty()) Text(importMsg, style = MaterialTheme.typography.bodySmall)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lista adresów", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { pickTxt.launch(arrayOf("text/plain", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Import z pliku") }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    runCatching { repo.resetAll() }
                                    importMsg = "Baza wyczyszczona"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Wyczyść bazę") }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Głosowanie", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = s.voteId,
                        onValueChange = { v -> s = s.copy(voteId = v); scope.launch { Prefs.setVoteId(ctx, v) } },
                        label = { Text("ID głosowania") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = s.batchLimit.toString(),
                        onValueChange = { v -> val n = v.toIntOrNull() ?: 150; s = s.copy(batchLimit = n); scope.launch { Prefs.setBatch(ctx, n) } },
                        label = { Text("Limit partii") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = s.delayMinSec.toString(),
                            onValueChange = { v -> val n = v.toIntOrNull() ?: 25; s = s.copy(delayMinSec = n); scope.launch { Prefs.setDelays(ctx, n, s.delayMaxSec) } },
                            label = { Text("Min s") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = s.delayMaxSec.toString(),
                            onValueChange = { v -> val n = v.toIntOrNull() ?: 45; s = s.copy(delayMaxSec = n); scope.launch { Prefs.setDelays(ctx, s.delayMinSec, n) } },
                            label = { Text("Max s") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = s.rotateEvery.toString(),
                            onValueChange = { v -> val n = v.toIntOrNull() ?: 1; s = s.copy(rotateEvery = n); scope.launch { Prefs.setRotate(ctx, n) } },
                            label = { Text("Co ile IP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Podszywanie się pod UA")
                        Switch(checked = s.uaSpoof, onCheckedChange = { v -> s = s.copy(uaSpoof = v); scope.launch { Prefs.setUaSpoof(ctx, v) } })
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Poczta (IMAP)", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = s.mailEnabled, onCheckedChange = { v -> s = s.copy(mailEnabled = v); scope.launch { Prefs.setMailEnabled(ctx, v) } })
                    }
                    OutlinedTextField(
                        value = s.mailHost,
                        onValueChange = { v -> s = s.copy(mailHost = v); scope.launch { Prefs.setMailHost(ctx, v) } },
                        label = { Text("Host") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = s.mailPort.toString(),
                        onValueChange = { v -> val n = v.toIntOrNull() ?: 993; s = s.copy(mailPort = n); scope.launch { Prefs.setMailPort(ctx, n) } },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = s.mailUser,
                        onValueChange = { v -> s = s.copy(mailUser = v); scope.launch { Prefs.setMailUser(ctx, v) } },
                        label = { Text("Login") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = s.mailPass,
                        onValueChange = { v -> s = s.copy(mailPass = v); scope.launch { Prefs.setMailPass(ctx, v) } },
                        label = { Text("Hasło") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(onClick = {
                        scope.launch {
                            mailMsg = "Test..."
                            mailMsg = MailChecker.test(MailChecker.Cfg(s.mailHost, s.mailPort, s.mailUser, s.mailPass))
                        }
                    }) { Text("Test IMAP") }
                    if (mailMsg.isNotEmpty()) Text(mailMsg, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { VoteService.start(ctx) },
                    enabled = !running && remaining > 0,
                    modifier = Modifier.weight(1f)
                ) { Text("START") }
                OutlinedButton(
                    onClick = { VoteService.stop(ctx) },
                    enabled = running,
                    modifier = Modifier.weight(1f)
                ) { Text("STOP") }
            }

            OutlinedButton(
                onClick = { runCatching { startActivity(Intent(ASettings.ACTION_ACCESSIBILITY_SETTINGS)) } },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ustawienia dostępności") }

            OutlinedButton(
                onClick = {
                    runCatching {
                        val pm = getSystemService(PowerManager::class.java)
                        if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                            startActivity(
                                Intent(ASettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Wyłącz optymalizację baterii") }

            OutlinedButton(
                onClick = {
                    runCatching {
                        val f = FileLog.file() ?: return@runCatching
                        val uri = FileProvider.getUriForFile(ctx, BuildConfig.APPLICATION_ID + ".fileprovider", f)
                        val i = Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(Intent.createChooser(i, "Udostępnij log"))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Udostępnij log") }

            Spacer(Modifier.height(24.dp))
        }
    }

    private var importMsg by mutableStateOf("")
}
