package com.vaultwrite

import java.time.LocalDate

/**
 * Pure markdown rewrite for Obsidian Daily notes: append one bullet under an arbitrary
 * heading (e.g. `## Workouts`), creating the heading and/or a Daily stub when missing.
 *
 * Android-free so it is unit-testable on the JVM. Ported from ToDaily's DailyNoteAppender
 * and generalised (any heading, explicit dedupe key, heading-level-aware section end).
 */
object DailyNoteAppender {

    data class Rewrite(
        val markdown: String,
        val createdFile: Boolean,
        val duplicate: Boolean,
        val bullet: String,
    )

    /** `Workouts` → `## Workouts`; `### Gym` stays as is. */
    fun normalizeHeading(heading: String): String {
        val trimmed = heading.trim()
        require(trimmed.isNotEmpty()) { "heading must not be blank" }
        return if (trimmed.startsWith("#")) trimmed else "## $trimmed"
    }

    /** Ensure a single-line list item: `foo` → `- foo`. */
    fun normalizeBullet(bullet: String): String {
        val line = bullet.trim().replace("\r\n", " ").replace('\n', ' ')
        require(line.isNotEmpty()) { "bullet must not be blank" }
        return if (line.startsWith("- ") || line.startsWith("* ")) line else "- $line"
    }

    /**
     * New Daily note mirroring the vault's `Templates/Daily.md` shape
     * (Private / UBS / Notes / Links / Photos). [extraHeadings] that are not already part of
     * the template are appended at the end.
     */
    fun stub(date: LocalDate, extraHeadings: List<String> = emptyList()): String {
        val iso = date.toString()
        val base = """
            |---
            |title: $iso
            |created: $iso
            |tags:
            |  - journal
            |---
            |
            |# $iso
            |
            |#journal
            |
            |## Private
            |
            |### Journal
            |
            |### Ideas
            |
            |### Open loops
            |
            |## UBS
            |
            |### Journal
            |
            |### Ideas
            |
            |### Open loops
            |
            |## Notes
            |
            |## Links
            |
            |## Photos
            |
        """.trimMargin()
        var out = base
        for (h in extraHeadings.map(::normalizeHeading)) {
            if (!hasHeading(out, h)) out = appendHeading(out, h)
        }
        return ensureTrailingNewline(out)
    }

    /**
     * Append [bullet] under [heading] in [existing] (null/blank → new stub for [date]).
     *
     * Duplicate detection: when [dedupeKey] is non-blank, any line containing it counts as
     * already logged (e.g. `gymmonth:id=…`). Otherwise an identical bullet line is a duplicate.
     */
    fun append(
        existing: String?,
        date: LocalDate,
        heading: String,
        bullet: String,
        dedupeKey: String? = null,
    ): Rewrite {
        val h = normalizeHeading(heading)
        val line = normalizeBullet(bullet)
        val created = existing.isNullOrBlank()
        val source = if (created) stub(date, listOf(h)) else existing!!.replace("\r\n", "\n")
        if (isDuplicate(source, line, dedupeKey)) {
            return Rewrite(ensureTrailingNewline(source), created, duplicate = true, bullet = line)
        }
        val withHeading = if (hasHeading(source, h)) source else appendHeading(source, h)
        val rewritten = insertBullet(withHeading, line, h)
        return Rewrite(ensureTrailingNewline(rewritten), created, duplicate = false, bullet = line)
    }

    fun isDuplicate(markdown: String, bullet: String, dedupeKey: String?): Boolean {
        val key = dedupeKey?.trim().orEmpty()
        return if (key.isNotEmpty()) {
            markdown.lineSequence().any { it.contains(key) }
        } else {
            val needle = normalizeBullet(bullet)
            markdown.lineSequence().any { it.trim() == needle }
        }
    }

    fun hasHeading(markdown: String, heading: String): Boolean {
        val h = normalizeHeading(heading)
        return markdown.lineSequence().any { it.trim().equals(h, ignoreCase = true) }
    }

    /** Markdown ATX heading level of [line] (0 if not a heading). */
    internal fun headingLevel(line: String): Int {
        val t = line.trimStart()
        var n = 0
        while (n < t.length && t[n] == '#') n++
        if (n == 0 || n > 6) return 0
        return if (n == t.length || t[n] == ' ') n else 0
    }

    private fun appendHeading(markdown: String, heading: String): String =
        "${markdown.trimEnd()}\n\n$heading\n"

    /** Insert [bullet] at the end of [heading]'s section (before the next heading of same/higher level). */
    private fun insertBullet(markdown: String, bullet: String, heading: String): String {
        val lines = markdown.split("\n")
        val headingIndex = lines.indexOfFirst { it.trim().equals(heading, ignoreCase = true) }
        if (headingIndex < 0) return "${markdown.trimEnd()}\n\n$heading\n\n$bullet\n"
        val level = headingLevel(heading).coerceAtLeast(1)
        var next = lines.size
        var inFence = false
        for (i in headingIndex + 1 until lines.size) {
            val t = lines[i].trim()
            if (t.startsWith("```") || t.startsWith("~~~")) inFence = !inFence
            if (inFence) continue
            val l = headingLevel(t)
            if (l in 1..level) {
                next = i
                break
            }
        }
        val section = lines.subList(headingIndex + 1, next).toMutableList()
        while (section.isNotEmpty() && section.last().isBlank()) section.removeAt(section.lastIndex)
        if (section.isEmpty()) section.add("")
        section.add(bullet)
        val rebuilt = ArrayList<String>(lines.size + 3)
        rebuilt.addAll(lines.subList(0, headingIndex + 1))
        rebuilt.addAll(section)
        if (next < lines.size) {
            rebuilt.add("")
            rebuilt.addAll(lines.subList(next, lines.size))
        }
        return rebuilt.joinToString("\n")
    }

    private fun ensureTrailingNewline(markdown: String): String =
        if (markdown.endsWith("\n")) markdown else "$markdown\n"
}
