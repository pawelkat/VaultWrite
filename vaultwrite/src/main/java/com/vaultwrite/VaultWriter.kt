package com.vaultwrite

import android.content.Context
import android.content.Intent
import java.time.LocalDate

/**
 * Android façade: storage check + [VaultFileWriter]. Blocking IO — call off the main thread.
 * [vaultRoot] is read on every call so apps can expose an editable root in Settings.
 */
class VaultWriter(
    context: Context,
    private val vaultRoot: () -> String = { VaultPaths.DEFAULT_VAULT_ROOT },
) {
    private val storage = StorageAccess(context)

    fun hasWriteAccess(): Boolean = storage.hasWriteAccess()

    fun manageAccessIntent(): Intent = storage.manageAllFilesIntent()

    fun manageAccessFallbackIntent(): Intent = storage.manageAllFilesFallbackIntent()

    fun appendToDaily(
        date: LocalDate,
        heading: String,
        bullet: String,
        dedupeKey: String? = null,
    ): VaultWriteResult {
        val writer = VaultFileWriter(vaultRoot())
        if (!hasWriteAccess()) {
            return VaultWriteResult.NeedStoragePermission(
                VaultPaths.dailyFile(writer.root, date).absolutePath,
            )
        }
        return writer.appendToDaily(date, heading, bullet, dedupeKey)
    }

    fun writeNote(relativePath: String, markdown: String): VaultWriteResult {
        val writer = VaultFileWriter(vaultRoot())
        if (!hasWriteAccess()) {
            val abs = runCatching { VaultPaths.resolve(writer.root, relativePath).absolutePath }
                .getOrDefault(relativePath)
            return VaultWriteResult.NeedStoragePermission(abs)
        }
        return writer.writeNote(relativePath, markdown)
    }

    fun readNote(relativePath: String): String? =
        if (hasWriteAccess()) VaultFileWriter(vaultRoot()).readNote(relativePath) else null
}
