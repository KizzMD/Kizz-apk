package com.kizzdev.kizzstrap

import java.io.File
import kotlin.system.exitProcess

/** Berjalan di proses terpisah dengan hak akses shell (dijalankan oleh Shizuku). */
class FileService : IFileService.Stub() {

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
}
