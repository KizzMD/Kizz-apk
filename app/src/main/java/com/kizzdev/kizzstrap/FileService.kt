package com.kizzdev.kizzstrap

import java.io.File
import kotlin.system.exitProcess

/** Berjalan di proses terpisah dengan hak akses shell (dijalankan oleh Shizuku). */
class FileService : IFileService.Stub() {

    private fun allowed(path: String): Boolean {
        val base = "/sdcard/Android/data/"
        if (path.contains("..") || !path.startsWith(base)) return false
        val pkg = path.removePrefix(base).substringBefore('/')
        return pkg.contains("roblox", ignoreCase = true)
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun listDir(path: String): String = try {
        val names = File(path).list()
        if (names == null) "ERR: folder tidak bisa dibaca / tidak ada"
        else names.sorted().joinToString("\n").ifEmpty { "(kosong)" }
    } catch (e: Exception) {
        "ERR: ${e.message}"
    }

    override fun readFile(path: String): String = try {
        File(path).readText()
    } catch (e: Exception) {
        "ERR: ${e.message}"
    }

    override fun writeFile(path: String, content: String): String = try {
        val f = File(path)
        f.parentFile?.mkdirs()
        f.writeText(content)
        "OK: tersimpan ke $path"
    } catch (e: Exception) {
        "ERR: ${e.message}"
    }

    override fun copyFile(from: String, to: String): String = try {
        File(from).copyTo(File(to), overwrite = true)
        "OK: $from -> $to"
    } catch (e: Exception) {
        "ERR: ${e.message}"
    }

    override fun findFiles(root: String, name: String, maxDepth: Int): String = try {
        val out = ArrayList<String>()
        fun walk(f: File, depth: Int) {
            if (depth > maxDepth || out.size >= 60) return
            val kids = f.listFiles() ?: return
            for (k in kids) {
                if (k.name.contains(name, ignoreCase = true)) out.add(k.absolutePath)
                if (k.isDirectory) walk(k, depth + 1)
            }
        }
        walk(File(root), 0)
        if (out.isEmpty()) "(tidak ditemukan)" else out.joinToString("\n")
    } catch (e: Exception) {
        "ERR: ${e.message}"
    }

    override fun deleteRecursive(path: String, keepRoot: Boolean): String = try {
        if (!allowed(path)) {
            "ERR: path di luar folder Roblox ditolak"
        } else {
            val f = File(path)
            if (!f.exists()) {
                "OK: folder tidak ada, tidak ada yang dihapus"
            } else {
                var n = 0
                f.walkBottomUp().forEach {
                    if (!(keepRoot && it == f)) {
                        if (it.delete()) n++
                    }
                }
                "OK: $n item dihapus"
            }
        }
    } catch (e: Exception) {
        "ERR: ${e.message}"
    }

    override fun exec(command: String): String = try {
        val p = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText().trim()
        val code = p.waitFor()
        (if (out.isEmpty()) "(tanpa output)" else out) + "\n[exit $code]"
    } catch (e: Exception) {
        "ERR: ${e.message}"
    }

    override fun sizeOf(path: String): Long = try {
        File(path).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    } catch (e: Exception) {
        0L
    }
}
