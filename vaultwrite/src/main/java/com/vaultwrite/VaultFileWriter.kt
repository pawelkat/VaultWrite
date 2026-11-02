package com.vaultwrite

import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDate

/**
 * Plain java.io reads/writes under a vault root. No Android, no network — DriveSync handles
 * sync. Blocking: call from an IO dispatcher. Never throws; failures become [VaultWriteResult.Failed].
 */
class VaultFileWriter(vaultRoot: String) {

    val root: String = vaultRoot.trim().ifEmpty { VaultPaths.DEFAULT_VAULT_ROOT }

    fun appendToDaily(
        date: LocalDate,
        heading: String,
        bullet: String,
        dedupeKey: String? = null,
    ): VaultWriteResult {
        val relative = VaultPaths.dailyRelativePath(date)
        val file = runCatching { VaultPaths.resolve(root, relative) }
            .getOrElse { return VaultWriteResult.Failed(it.message ?: "bad path") }
        return runCatching {
            val existing = readOrNull(file)
            val rewrite = DailyNoteAppender.append(existing, date, heading, bullet, dedupeKey)
            if (!rewrite.duplicate || existing.isNullOrBlank()) {
                write(file, rewrite.markdown)
            }
            VaultWriteResult.Saved(
                relativePath = relative,
                absolutePath = file.absolutePath,
                createdFile = rewrite.createdFile,
                duplicate = rewrite.duplicate,
                bullet = rewrite.bullet,
            )
        }.getOrElse { failed(it, file) }
    }

    /** Create or overwrite a whole note (e.g. `Notes/GymMonth settings.md`). Skips identical content. */
    fun writeNote(relativePath: String, markdown: String): VaultWriteResult {
        val file = runCatching { VaultPaths.resolve(root, relativePath) }
            .getOrElse { return VaultWriteResult.Failed(it.message ?: "bad path") }
        return runCatching {
            val body = if (markdown.endsWith("\n")) markdown else "$markdown\n"
            val existing = readOrNull(file)
            val same = existing == body
            if (!same) write(file, body)
            VaultWriteResult.Saved(
                relativePath = relativePath,
                absolutePath = file.absolutePath,
                createdFile = existing == null,
                duplicate = same,
            )
        }.getOrElse { failed(it, file) }
    }

    /** Returns the note text, or null if missing/unreadable. */
    fun readNote(relativePath: String): String? = runCatching {
        readOrNull(VaultPaths.resolve(root, relativePath))
    }.getOrNull()

    private fun readOrNull(file: File): String? =
        if (file.isFile) file.readText(StandardCharsets.UTF_8) else null

    private fun write(file: File, text: String) {
        file.parentFile?.let { if (!it.isDirectory && !it.mkdirs()) error("cannot create ${it.absolutePath}") }
        file.writeText(text, StandardCharsets.UTF_8)
    }

    private fun failed(e: Throwable, file: File) = VaultWriteResult.Failed(
        message = e.message ?: e::class.java.simpleName,
        absolutePath = file.absolutePath,
    )
}
