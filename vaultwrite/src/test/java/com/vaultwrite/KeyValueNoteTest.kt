package com.vaultwrite

import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyValueNoteTest {
    @Test
    fun renderAndParseRoundTrip() {
        val rows = listOf("monthlyTargetVisits" to "12", "path" to "a|b", "vaultRoot" to "/sdcard/DriveSyncFiles/Vault")
        val md = KeyValueNote.render(
            title = "GymMonth settings",
            source = "GymMonth",
            tags = listOf("gymmonth", "settings"),
            rows = rows,
            updatedAt = OffsetDateTime.of(2026, 11, 2, 13, 5, 7, 123, ZoneOffset.ofHours(1)),
            intro = "Written by GymMonth.",
        )
        assertTrue(md.startsWith("---\ntitle: GymMonth settings\nsource: GymMonth\nupdated: 2026-11-02T13:05:07+01:00\n"))
        assertTrue(md.contains("| path | a\\|b |"))
        assertEquals(rows.toMap(), KeyValueNote.parse(md))
    }
}
