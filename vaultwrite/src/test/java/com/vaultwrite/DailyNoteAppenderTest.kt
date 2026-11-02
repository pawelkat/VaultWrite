package com.vaultwrite

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyNoteAppenderTest {
    private val date = LocalDate.of(2026, 11, 2)
    private val heading = "## Workouts"

    @Test
    fun normalizeHeadingAddsLevelTwo() {
        assertEquals("## Workouts", DailyNoteAppender.normalizeHeading("Workouts"))
        assertEquals("### Gym", DailyNoteAppender.normalizeHeading("  ### Gym "))
    }

    @Test
    fun normalizeBulletAddsDash() {
        assertEquals("- Gym", DailyNoteAppender.normalizeBullet("Gym"))
        assertEquals("- Gym", DailyNoteAppender.normalizeBullet("- Gym\n"))
    }

    @Test
    fun createsStubWithTemplateSectionsAndTargetHeading() {
        val r = DailyNoteAppender.append(null, date, heading, "- Gym · PureGym")
        assertTrue(r.createdFile)
        assertFalse(r.duplicate)
        val md = r.markdown
        assertTrue(md.startsWith("---\ntitle: 2026-11-02\n"))
        listOf("## Private", "## UBS", "## Notes", "## Links", "## Photos", "## Workouts").forEach {
            assertTrue("missing $it", md.lines().contains(it))
        }
        assertTrue(md.endsWith("## Workouts\n\n- Gym · PureGym\n"))
    }

    @Test
    fun addsHeadingAtEndWhenMissing() {
        val existing = "# 2026-11-02\n\n## Notes\n\n- a note\n"
        val r = DailyNoteAppender.append(existing, date, "Workouts", "- Gym")
        assertFalse(r.createdFile)
        assertEquals("# 2026-11-02\n\n## Notes\n\n- a note\n\n## Workouts\n\n- Gym\n", r.markdown)
    }

    @Test
    fun insertsBeforeNextLevelTwoHeading() {
        val existing = "## Workouts\n\n- Swim\n\n## Links\n\n- x\n"
        val r = DailyNoteAppender.append(existing, date, heading, "- Gym")
        assertEquals("## Workouts\n\n- Swim\n- Gym\n\n## Links\n\n- x\n", r.markdown)
    }

    @Test
    fun subheadingsStayInsideSection() {
        val existing = "## Private\n\n### Journal\n\n- j\n\n## UBS\n"
        val r = DailyNoteAppender.append(existing, date, "## Private", "- p")
        assertEquals("## Private\n\n### Journal\n\n- j\n- p\n\n## UBS\n", r.markdown)
    }

    @Test
    fun emptySectionGetsBlankLineThenBullet() {
        val existing = "## Workouts\n## Links\n"
        val r = DailyNoteAppender.append(existing, date, heading, "- Gym")
        assertEquals("## Workouts\n\n- Gym\n\n## Links\n", r.markdown)
    }

    @Test
    fun duplicateSkippedByDedupeKey() {
        val existing = "## Workouts\n\n- Gym · 18:00–19:00 <!-- gymmonth:id=manual-1 -->\n"
        val r = DailyNoteAppender.append(
            existing, date, heading,
            "- Gym · PureGym · 18:00–19:00 <!-- gymmonth:id=manual-1 -->",
            dedupeKey = "gymmonth:id=manual-1",
        )
        assertTrue(r.duplicate)
        assertEquals(existing, r.markdown)
    }

    @Test
    fun differentDedupeKeyAppends() {
        val existing = "## Workouts\n\n- Gym <!-- gymmonth:id=manual-1 -->\n"
        val r = DailyNoteAppender.append(
            existing, date, heading, "- Gym <!-- gymmonth:id=manual-2 -->",
            dedupeKey = "gymmonth:id=manual-2",
        )
        assertFalse(r.duplicate)
        assertTrue(r.markdown.contains("manual-1 -->\n- Gym <!-- gymmonth:id=manual-2 -->\n"))
    }

    @Test
    fun identicalBulletIsDuplicateWithoutKey() {
        val existing = "## Workouts\n\n- Gym\n"
        assertTrue(DailyNoteAppender.append(existing, date, heading, "Gym").duplicate)
    }

    @Test
    fun crlfNormalized() {
        val r = DailyNoteAppender.append("## Workouts\r\n\r\n- a\r\n", date, heading, "- b")
        assertEquals("## Workouts\n\n- a\n- b\n", r.markdown)
    }

    @Test
    fun headingInsideCodeFenceIgnoredAsBoundary() {
        val existing = "## Workouts\n\n```\n## not a heading\n```\n\n## Links\n"
        val r = DailyNoteAppender.append(existing, date, heading, "- Gym")
        assertEquals("## Workouts\n\n```\n## not a heading\n```\n- Gym\n\n## Links\n", r.markdown)
    }

    @Test
    fun headingLevelParsing() {
        assertEquals(2, DailyNoteAppender.headingLevel("## Workouts"))
        assertEquals(0, DailyNoteAppender.headingLevel("#journal"))
        assertEquals(3, DailyNoteAppender.headingLevel("### Ideas"))
    }
}
