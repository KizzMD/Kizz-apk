package com.kizzdev.kizzstrap

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.math.sin
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku

private const val ROBLOX = "com.roblox.client"
private const val URL_TIKTOK = "https://www.tiktok.com/@kizzdev"
private const val URL_WA = "https://wa.me/message/SJY6HXRWJT6EJ1"

private fun defPath(p: String) = "/sdcard/Android/data/$p/files/ClientSettings/ClientAppSettings.json"

private val BG = Color(0xFF07040F)
private val CARD = Color(0xD9130B24)
private val VIOLET = Color(0xFF8B3DFF)
private val MAGENTA = Color(0xFFC026D3)
private val LILAC = Color(0xFFD8B4FE)
private val TXT = Color(0xFFF3E8FF)
private val SUB = Color(0xFFA78BCA)
private val OKC = Color(0xFF34D399)
private val BAD = Color(0xFFF87171)
private val GRAD = Brush.horizontalGradient(listOf(VIOLET, MAGENTA))
private val GRAD_V = Brush.verticalGradient(listOf(VIOLET, MAGENTA))
private val FLAT = Brush.horizontalGradient(listOf(Color(0xFF241638), Color(0xFF241638)))
private val CUT = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp)
private val CUT_S = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)

data class Cfg(
    val on: Set<String> = emptySet(),
    val msaa: Int = 0,
    val frm: Int = 0,
    val renderer: Int = 0,
    val res: Int = 0,
    val fps: Int = 0
) {
    fun toJson(): JSONObject = JSONObject()
        .put("on", JSONArray(on.toList()))
        .put("msaa", msaa)
        .put("frm", frm)
        .put("renderer", renderer)
        .put("res", res)
        .put("fps", fps)

    companion object {
        fun from(o: JSONObject): Cfg {
            val a = o.optJSONArray("on")
            val s = HashSet<String>()
            if (a != null) for (i in 0 until a.length()) s.add(a.getString(i))
            return Cfg(s, o.optInt("msaa"), o.optInt("frm"), o.optInt("renderer"), o.optInt("res"), o.optInt("fps"))
        }
    }
}

class Tog(val id: String, val title: String, val desc: String, val flags: List<Pair<String, Any>>)

private val TOGGLES = listOf(
    Tog(
        "booster", "Booster grafis 1-ketukan",
        "Gabungan: MSAA 1x, tanpa pasca-proses, bayangan mati, rumput dihapus.",
        listOf(
            "FIntDebugForceMSAASamples" to 1,
            "FFlagDisablePostFx" to true,
            "FIntRenderShadowIntensity" to 0,
            "FIntFRMMinGrassDistance" to 0,
            "FIntFRMMaxGrassDistance" to 0,
            "FIntRenderGrassDetailStrands" to 0
        )
    ),
    Tog(
        "lod", "Kurangi detail model 3D",
        "Memakai versi sederhana model 3D jarak jauh untuk meringankan GPU.",
        listOf(
            "DFIntCSGLevelOfDetailSwitchingDistance" to 0,
            "DFIntCSGLevelOfDetailSwitchingDistanceL12" to 0,
            "DFIntCSGLevelOfDetailSwitchingDistanceL23" to 0,
            "DFIntCSGLevelOfDetailSwitchingDistanceL34" to 0
        )
    ),
    Tog(
        "tex", "Kurangi beban tekstur",
        "Menurunkan kualitas tekstur untuk menghemat RAM dan VRAM.",
        listOf("DFFlagTextureQualityOverrideEnabled" to true, "DFIntTextureQualityOverride" to 0)
    ),
    Tog(
        "mips", "Turunkan tekstur jarak jauh",
        "Melewati level mip tekstur untuk menghemat memori.",
        listOf("FIntDebugTextureManagerSkipMips" to 8)
    ),
    Tog(
        "shadow", "Nonaktifkan bayangan",
        "Mematikan bayangan dalam game untuk meringankan GPU.",
        listOf("FIntRenderShadowIntensity" to 0)
    ),
    Tog(
        "postfx", "Kurangi efek pasca-pemrosesan",
        "Menghapus bloom, blur, dan depth-of-field.",
        listOf("FFlagDisablePostFx" to true)
    ),
    Tog(
        "grass", "Hapus rumput",
        "Jarak render rumput jadi nol agar lebih lancar di area luar ruangan.",
        listOf(
            "FIntFRMMinGrassDistance" to 0,
            "FIntFRMMaxGrassDistance" to 0,
            "FIntRenderGrassDetailStrands" to 0
        )
    ),
    Tog(
        "wind", "Matikan angin global",
        "Menghentikan animasi angin pada tanaman dan partikel.",
        listOf("FFlagGlobalWindActivated" to false, "FFlagGlobalWindRendering" to false)
    ),
    Tog(
        "light", "Batasi cahaya lokal",
        "Mengurangi pembaruan cahaya lokal yang berat di GPU.",
        listOf("FIntRenderLocalLightUpdatesMax" to 1, "FIntRenderLocalLightUpdatesMin" to 1)
    ),
    Tog(
        "cull", "Culling cahaya GPU",
        "Membuang cahaya di luar pandangan supaya GPU lebih ringan.",
        listOf("FFlagFastGPULightCulling3" to true)
    ),
    Tog(
        "tele", "Matikan telemetri",
        "Mengurangi kerja CPU dan jaringan di latar belakang.",
        listOf(
            "FFlagDebugDisableTelemetryEphemeralCounter" to true,
            "FFlagDebugDisableTelemetryEphemeralStat" to true,
            "FFlagDebugDisableTelemetryEventIngest" to true,
            "FFlagDebugDisableTelemetryPoint" to true,
            "FFlagDebugDisableTelemetryV2Counter" to true,
            "FFlagDebugDisableTelemetryV2Event" to true,
            "FFlagDebugDisableTelemetryV2Stat" to true
        )
    ),
    Tog(
        "sky", "Langit abu-abu (SkyGray)",
        "Mengganti langit dengan latar abu-abu sederhana.",
        listOf("FFlagDebugSkyGray" to true)
    ),
    Tog(
        "fpscount", "Tampilkan penghitung FPS",
        "Menampilkan penghitung FPS bawaan Roblox di layar.",
        listOf("FFlagDebugDisplayFPS" to true)
    )
)

