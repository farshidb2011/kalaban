package com.kalaban.farshidb2011.core.csv

/** RFC 4180 CSV reading/writing with delimiter detection; written files start with a BOM so Excel reads UTF-8. */
object Csv {
    const val BOM = "\uFEFF"
    val supportedDelimiters = listOf(',', ';', '\t')

    fun detectDelimiter(text: String): Char {
        val firstLine = text.removePrefix(BOM).lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        var inQuotes = false
        val counts = HashMap<Char, Int>()
        for (ch in firstLine) {
            if (ch == '"') inQuotes = !inQuotes
            if (!inQuotes && ch in supportedDelimiters) counts[ch] = (counts[ch] ?: 0) + 1
        }
        return counts.maxByOrNull { it.value }?.key ?: ','
    }

    fun parse(text: String, delimiter: Char = detectDelimiter(text)): List<List<String>> {
        val s = text.removePrefix(BOM)
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < s.length && s[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        row.add(field.toString())
                        field.setLength(0)
                    }
                    '\r' -> Unit
                    '\n' -> {
                        row.add(field.toString())
                        field.setLength(0)
                        if (row.any { it.isNotEmpty() }) rows.add(row)
                        row = ArrayList()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            if (row.any { it.isNotEmpty() }) rows.add(row)
        }
        return rows
    }

    fun write(rows: List<List<String>>, delimiter: Char = ','): String {
        val out = StringBuilder(BOM)
        for (row in rows) {
            row.forEachIndexed { index, cell ->
                if (index > 0) out.append(delimiter)
                out.append(escape(cell, delimiter))
            }
            out.append("\r\n")
        }
        return out.toString()
    }

    private fun escape(cell: String, delimiter: Char): String {
        val needsQuotes = cell.any { it == delimiter || it == '"' || it == '\n' || it == '\r' } ||
            cell.startsWith(' ') || cell.endsWith(' ')
        return if (needsQuotes) "\"" + cell.replace("\"", "\"\"") + "\"" else cell
    }

    /** Neutralizes spreadsheet formula injection in free-text cells (`=`, `+`, `-`, `@` prefixes). */
    fun text(value: String?): String {
        val v = value.orEmpty()
        return if (v.isNotEmpty() && v[0] in "=+-@\t\r") "'$v" else v
    }
}
