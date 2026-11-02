package com.vaultwrite

import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultFileWriterTest {
    private lateinit var root: File
    private val date = LocalDate.of(2026, 11, 2)

    @Before
    fun setUp() {
        root = Files.createTempDirectory("vault").toFile()
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun appendCreatesDailyThenSkipsDuplicate() {
        val w = VaultFileWriter(root.absolutePath)
        val first = w.appendToDaily(date, "Workouts", "- Gym <!-- k1 -->", "k1") as VaultWriteResult.Saved
        assertTrue(first.createdFile)
        assertEquals("Daily/2026-11-02.md", first.relativePath)
        val file = File(root, "Daily/2026-11-02.md")
        assertTrue(file.isFile)
        val before = file.readText()
        val second = w.appendToDaily(date, "Workouts", "- Gym <!-- k1 -->", "k1") as VaultWriteResult.Saved
        assertTrue(second.duplicate)
        assertFalse(second.createdFile)
        assertEquals(before, file.readText())
    }

    @Test
    fun writeNoteCreatesOverwritesAndDetectsSame() {
        val w = VaultFileWriter(root.absolutePath)
        val a = w.writeNote("app_settings/App/settings.md", "one") as VaultWriteResult.Saved
        assertTrue(a.createdFile)
        assertEquals("one\n", w.readNote("app_settings/App/settings.md"))
        val b = w.writeNote("app_settings/App/settings.md", "two\n") as VaultWriteResult.Saved
        assertFalse(b.createdFile)
        assertFalse(b.duplicate)
        val c = w.writeNote("app_settings/App/settings.md", "two\n") as VaultWriteResult.Saved
        assertTrue(c.duplicate)
    }

    @Test
    fun rejectsPathEscape() {
        val w = VaultFileWriter(root.absolutePath)
        assertTrue(w.writeNote("../evil.md", "x") is VaultWriteResult.Failed)
        assertTrue(w.writeNote("/etc/evil.md", "x") is VaultWriteResult.Failed)
        assertNull(w.readNote("../evil.md"))
    }

    @Test
    fun failsSoftWhenRootIsAFile() {
        val blocker = File(root, "blocker").apply { writeText("x") }
        val w = VaultFileWriter(blocker.absolutePath)
        assertTrue(w.appendToDaily(date, "Workouts", "- Gym") is VaultWriteResult.Failed)
    }

    @Test
    fun blankRootFallsBackToDefault() {
        assertEquals(VaultPaths.DEFAULT_VAULT_ROOT, VaultFileWriter("  ").root)
    }
}