private val ALL_IDS = TOGGLES.map { it.id }.toSet()

private val BUILTIN = listOf(
    "Antilag Ringan" to Cfg(on = setOf("postfx", "grass", "shadow", "tele", "fpscount"), fps = 60),
    "Antilag Maksimal" to Cfg(
        on = setOf("booster", "lod", "tex", "shadow", "postfx", "grass", "light", "wind", "tele", "sky"),
        msaa = 1, frm = 1, renderer = 1, res = 720, fps = 60
    ),
    "Potato Ekstrem" to Cfg(on = ALL_IDS - "fpscount", msaa = 1, frm = 1, renderer = 1, res = 540, fps = 60),
    "Seimbang" to Cfg(on = setOf("postfx", "grass", "tele"), msaa = 2, frm = 8, fps = 90),
    "Pamer FPS 120" to Cfg(on = setOf("postfx", "shadow", "tele", "fpscount"), msaa = 1, fps = 120),
    "Bersih (tanpa FastFlag)" to Cfg()
)

private fun score(c: Cfg): Int {
    var s = c.on.size * 60 / TOGGLES.size
    if (c.fps > 0) s += 8
    if (c.msaa > 0) s += 8
    if (c.frm > 0) s += 8
    if (c.res > 0) s += 8
    if (c.renderer > 0) s += 8
    return s.coerceAtMost(100)
}

private fun buildJson(c: Cfg, customText: String): JSONObject {
    val o = JSONObject()
    fun put(k: String, v: Any) {
        o.put(k, if (v is Boolean) (if (v) "True" else "False") else v)
    }
    TOGGLES.filter { it.id in c.on }.forEach { t -> t.flags.forEach { (k, v) -> put(k, v) } }
    if (c.msaa > 0) put("FIntDebugForceMSAASamples", c.msaa)
    if (c.frm > 0) put("DFIntDebugFRMQualityLevelOverride", c.frm)
    when (c.renderer) {
        1 -> put("FFlagDebugGraphicsPreferVulkan", true)
        2 -> put("FFlagDebugGraphicsPreferOpenGL", true)
    }
    if (c.res > 0) {
        val kp = when (c.res) {
            540 -> 518
            720 -> 922
            else -> 2074
        }
        put("DFIntDebugDynamicRenderKiloPixels", kp)
    }
    if (c.fps > 0) put("DFIntTaskSchedulerTargetFps", c.fps)
    val extra = try {
        JSONObject(if (customText.isBlank()) "{}" else customText)
    } catch (e: Exception) {
        null
    }
    if (extra != null) {
        val keys = extra.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            o.put(k, extra.get(k))
        }
    }
    return o
}

class MainActivity : ComponentActivity() {

    private val prefs by lazy { getSharedPreferences("kizz", MODE_PRIVATE) }

    private var service by mutableStateOf<IFileService?>(null)
    private var ready by mutableStateOf(false)
    private var status by mutableStateOf("Memeriksa Shizuku...")
    private var logText by mutableStateOf("")

    private var splashOn by mutableStateOf(true)
    private var tab by mutableStateOf(0)
    private var cfg by mutableStateOf(Cfg())
    private var custom by mutableStateOf("{\n}")
    private var pkg by mutableStateOf(ROBLOX)
    private var pkgs by mutableStateOf(listOf(ROBLOX))
    private var manualPkg by mutableStateOf("")
    private var path by mutableStateOf(defPath(ROBLOX))
    private var found by mutableStateOf<List<String>>(emptyList())
    private var writeAllLoc by mutableStateOf(true)
    private var applyOnLaunch by mutableStateOf(true)
    private var saved by mutableStateOf<List<Pair<String, Cfg>>>(emptyList())
    private var presetName by mutableStateOf("")
    private var placeInput by mutableStateOf("")

    private var gmDown by mutableStateOf(0)
    private var gmFps by mutableStateOf(0)
    private var resPct by mutableStateOf(85)
    private var dnsSel by mutableStateOf(1)

    private val canUse: Boolean get() = ready && service != null

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = IFileService.Stub.asInterface(binder)
            logText = "Service Shizuku terhubung"
            scanPackages()
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
            status = "Shizuku jalan, izin belum diberikan"
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

    /** Jalankan tugas Shizuku di thread latar supaya UI tidak macet. */
    private fun bg(label: String, block: (IFileService) -> String) {
        val s = service
        if (s == null) {
            logText = "Service belum terhubung. Cek status Shizuku."
            return
        }
        logText = "$label..."
        Thread {
            val r = try {
                block(s)
            } catch (e: Exception) {
                "ERR: ${e.message}"
            }
            runOnUiThread { logText = "$label\n$r" }
        }.start()
    }

