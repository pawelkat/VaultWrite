package com.vaultwrite

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class VaultCatalogTest {
    private lateinit var root: File
    private val rel = "app_settings/GymMonth/default_gyms.json"

    @Before fun setUp() { root = Files.createTempDirectory("vault").toFile() }
    @After fun tearDown() { root.deleteRecursively() }

    private fun load(available: Boolean = true, accept: (String) -> Boolean = { it.isNotBlank() }) =
        VaultCatalog.loadOrSeed(root.absolutePath, rel, available, { "{\"bundled\":true}" }, accept)

    @Test
    fun seedsOnceWhenMissing() {
        val r = load()
        assertEquals(VaultCatalog.Source.SEEDED, r.source)
        assertEquals("{\"bundled\":true}", File(root, rel).readText())
        assertEquals(VaultCatalog.Source.VAULT, load().source)
    }

    @Test
    fun existingVaultFileWinsAndIsNeverOverwritten() {
        File(root, rel).apply { parentFile!!.mkdirs(); writeText("{\"user\":1}") }
        val r = load()
        assertEquals(VaultCatalog.Source.VAULT, r.source)
        assertEquals("{\"user\":1}", r.text)
        assertEquals("{\"user\":1}", File(root, rel).readText())
    }

    @Test
    fun invalidVaultCopyFallsBackWithoutOverwriting() {
        File(root, rel).apply { parentFile!!.mkdirs(); writeText("garbage") }
        val r = load(accept = { it.startsWith("{") })
        assertEquals(VaultCatalog.Source.BUNDLED, r.source)
        assertNotNull(r.error)
        assertEquals("garbage", File(root, rel).readText())
    }

    @Test
    fun noStorageAccessUsesBundledAndWritesNothing() {
        val r = load(available = false)
        assertEquals(VaultCatalog.Source.BUNDLED, r.source)
        assertEquals(false, File(root, rel).exists())
    }

    @Test
    fun missingVaultRootUsesBundled() {
        val r = VaultCatalog.loadOrSeed(File(root, "nope").absolutePath, rel, true, { "b" })
        assertEquals(VaultCatalog.Source.BUNDLED, r.source)
        assertEquals("b", r.text)
        assertEquals(false, File(root, "nope").exists())
    }
}
