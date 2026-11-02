package com.vaultwrite

import java.io.File
import java.nio.charset.StandardCharsets

/**
 * User-editable catalog files (e.g. `default_gyms.json`) that ship inside the APK but live in
 * the vault once seeded:
 *
 * 1. Vault file missing → copy the bundled text there once ([Source.SEEDED]).
 * 2. Vault file exists → always use it ([Source.VAULT]); it is **never** overwritten.
 * 3. Vault unavailable / unreadable / rejected by [accept] → bundled text ([Source.BUNDLED]).
 *
 * Blocking IO; never throws.
 */
object VaultCatalog {

    enum class Source { VAULT, SEEDED, BUNDLED }

    data class Loaded(
        val text: String,
        val source: Source,
        /** Absolute vault path that was tried (null if the path itself was invalid). */
        val vaultPath: String?,
        val error: String? = null,
    )

    /**
     * @param vaultAvailable false when storage access is not granted — skips vault IO entirely.
     * @param accept validate vault text (e.g. parses + non-empty); false → fall back to bundled.
     */
    fun loadOrSeed(
        vaultRoot: String,
        relativePath: String,
        vaultAvailable: Boolean,
        bundled: () -> String,
        accept: (String) -> Boolean = { it.isNotBlank() },
    ): Loaded {
        val file: File = runCatching { VaultPaths.resolve(vaultRoot, relativePath) }
            .getOrElse { return Loaded(bundled(), Source.BUNDLED, null, it.message) }
        val path = file.absolutePath
        if (!vaultAvailable) return Loaded(bundled(), Source.BUNDLED, path, "no storage access")
        val rootDir = File(vaultRoot.trim().ifEmpty { VaultPaths.DEFAULT_VAULT_ROOT })
        if (!rootDir.isDirectory) return Loaded(bundled(), Source.BUNDLED, path, "vault root missing")

        if (file.exists()) {
            val text = runCatching { file.readText(StandardCharsets.UTF_8) }
                .getOrElse { return Loaded(bundled(), Source.BUNDLED, path, it.message ?: "unreadable") }
            val ok = runCatching { accept(text) }.getOrDefault(false)
            return if (ok) {
                Loaded(text, Source.VAULT, path)
            } else {
                Loaded(bundled(), Source.BUNDLED, path, "vault copy invalid; using bundled")
            }
        }

        val seed = bundled()
        return runCatching {
            file.parentFile?.let { if (!it.isDirectory && !it.mkdirs()) error("cannot create ${it.absolutePath}") }
            // createNewFile is atomic: never clobber a file that appeared meanwhile.
            if (file.createNewFile()) {
                file.writeText(seed, StandardCharsets.UTF_8)
                Loaded(seed, Source.SEEDED, path)
            } else {
                val text = file.readText(StandardCharsets.UTF_8)
                if (accept(text)) Loaded(text, Source.VAULT, path) else Loaded(seed, Source.BUNDLED, path, "vault copy invalid")
            }
        }.getOrElse { Loaded(seed, Source.BUNDLED, path, it.message ?: it::class.java.simpleName) }
    }
}
