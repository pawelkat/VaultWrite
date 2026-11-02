package com.vaultwrite

sealed class VaultWriteResult {
    data class Saved(
        /** Path relative to the vault root, e.g. `Daily/2026-11-02.md`. */
        val relativePath: String,
        val absolutePath: String,
        val createdFile: Boolean,
        /** True when nothing was written because the entry/content was already there. */
        val duplicate: Boolean,
        val bullet: String? = null,
    ) : VaultWriteResult()

    data class NeedStoragePermission(val absolutePath: String) : VaultWriteResult()

    data class Failed(val message: String, val absolutePath: String? = null) : VaultWriteResult()

    val isOk: Boolean get() = this is Saved
}