    private fun openUrl(u: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
        } catch (e: Exception) {
            logText = "Tidak bisa membuka link: ${e.message}"
        }
    }

    private fun selectPkg(p: String) {
        pkg = p
        path = defPath(p)
        found = emptyList()
        persist()
    }

    private fun scanPackages() {
        val s = service ?: return
        Thread {
            val r = try {
                s.exec("pm list packages | grep -i roblox")
            } catch (e: Exception) {
                "ERR: ${e.message}"
            }
            val list = r.lines()
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:").trim() }
            runOnUiThread {
                if (list.isNotEmpty()) {
                    pkgs = list
                    if (pkg !in list) selectPkg(list[0])
                }
                logText = "Scan Roblox: ${list.size} paket ditemukan\n" + list.joinToString("\n")
            }
        }.start()
    }

    private fun scanFiles() {
        val s = service
        if (s == null) {
            logText = "Service belum terhubung. Cek status Shizuku."
            return
        }
        val root = "/sdcard/Android/data/$pkg"
        logText = "Mencari file pengaturan..."
        Thread {
            val r = try {
                s.findFiles(root, "settings", 6)
            } catch (e: Exception) {
                "ERR: ${e.message}"
            }
            runOnUiThread {
                found = r.lines().filter { it.startsWith("/") }
                logText = "Pencarian selesai: ${found.size} hasil\n$r"
            }
        }.start()
    }

    private fun currentJson(): String = buildJson(cfg, custom).toString(2)

    /** Semua lokasi yang akan ditulis: path utama + lokasi ClientSettings yang terdeteksi. */
    private fun targetPaths(): List<String> {
        val l = LinkedHashSet<String>()
        l.add(path)
        if (writeAllLoc) {
            found.forEach { f ->
                val name = f.substringAfterLast('/')
                if (name.equals("ClientAppSettings.json", ignoreCase = true)) {
                    l.add(f)
                } else if (name.equals("ClientSettings", ignoreCase = true)) {
                    l.add("$f/ClientAppSettings.json")
                }
            }
        }
        return l.toList()
    }

    private fun writeAll(text: String, label: String, after: (() -> Unit)? = null) {
        val s = service
        if (s == null) {
            logText = "Service belum terhubung. Cek status Shizuku."
            return
        }
        val ps = targetPaths()
        logText = "$label..."
        Thread {
            val sb = StringBuilder()
            for (p in ps) {
                try {
                    if (s.readFile("$p.bak").startsWith("ERR")) s.copyFile(p, "$p.bak")
                    sb.append(s.writeFile(p, text)).append('\n')
                } catch (e: Exception) {
                    sb.append("ERR: ${e.message}\n")
                }
            }
            runOnUiThread {
                logText = "$label (${ps.size} lokasi)\n$sb"
                after?.invoke()
            }
        }.start()
    }

    private fun applyNow(label: String, after: (() -> Unit)? = null) {
        writeAll(currentJson(), label, after)
    }

    private fun stopThen(next: () -> Unit) {
        val s = service
        val p = pkg
        if (s == null) {
            next()
            return
        }
        Thread {
            try {
                s.exec("am force-stop $p")
            } catch (e: Exception) {
            }
            runOnUiThread { next() }
        }.start()
    }

    private fun prepareThen(next: () -> Unit) {
        if (service == null || !applyOnLaunch) {
            next()
            return
        }
        applyNow("Menerapkan FastFlag") { stopThen(next) }
    }

    /** Tes: hanya menyalakan penghitung FPS. Kalau muncul di Roblox, FastFlag bekerja di HP ini. */
    private fun testFlag() {
        writeAll("{\n  \"FFlagDebugDisplayFPS\": \"True\"\n}", "Tes FastFlag (penghitung FPS)") {
            stopThen { launchRoblox() }
        }
    }

    private fun launchRoblox() {
        val i = packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) {
            startActivity(i)
        } else {
            val p = pkg
            bg("Meluncurkan $p") { it.exec("monkey -p $p -c android.intent.category.LAUNCHER 1") }
        }
    }

    private fun launchAll() {
        prepareThen { launchRoblox() }
    }

    private fun boostAll() {
        val p = pkg
        bg("Boost 1 ketuk") {
            it.exec(
                "am kill-all; cmd game mode performance $p; " +
                    "am set-standby-bucket $p active; cmd deviceidle whitelist +$p; pm trim-caches 999G"
            )
        }
    }

    private fun applyGameMode() {
        var c = "cmd game set --mode performance"
        if (gmDown > 0) c += " --downscale ${gmDown / 100.0}"
        if (gmFps > 0) c += " --fps $gmFps"
        c += " $pkg"
        val cmd = c
        bg("Game Mode sistem") { it.exec(cmd) }
    }

    private fun applyScreenScale() {
        val pct = resPct
        bg("Resolusi layar $pct%") { s ->
            val out = s.exec("wm size")
            val m = Regex("Physical size: (\\d+)x(\\d+)").find(out)
            if (m == null) {
                "Gagal membaca ukuran layar:\n$out"
            } else {
                val w = m.groupValues[1].toInt() * pct / 100
                val h = m.groupValues[2].toInt() * pct / 100
                s.exec("wm size ${w}x$h")
            }
        }
    }

    private fun applyDns() {
        val cmd = when (dnsSel) {
            1 -> "settings put global private_dns_mode hostname; settings put global private_dns_specifier one.one.one.one"
            2 -> "settings put global private_dns_mode hostname; settings put global private_dns_specifier dns.google"
            else -> "settings put global private_dns_mode opportunistic"
        }
        bg("Private DNS") { it.exec(cmd) }
    }

    private fun joinGame() {
        val m = Regex("(\\d{4,})").find(placeInput)
        if (m == null) {
            logText = "Isi Place ID atau tempel link game Roblox"
            return
        }
        val id = m.value
        prepareThen {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("roblox://experiences/start?placeId=$id"))
                    .setPackage(pkg)
                startActivity(intent)
                logText = "Membuka game $id di $pkg"
            } catch (e: Exception) {
                logText = "Gagal membuka game: ${e.message}"
            }
        }
    }

    private fun copyText(t: String) {
        val cm = getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("kizz", t))
        logText = "JSON disalin ke clipboard"
    }

    private fun pasteJson() {
        val cm = getSystemService(ClipboardManager::class.java)
        val t = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
        if (t.isNullOrBlank()) {
            logText = "Clipboard kosong"
            return
        }
        try {
            custom = JSONObject(t).toString(2)
            logText = "JSON dari clipboard dimasukkan ke FastFlag tambahan"
        } catch (e: Exception) {
            logText = "Isi clipboard bukan JSON valid: ${e.message}"
        }
    }

    private fun loadSaved(): List<Pair<String, Cfg>> = try {
        val o = JSONObject(prefs.getString("presets", "{}") ?: "{}")
        val out = ArrayList<Pair<String, Cfg>>()
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            out.add(k to Cfg.from(o.getJSONObject(k)))
        }
        out
    } catch (e: Exception) {
        emptyList()
    }

    private fun storeSaved() {
        val o = JSONObject()
        saved.forEach { (n, c) -> o.put(n, c.toJson()) }
        prefs.edit().putString("presets", o.toString()).apply()
    }

    private fun persist() {
        prefs.edit()
            .putString("cfg", cfg.toJson().toString())
            .putString("custom", custom)
            .putString("path", path)
            .putString("pkg", pkg)
            .putBoolean("aol", applyOnLaunch)
            .putBoolean("wal", writeAllLoc)
            .apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            cfg = Cfg.from(JSONObject(prefs.getString("cfg", "{}") ?: "{}"))
        } catch (e: Exception) {
        }
        custom = prefs.getString("custom", custom) ?: custom
        pkg = prefs.getString("pkg", ROBLOX) ?: ROBLOX
        pkgs = listOf(pkg)
        path = prefs.getString("path", defPath(pkg)) ?: defPath(pkg)
        applyOnLaunch = prefs.getBoolean("aol", true)
        writeAllLoc = prefs.getBoolean("wal", true)
        saved = loadSaved()

        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permResult)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(primary = VIOLET, secondary = MAGENTA, background = BG, surface = CARD)
            ) { Root() }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onPause() {
        persist()
        super.onPause()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permResult)
        super.onDestroy()
    }

    // ---------------------------------------------------------------- UI

    @Composable
    private fun Root() {
        val prog = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            prog.animateTo(1f, tween(2300, easing = LinearEasing))
            splashOn = false
        }
        val a by animateFloatAsState(if (splashOn) 1f else 0f, tween(500), label = "fade")
        Box(Modifier.fillMaxSize()) {
            App()
            if (a > 0.01f) Splash(prog.value, Modifier.alpha(a))
        }
    }

    @Composable
    private fun App() {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A0B3B), BG, BG)))
        ) {
            Particles(Modifier.fillMaxSize())
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                StatusBar()
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (tab) {
                        0 -> TabHome()
                        1 -> TabFlags()
                        2 -> TabBoost()
                        else -> TabMore()
                    }
                    LogCard()
                    Spacer(Modifier.height(120.dp))
                }
            }
            BottomBar(Modifier.align(Alignment.BottomCenter).systemBarsPadding())
        }
    }

    @Composable
    private fun StatusBar() {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (canUse) OKC else BAD))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(status, color = TXT, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(pkg, color = SUB, fontSize = 11.sp)
            }
            if (!ready) Btn("Beri izin") { requestPerm() }
        }
    }

    @Composable
    private fun TabHome() {
        Box(Modifier.fillMaxWidth().height(210.dp).clip(CUT).border(1.dp, GRAD, CUT)) {
            Image(
                painterResource(R.drawable.kizz_logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA07040F), Color(0xF007040F)))
                )
            )
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    "KizzDev",
                    style = TextStyle(
                        brush = GRAD,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp
                    )
                )
                Text("ROBLOX ANTILAG SUITE", color = LILAC, fontSize = 11.sp, letterSpacing = 2.sp)
            }
        }
        Panel("Skor antilag") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Gauge(score(cfg) / 100f, "${score(cfg)}%", "ANTILAG", Modifier.size(150.dp))
                Spacer(Modifier.width(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat("Paket", pkg)
                    Stat("FPS target", if (cfg.fps == 0) "Bawaan" else "${cfg.fps}")
                    Stat("Optimasi aktif", "${cfg.on.size} dari ${TOGGLES.size}")
                    Stat("Shizuku", if (canUse) "Siap" else "Belum siap")
                }
            }
        }
        Panel("Aksi cepat") {
            Btn("BOOST 1 KETUK", Modifier.fillMaxWidth(), canUse, true) { boostAll() }
            Btn("LUNCURKAN ROBLOX", Modifier.fillMaxWidth(), true, true) { launchAll() }
            Label("Preset cepat (lalu tekan Luncurkan)")
            Chips(BUILTIN.map { it.first to it.first }, "") { n ->
                val p = BUILTIN.firstOrNull { it.first == n }
                if (p != null) {
                    cfg = p.second
                    logText = "Preset \"$n\" dipakai"
                }
            }
        }
        Panel("Tes grafik di Roblox") {
            Label(
                "Menyalakan penghitung FPS Roblox lalu membuka game. Kalau angka FPS muncul di layar Roblox, " +
                    "FastFlag bekerja di HP ini. Kalau tidak muncul, buka tab Lainnya dan pakai Deteksi otomatis."
            )
            Btn("Tes FastFlag sekarang", Modifier.fillMaxWidth(), canUse, false) { testFlag() }
        }
        Panel("Hubungi Admin") {
            Label("Butuh bantuan, key, atau mau request fitur? Hubungi admin KizzDev.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("TikTok @kizzdev", Modifier.weight(1f), true, true) { openUrl(URL_TIKTOK) }
                Btn("WhatsApp Admin", Modifier.weight(1f), true, true) { openUrl(URL_WA) }
            }
        }
    }

    @Composable
    private fun TabFlags() {
        Panel("Versi Roblox") {
            Label("Pilih Roblox yang mau dioptimasi (mendukung versi regional dan klon)")
            Chips(pkgs.map { it to it }, pkg) { selectPkg(it) }
            Btn("Scan Roblox terpasang", Modifier.fillMaxWidth(), canUse, false) { scanPackages() }
            Field(manualPkg, "Atau ketik nama paket manual") { manualPkg = it }
            Btn("Pakai paket ini", Modifier.fillMaxWidth(), true, false) {
                val p = manualPkg.trim()
                if (p.isNotEmpty()) {
                    if (p !in pkgs) pkgs = pkgs + p
                    selectPkg(p)
                }
            }
        }
        Panel("Mode optimasi") {
            SwitchRow(
                "Terapkan saat Luncurkan",
                "Tombol ▶ di tengah menulis FastFlag lalu membuka Roblox. Matikan untuk membuka Roblox tanpa perubahan.",
                applyOnLaunch
            ) { applyOnLaunch = it }
        }
        Panel("Kualitas grafis") {
            Label("Anti-aliasing (MSAA)")
            Chips(listOf("Bawaan" to 0, "1x" to 1, "2x" to 2, "4x" to 4), cfg.msaa) { cfg = cfg.copy(msaa = it) }
            Label("Kualitas render FRM: " + (if (cfg.frm == 0) "bawaan" else "level ${cfg.frm}"))
            Slider(
                value = cfg.frm.toFloat(),
                onValueChange = { cfg = cfg.copy(frm = it.roundToInt()) },
                valueRange = 0f..21f,
                steps = 20,
                colors = SliderDefaults.colors(
                    thumbColor = MAGENTA,
                    activeTrackColor = VIOLET,
                    inactiveTrackColor = Color(0xFF2A1A44)
                )
            )
            Label("Renderer")
            Chips(listOf("Otomatis" to 0, "Vulkan" to 1, "OpenGL" to 2), cfg.renderer) { cfg = cfg.copy(renderer = it) }
            Label("Resolusi render dinamis")
            Chips(
                listOf("Bawaan" to 0, "540p" to 540, "720p" to 720, "1080p" to 1080),
                cfg.res
            ) { cfg = cfg.copy(res = it) }
        }
        Panel("Target FPS") {
            Chips(
                listOf("Bawaan" to 0, "30" to 30, "60" to 60, "90" to 90, "120" to 120, "144" to 144, "240" to 240),
                cfg.fps
            ) { cfg = cfg.copy(fps = it) }
            Label("FPS sebenarnya tetap tergantung game dan GPU.")
        }
        Panel("Optimasi dan efek visual") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Aktifkan semua", Modifier.weight(1f), true, false) { cfg = cfg.copy(on = ALL_IDS) }
                Btn("Matikan semua", Modifier.weight(1f), true, false) { cfg = cfg.copy(on = emptySet()) }
            }
            TOGGLES.forEach { t ->
                SwitchRow(t.title, t.desc, t.id in cfg.on) { v ->
                    cfg = cfg.copy(on = if (v) cfg.on + t.id else cfg.on - t.id)
                }
            }
        }
        Panel("Terapkan") {
            Btn("Terapkan FastFlag sekarang", Modifier.fillMaxWidth(), canUse, true) { applyNow("Menerapkan") }
            Btn("Tes FastFlag (penghitung FPS)", Modifier.fillMaxWidth(), canUse, false) { testFlag() }
        }
    }

    @Composable
    private fun TabBoost() {
        Panel("Game Mode sistem (pasti bekerja di Roblox)") {
            Label("Menurunkan resolusi render dan membatasi FPS langsung dari sistem Android 12 ke atas, tanpa FastFlag.")
            Label("Downscale resolusi")
            Chips(
                listOf("Mati" to 0, "0.9" to 90, "0.8" to 80, "0.7" to 70, "0.6" to 60, "0.5" to 50),
                gmDown
            ) { gmDown = it }
            Label("Batas FPS")
            Chips(
                listOf("Mati" to 0, "30" to 30, "45" to 45, "60" to 60, "90" to 90, "120" to 120),
                gmFps
            ) { gmFps = it }
            Btn("Terapkan Game Mode", Modifier.fillMaxWidth(), canUse, true) { applyGameMode() }
            Btn("Reset Game Mode", Modifier.fillMaxWidth(), canUse, false) {
                bg("Reset Game Mode") { it.exec("cmd game reset $pkg") }
            }
        }
        Panel("Boost dan antilag") {
            ActionRow("Boost 1 ketuk", "Bersihkan RAM, game mode performa, prioritas aktif, bebas hemat baterai, bersihkan cache.", canUse) {
                boostAll()
            }
            ActionRow("Force stop Roblox", "Menutup Roblox sepenuhnya supaya pengaturan baru terbaca.", canUse) {
                bg("Force stop") { it.exec("am force-stop $pkg") }
            }
            ActionRow("Bersihkan cache Roblox", "Menghapus cache tanpa menghapus login atau data game.", canUse) {
                bg("Bersihkan cache") { it.deleteRecursive("/sdcard/Android/data/$pkg/cache", true) }
            }
            ActionRow("Bersihkan cache semua aplikasi", "Mengosongkan cache semua aplikasi untuk melegakan penyimpanan.", canUse) {
                bg("Trim cache") { it.exec("pm trim-caches 999G") }
            }
            ActionRow("Bersihkan RAM latar belakang", "Menutup aplikasi lain di latar belakang agar RAM lega.", canUse) {
                bg("Bersihkan RAM") { it.exec("am kill-all") }
            }
            ActionRow("Game mode: Performa", "Meminta sistem memberi prioritas performa (Android 12 ke atas).", canUse) {
                bg("Game mode Performa") { it.exec("cmd game mode performance $pkg") }
            }
            ActionRow("Prioritas aplikasi aktif", "Menaikkan prioritas Roblox di manajer aplikasi latar belakang.", canUse) {
                bg("Prioritas aktif") { it.exec("am set-standby-bucket $pkg active") }
            }
            ActionRow("Kecualikan dari hemat baterai", "Roblox tidak dibatasi mode hemat daya saat dimainkan.", canUse) {
                bg("Whitelist baterai") { it.exec("cmd deviceidle whitelist +$pkg") }
            }
            ActionRow("Kompilasi cepat", "Mengoptimalkan kode aplikasi Roblox. Bisa makan beberapa menit.", canUse) {
                bg("Kompilasi") { it.exec("cmd package compile -m speed-profile -f $pkg") }
            }
        }
        Panel("Layar dan refresh rate") {
            Label("Turunkan resolusi layar sistem agar GPU lebih ringan. Tekan Reset untuk kembali normal.")
            Chips(listOf("100%" to 100, "85%" to 85, "75%" to 75, "60%" to 60), resPct) { resPct = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Terapkan", Modifier.weight(1f), canUse, true) { applyScreenScale() }
                Btn("Reset layar", Modifier.weight(1f), canUse, false) {
                    bg("Reset layar") { it.exec("wm size reset") }
                }
            }
            Label("Refresh rate (tergantung HP)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("60Hz", Modifier.weight(1f), canUse, false) {
                    bg("Refresh 60Hz") { it.exec("settings put system min_refresh_rate 60; settings put system peak_refresh_rate 60") }
                }
                Btn("90Hz", Modifier.weight(1f), canUse, false) {
                    bg("Refresh 90Hz") { it.exec("settings put system min_refresh_rate 90; settings put system peak_refresh_rate 90") }
                }
                Btn("120Hz", Modifier.weight(1f), canUse, false) {
                    bg("Refresh 120Hz") { it.exec("settings put system min_refresh_rate 120; settings put system peak_refresh_rate 120") }
                }
            }
            Btn("Reset refresh rate", Modifier.fillMaxWidth(), canUse, false) {
                bg("Reset refresh rate") {
                    it.exec("settings delete system min_refresh_rate; settings delete system peak_refresh_rate")
                }
            }
        }
        Panel("Jaringan") {
            Label("Private DNS")
            Chips(listOf("Otomatis" to 0, "Cloudflare" to 1, "Google" to 2), dnsSel) { dnsSel = it }
            Btn("Terapkan DNS", Modifier.fillMaxWidth(), canUse, false) { applyDns() }
            ActionRow("Tes ping Roblox", "Mengukur ping ke server Roblox untuk cek lag jaringan.", canUse) {
                bg("Tes ping") { it.exec("ping -c 4 -W 2 www.roblox.com") }
            }
        }
        Panel("Gabung game cepat") {
            Field(placeInput, "Place ID atau link game Roblox") { placeInput = it }
            Btn("Buka game", Modifier.fillMaxWidth(), true, true) { joinGame() }
            Label("Memakai Roblox terpilih: $pkg")
        }
        Panel("Animasi sistem") {
            ActionRow("Animasi 0.5x", "Mempercepat animasi sistem agar terasa lebih responsif.", canUse) {
                bg("Animasi 0.5x") {
                    it.exec(
                        "settings put global window_animation_scale 0.5; " +
                            "settings put global transition_animation_scale 0.5; " +
                            "settings put global animator_duration_scale 0.5"
                    )
                }
            }
            ActionRow("Animasi normal 1x", "Mengembalikan animasi sistem seperti semula.", canUse) {
                bg("Animasi 1x") {
                    it.exec(
                        "settings put global window_animation_scale 1; " +
                            "settings put global transition_animation_scale 1; " +
                            "settings put global animator_duration_scale 1"
                    )
                }
            }
        }
        Panel("Info") {
            ActionRow("Info Roblox", "Versi terpasang dan ukuran cache.", canUse) {
                bg("Info Roblox") { s ->
                    val v = s.exec("dumpsys package $pkg | grep -m2 -E 'versionName|versionCode'")
                    val c = s.sizeOf("/sdcard/Android/data/$pkg/cache")
                    "$v\nCache: ${c / 1024 / 1024} MB"
                }
            }
            ActionRow("Info perangkat", "Model, versi Android, layar, dan RAM.", canUse) {
                bg("Info perangkat") {
                    it.exec(
                        "getprop ro.product.model; getprop ro.build.version.release; " +
                            "wm size; wm density; grep -m1 MemTotal /proc/meminfo"
                    )
                }
            }
        }
    }

    @Composable
    private fun TabMore() {
        Panel("Preset bawaan") {
            BUILTIN.forEach { (n, c) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(n, color = TXT, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Btn("Pakai") {
                        cfg = c
                        logText = "Preset \"$n\" dipakai. Tekan Terapkan atau tombol ▶."
                    }
                }
            }
        }
        Panel("Preset saya") {
            Field(presetName, "Nama preset") { presetName = it }
            Btn("Simpan konfigurasi saat ini", Modifier.fillMaxWidth(), true, true) {
                val n = presetName.trim()
                if (n.isEmpty()) {
                    logText = "Isi nama preset dulu"
                } else {
                    saved = saved.filter { it.first != n } + (n to cfg)
                    storeSaved()
                    logText = "Preset \"$n\" tersimpan"
                }
            }
            if (saved.isEmpty()) Label("Belum ada preset tersimpan.")
            saved.forEach { (n, c) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(n, color = TXT, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Btn("Pakai") {
                        cfg = c
                        logText = "Preset \"$n\" dipakai"
                    }
                    Btn("Hapus") {
                        saved = saved.filter { it.first != n }
                        storeSaved()
                    }
                }
            }
        }
        Panel("Lokasi file pengaturan") {
            Field(path, "Path ClientAppSettings.json") { path = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Deteksi otomatis", Modifier.weight(1f), canUse, false) { scanFiles() }
                Btn("Lihat folder", Modifier.weight(1f), canUse, false) {
                    bg("Isi folder") { it.listDir(path.substringBeforeLast('/')) }
                }
            }
            SwitchRow(
                "Tulis ke semua lokasi terdeteksi",
                "Menulis FastFlag ke path utama dan semua folder ClientSettings yang ditemukan.",
                writeAllLoc
            ) { writeAllLoc = it }
            if (found.isEmpty()) {
                Label("Tekan Deteksi otomatis. Hasilnya muncul di sini dan bisa langsung dipakai.")
            }
            found.forEach { f ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        f.removePrefix("/sdcard/Android/data/"),
                        color = LILAC, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    Btn("Pakai") {
                        path = if (f.endsWith(".json")) f else "$f/ClientAppSettings.json"
                        logText = "Path diganti ke $path"
                    }
                }
            }
        }
        Panel("FastFlag tambahan (JSON)") {
            Field(custom, "JSON tambahan", 120.dp) { custom = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Tempel clipboard", Modifier.weight(1f), true, false) { pasteJson() }
                Btn("Salin JSON akhir", Modifier.weight(1f), true, false) { copyText(currentJson()) }
            }
        }
        Panel("JSON yang akan ditulis") {
            SelectionContainer {
                Text(currentJson(), color = LILAC, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
        Panel("Aksi file") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Baca", Modifier.weight(1f), canUse, false) {
                    bg("Membaca file") { s ->
                        val r = s.readFile(path)
                        if (r.startsWith("ERR")) r else "Isi file:\n$r"
                    }
                }
                Btn("Backup", Modifier.weight(1f), canUse, false) {
                    bg("Backup") { it.copyFile(path, "$path.bak") }
                }
                Btn("Restore", Modifier.weight(1f), canUse, false) {
                    bg("Restore") { it.copyFile("$path.bak", path) }
                }
            }
            Btn("Reset FastFlag (kosongkan file)", Modifier.fillMaxWidth(), canUse, false) {
                writeAll("{}", "Reset FastFlag")
            }
            Btn("Terapkan sekarang", Modifier.fillMaxWidth(), canUse, true) { applyNow("Menerapkan") }
        }
        Panel("Admin KizzDev") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("TikTok @kizzdev", Modifier.weight(1f), true, false) { openUrl(URL_TIKTOK) }
                Btn("WhatsApp Admin", Modifier.weight(1f), true, false) { openUrl(URL_WA) }
            }
        }
    }

    @Composable
    private fun LogCard() {
        Panel("Log") {
            SelectionContainer {
                Text(
                    if (logText.isBlank()) "Belum ada aktivitas" else logText,
                    color = LILAC, fontFamily = FontFamily.Monospace, fontSize = 12.sp
                )
            }
        }
    }

    @Composable
    private fun BottomBar(modifier: Modifier) {
        val shape = CutCornerShape(topStart = 24.dp, bottomEnd = 24.dp)
        Box(modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Color(0xF2130B24))
                    .border(1.dp, Brush.horizontalGradient(listOf(VIOLET, MAGENTA)), shape)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavItem("🏠", "Beranda", 0, Modifier.weight(1f))
                NavItem("🚩", "FastFlag", 1, Modifier.weight(1f))
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(GRAD)
                        .border(2.dp, LILAC.copy(alpha = 0.6f), CircleShape)
                        .clickable { launchAll() },
                    contentAlignment = Alignment.Center
                ) { Text("▶", color = Color.White, fontSize = 26.sp) }
                NavItem("⚡", "Boost", 2, Modifier.weight(1f))
                NavItem("⚙️", "Lainnya", 3, Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun NavItem(icon: String, label: String, idx: Int, modifier: Modifier) {
        val on = tab == idx
        Column(
            modifier
                .clip(CUT_S)
                .clickable { tab = idx }
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Text(
                label,
                fontSize = 11.sp,
                color = if (on) LILAC else SUB,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ------------------------------------------------------------ efek visual

@Composable
private fun Particles(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "particles")
    val p = t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "pp"
    )
    Canvas(modifier) {
        val ph = p.value
        for (i in 0 until 34) {
            val sx = ((i * 37) % 100) / 100f
            val sp = 0.5f + ((i * 53) % 50) / 100f
            val y = size.height * (1f - ((ph * sp + i * 0.137f) % 1f))
            val x = size.width * sx + sin(ph * 6.2832f * sp + i) * 16f
            drawCircle(
                color = Color(0xFFB57BFF).copy(alpha = 0.10f + (i % 5) * 0.04f),
                radius = 1.5f + (i % 4),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun Gauge(value: Float, big: String, small: String, modifier: Modifier = Modifier) {
    val v by animateFloatAsState(value, tween(900), label = "gauge")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 20f
            val d = size.minDimension - sw
            val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            drawArc(
                color = Color(0xFF241638),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = tl,
                size = Size(d, d),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.linearGradient(
                    listOf(VIOLET, MAGENTA),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                startAngle = 135f,
                sweepAngle = 270f * v,
                useCenter = false,
                topLeft = tl,
                size = Size(d, d),
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(big, color = TXT, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text(small, color = SUB, fontSize = 11.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = SUB, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(value, color = TXT, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// ------------------------------------------------------------ layar loading

@Composable
private fun Splash(progress: Float, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "pulse")
    val s by t.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale"
    )
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF220B52), Color(0xFF07040F)))),
        contentAlignment = Alignment.Center
    ) {
        Particles(Modifier.fillMaxSize())
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.kizz_logo),
                contentDescription = null,
                modifier = Modifier.size(230.dp).scale(s).clip(CUT).border(1.5.dp, GRAD, CUT),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(26.dp))
            Text(
                "KIZZDEV",
                style = TextStyle(
                    brush = GRAD,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    letterSpacing = 6.sp
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    progress < 0.35f -> "Memuat modul..."
                    progress < 0.7f -> "Menyiapkan FastFlag..."
                    else -> "Menghubungkan Shizuku..."
                },
                color = SUB, fontSize = 12.sp, letterSpacing = 1.sp
            )
            Spacer(Modifier.height(18.dp))
            Box(Modifier.width(220.dp).height(6.dp).background(Color(0xFF241638))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(GRAD))
            }
        }
    }
}

// ------------------------------------------------------------ komponen UI

@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = CUT,
                clip = false,
                ambientColor = VIOLET.copy(alpha = 0.6f),
                spotColor = VIOLET.copy(alpha = 0.6f)
            )
            .clip(CUT)
            .background(CARD)
            .border(
                1.dp,
                Brush.linearGradient(listOf(VIOLET.copy(alpha = 0.8f), MAGENTA.copy(alpha = 0.25f))),
                CUT
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(16.dp).background(GRAD_V))
            Spacer(Modifier.width(8.dp))
            Text(
                title.uppercase(),
                color = LILAC,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 1.5.sp
            )
        }
        content()
    }
}

