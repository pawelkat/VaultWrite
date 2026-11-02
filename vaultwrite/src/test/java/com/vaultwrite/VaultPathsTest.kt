package com.vaultwrite

import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultPathsTest {
    @Test
    fun dailyPath() {
        assertEquals("Daily/2026-11-02.md", VaultPaths.dailyRelativePath(LocalDate.of(2026, 11, 2)))
        assertEquals(
            File("/sdcard/DriveSyncFiles/Vault/Daily/2026-11-02.md"),
            VaultPaths.dailyFile(VaultPaths.DEFAULT_VAULT_ROOT, LocalDate.of(2026, 11, 2)),
        )
    }

    @Test
    fun noteRelativePath() {
        assertEquals("Notes/GymMonth settings.md", VaultPaths.noteRelativePath(" GymMonth settings "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDotDot() {
        VaultPaths.resolve("/v", "Notes/../../x.md")
    }
}
