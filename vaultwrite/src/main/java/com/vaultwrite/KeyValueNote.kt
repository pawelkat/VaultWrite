package com.vaultwrite

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Settings-style note: YAML frontmatter + a two-column `| Setting | Value |` table.
 * Used for `Notes/<App> settings.md`. Never put secrets in [rows].
 */
object KeyValueNote {

    fun render(
        title: String,
        source: String,
        tags: List<String>,
        rows: List<Pair<String, String>>,
        updatedAt: OffsetDateTime,
        intro: String? = null,
    ): String = buildString {
        val updated = updatedAt.withNano(0).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        append("---\n")
        append("title: ").append(title).append('\n')
        append("source: ").append(source).append('\n')
        append("updated: ").append(updated).append('\n')
        if (tags.isNotEmpty()) {
            append("tags:\n")
            tags.forEach { append("  - ").append(it).append('\n') }
        }
        append("---\n\n")
        append("# ").append(title).append("\n\n")
        intro?.trim()?.takeIf { it.isNotEmpty() }?.let { append(it).append("\n\n") }
        append("| Setting | Value |\n")
        append("| --- | --- |\n")
        rows.forEach { (k, v) -> append("| ").append(escape(k)).append(" | ").append(escape(v)).append(" |\n") }
    }

    /** Parse the first settings table back into key → value (header/separator rows skipped). */
    fun parse(markdown: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        var inTable = false
        for (raw in markdown.replace("\r\n", "\n").lineSequence()) {
            val line = raw.trim()
            if (!line.startsWith("|")) {
                if (inTable) break
                continue
            }
            val cells = splitRow(line)
            if (cells.size < 2) continue
            if (!inTable) {
                inTable = true
                if (cells[0].equals("Setting", ignoreCase = true)) continue
            }
            if (cells.all { it.matches(Regex(":?-{3,}:?")) }) continue
            out[cells[0]] = cells[1]
        }
        return out
    }

    private fun escape(s: String): String = s.replace("\n", " ").replace("|", "\\|").trim()

    private fun splitRow(line: String): List<String> {
        val body = line.removePrefix("|").removeSuffix("|")
        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < body.length) {
            val c = body[i]
            if (c == '\\' && i + 1 < body.length && body[i + 1] == '|') {
                sb.append('|'); i += 2; continue
            }
            if (c == '|') {
                cells.add(sb.toString().trim()); sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        cells.add(sb.toString().trim())
        return cells
    }
}