@Composable
private fun Label(t: String) {
    Text(t, color = SUB, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TXT, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(desc, color = SUB, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VIOLET,
                uncheckedThumbColor = SUB,
                uncheckedTrackColor = Color(0xFF2A1A44),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun <T> Chips(options: List<Pair<String, T>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, v) ->
            val on = v == selected
            Box(
                Modifier
                    .clip(CUT_S)
                    .background(if (on) GRAD else FLAT)
                    .clickable { onSelect(v) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    color = if (on) Color.White else SUB,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun Btn(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
    onClick: () -> Unit
) {
    val base = modifier.clip(CUT_S)
    val styled = if (filled) {
        base.background(GRAD)
    } else {
        base.background(Color(0xFF1D1133)).border(1.dp, VIOLET.copy(alpha = 0.6f), CUT_S)
    }
    Box(
        styled
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun ActionRow(title: String, desc: String, enabled: Boolean, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = TXT, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(desc, color = SUB, fontSize = 12.sp)
        Btn("Jalankan", Modifier.fillMaxWidth(), enabled, false, onClick)
    }
}

@Composable
private fun Field(value: String, label: String, minH: Dp = 0.dp, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().heightIn(min = minH),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TXT,
            unfocusedTextColor = TXT,
            focusedBorderColor = VIOLET,
            unfocusedBorderColor = Color(0xFF3A2860),
            cursorColor = LILAC,
            focusedLabelColor = LILAC,
            unfocusedLabelColor = SUB
        )
    )
}
