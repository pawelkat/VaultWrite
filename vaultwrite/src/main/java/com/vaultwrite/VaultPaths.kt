package com.vaultwrite

import java.io.File
import java.time.LocalDate

object VaultPaths {
    /** DriveSync mirror of the Obsidian vault on the S24 Ultra (same as ToDaily / VaultHome). */
    const val DEFAULT_VAULT_ROOT = "/sdcard/DriveSyncFiles/Vault"
    const val DAILY_FOLDER = "Daily"
    const val NOTES_FOLDER = "Notes"

    fun dailyRelativePath(date: LocalDate): String = "$DAILY_FOLDER/$date.md"

    fun dailyFile(vaultRoot: String, date: LocalDate): File = resolve(vaultRoot, dailyRelativePath(date))

    fun noteRelativePath(title: String): String = "$NOTES_FOLDER/${title.trim()}.md"

    /**
     * Resolve [relativePath] under [vaultRoot]. Rejects absolute paths and `..` escapes so a
     * caller can never write outside the vault.
     */
    fun resolve(vaultRoot: String, relativePath: String): File {
        val rel = relativePath.trim().replace('\\', '/')
        require(rel.isNotEmpty()) { "relative path must not be blank" }
        require(!rel.startsWith("/")) { "path must be relative to the vault: $relativePath" }
        val parts = rel.split('/').filter { it.isNotEmpty() && it != "." }
        require(parts.none { it == ".." }) { "path must stay inside the vault: $relativePath" }
        return File(File(vaultRoot.trim().ifEmpty { DEFAULT_VAULT_ROOT }), parts.joinToString(File.separator))
    }
}
