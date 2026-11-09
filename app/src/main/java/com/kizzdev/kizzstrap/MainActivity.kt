package com.kizzdev.kizzstrap

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val robloxPkg = "com.roblox.client"

    private var service by mutableStateOf<IFileService?>(null)
    private var ready by mutableStateOf(false)
    private var status by mutableStateOf("Memeriksa Shizuku...")
    private var logText by mutableStateOf("")
    private var browse by mutableStateOf("/sdcard/Android/data/$robloxPkg")
    private var path by mutableStateOf(
        "/sdcard/Android/data/$robloxPkg/files/ClientSettings/ClientAppSettings.json"
    )
    private var json by mutableStateOf("{\n}")

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IFileService.Stub.asInterface(binder)
            logText = "Service Shizuku terhubung"
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
        }
    }

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener { ready = false; service = null; refresh() }
    private val permResult = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    private fun serviceArgs() = Shizuku.UserServiceArgs(
        ComponentName(packageName, FileService::class.java.name)
    ).daemon(false).processNameSuffix("fs").debuggable(false).version(1)

    private fun refresh() {
        if (!Shizuku.pingBinder()) {
            ready = false
            status = "Shizuku belum berjalan. Buka aplikasi Shizuku lalu Start."
            return
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            ready = false
            status = "Shizuku jalan, izin belum diberikan."
            return
        }
        ready = true
        status = "Shizuku siap"
        if (service == null) Shizuku.bindUserService(serviceArgs(), conn)
    }

    private fun requestPerm() {
        if (!Shizuku.pingBinder()) {
            status = "Shizuku belum berjalan."
            return
        }
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            logText = "Izin ditolak permanen. Buka aplikasi Shizuku > kelola izin aplikasi."
            return
        }
        Shizuku.requestPermission(100)
    }

    private fun act(block: (IFileService) -> String) {
        val s = service
        if (s == null) {
            logText = "Service belum terhubung. Cek status Shizuku."
            return
        }
        logText = try {
            block(s)
        } catch (e: Exception) {
            "ERR: ${e.message}"
        }
    }

    private fun apply() {
        try {
            JSONObject(json)
        } catch (e: Exception) {
            logText = "JSON tidak valid: ${e.message}"
            return
        }
        act { it.writeFile(path, json) }
    }

    private fun launchRoblox() {
        val i = packageManager.getLaunchIntentForPackage(robloxPkg)
        if (i == null) logText = "Roblox belum terpasang" else startActivity(i)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permResult)
        setContent { MaterialTheme { Screen() } }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permResult)
        super.onDestroy()
    }

    @Composable
    private fun Screen() {
        val ok = ready && service != null
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("KizzStrap", style = MaterialTheme.typography.headlineMedium)
            Text(status)
            Button(onClick = { requestPerm() }, enabled = !ready) { Text("Beri izin Shizuku") }

            Text("1. Cari lokasi file", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = browse, onValueChange = { browse = it },
                label = { Text("Folder yang dilihat") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = { act { it.listDir(browse) } }, enabled = ok) {
                Text("Lihat isi folder")
            }

            Text("2. Edit pengaturan", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = path, onValueChange = { path = it },
                label = { Text("Path file pengaturan") }, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = json, onValueChange = { json = it },
                label = { Text("Isi JSON") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { json = "{\n  \"DFIntTaskSchedulerTargetFps\": 60\n}" }) { Text("Contoh 60 FPS") }
                OutlinedButton(onClick = { json = "{\n  \"DFIntTaskSchedulerTargetFps\": 120\n}" }) { Text("Contoh 120 FPS") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    act { s ->
                        val r = s.readFile(path)
                        if (!r.startsWith("ERR")) json = r
                        if (r.startsWith("ERR")) r else "Dibaca (${r.length} karakter)"
                    }
                }, enabled = ok) { Text("Baca") }
                OutlinedButton(onClick = { act { it.copyFile(path, "$path.bak") } }, enabled = ok) { Text("Backup") }
                OutlinedButton(onClick = { act { it.copyFile("$path.bak", path) } }, enabled = ok) { Text("Restore") }
            }
            Button(onClick = { apply() }, enabled = ok, modifier = Modifier.fillMaxWidth()) {
                Text("Terapkan")
            }
            Button(onClick = { launchRoblox() }, modifier = Modifier.fillMaxWidth()) {
                Text("Buka Roblox")
            }

            Text("Log", style = MaterialTheme.typography.titleMedium)
            Text(logText)
        }
    }
}
