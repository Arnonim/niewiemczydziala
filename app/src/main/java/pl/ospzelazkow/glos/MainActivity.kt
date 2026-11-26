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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.ospzelazkow.glos.data.Prefs
import pl.ospzelazkow.glos.data.Settings
import pl.ospzelazkow.glos.svc.RunState
import pl.ospzelazkow.glos.svc.VoteService
import pl.ospzelazkow.glos.util.FileLog
import pl.ospzelazkow.glos.util.MailChecker

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs

    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(applicationContext)

        if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Panel()
                }
            }
        }
    }

    @Composable
    private fun Panel() {
        val ctx = LocalContext.current
        val scope = rememberCoroutineScope()
        var s by remember { mutableStateOf(Settings()) }
        var log by remember { mutableStateOf("") }

        LaunchedEffect(Unit) { s = prefs.settings.first() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("OSP Głosowanie v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleLarge)
            Text("Stan: ${RunState.message}")
            Text("Głosy: ${RunState.voted}  Błędy: ${RunState.failed}  Limit IP: ${RunState.rate}")

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Poczta (IMAP)", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = s.imapHost,
                        onValueChange = { v -> s = s.copy(imapHost = v); scope.launch { prefs.save(s) } },
                        label = { Text("Host") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = s.imapPort.toString(),
                        onValueChange = { v -> s = s.copy(imapPort = v.toIntOrNull() ?: 993); scope.launch { prefs.save(s) } },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = s.imapUser,
                        onValueChange = { v -> s = s.copy(imapUser = v); scope.launch { prefs.save(s) } },
                        label = { Text("Login") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = s.imapPass,
                        onValueChange = { v -> s = s.copy(imapPass = v); scope.launch { prefs.save(s) } },
                        label = { Text("Hasło") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(onClick = {
                        scope.launch {
                            log = "Test..."
                            log = MailChecker.test(s)
                        }
                    }) { Text("Test IMAP") }
                    if (log.isNotEmpty()) Text(log, style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Głosowanie", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = s.email,
                        onValueChange = { v -> s = s.copy(email = v); scope.launch { prefs.save(s) } },
                        label = { Text("E-mail głosującego") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = s.delayMs.toString(),
                        onValueChange = { v -> s = s.copy(delayMs = v.toIntOrNull() ?: 1200); scope.launch { prefs.save(s) } },
                        label = { Text("Opóźnienie (ms)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { VoteService.start(ctx) },
                    enabled = !RunState.running,
                    modifier = Modifier.weight(1f)
                ) { Text("START") }
                OutlinedButton(
                    onClick = { VoteService.stop(ctx) },
                    enabled = RunState.running,
                    modifier = Modifier.weight(1f)
                ) { Text("STOP") }
            }

            OutlinedButton(
                onClick = {
                    runCatching {
                        startActivity(Intent(ASettings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ustawienia dostępności") }

            OutlinedButton(
                onClick = {
                    runCatching {
                        val pm = getSystemService(PowerManager::class.java)
                        if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                            startActivity(
                                Intent(ASettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                    .setData(Uri.parse("package:$packageName"))
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Wyłącz optymalizację baterii") }

            OutlinedButton(
                onClick = {
                    runCatching {
                        val f = FileLog.currentFile() ?: return@runCatching
                        val uri = FileProvider.getUriForFile(
                            ctx,
                            BuildConfig.APPLICATION_ID + ".fileprovider",
                            f
                        )
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
}
