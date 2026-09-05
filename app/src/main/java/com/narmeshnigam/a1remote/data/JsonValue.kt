package com.narmeshnigam.a1remote.data

/**
 * A JSON reader and writer small enough to read in one sitting.
 *
 * The findings file is the only JSON this app touches, and it has to be written by a pure
 * Kotlin unit: `org.json` is an Android class and would put the codec out of reach of the
 * round-trip test TEST_PLAN.md asks for. A dependency for one flat object with six field names
 * is not worth its weight, so this is the whole of it.
 *
 * It is strict rather than forgiving. A findings file that half-parses would show the operator
 * a verdict nobody recorded, which is worse than an error.
 */
internal sealed interface JsonValue {
    data class Text(val value: String) : JsonValue
    data class Number(val value: Long) : JsonValue
    data class Bool(val value: Boolean) : JsonValue
    data object Null : JsonValue
    data class Array(val items: List<JsonValue>) : JsonValue
    data class Object(val fields: Map<String, JsonValue>) : JsonValue {
        fun text(name: String): String? = (fields[name] as? Text)?.value
        fun array(name: String): List<JsonValue>? = (fields[name] as? Array)?.items
    }
}

/** Raised when the text is not JSON, or not the JSON this app wrote. */
internal class JsonException(message: String) : Exception(message)

/** Writes a [JsonValue] out, indented two spaces so a findings file is readable as it stands. */
internal object JsonWriter {

    private const val INDENT = "  "

    fun write(value: JsonValue): String = render(value, depth = 0)

    private fun render(value: JsonValue, depth: Int): String = when (value) {
        is JsonValue.Text -> quote(value.value)
        is JsonValue.Number -> value.value.toString()
        is JsonValue.Bool -> value.value.toString()
        JsonValue.Null -> "null"
        is JsonValue.Array -> renderArray(value.items, depth)
        is JsonValue.Object -> renderObject(value.fields, depth)
    }

    private fun renderArray(items: List<JsonValue>, depth: Int): String {
        if (items.isEmpty()) return "[]"
        val pad = INDENT.repeat(depth + 1)
        val body = items.joinToString(",\n") { item -> pad + render(item, depth + 1) }
        return "[\n$body\n${INDENT.repeat(depth)}]"
    }

    private fun renderObject(fields: Map<String, JsonValue>, depth: Int): String {
        if (fields.isEmpty()) return "{}"
        val pad = INDENT.repeat(depth + 1)
        val body = fields.entries.joinToString(",\n") { (name, field) ->
            "$pad${quote(name)}: ${render(field, depth + 1)}"
        }
        return "{\n$body\n${INDENT.repeat(depth)}}"
    }

    private fun quote(text: String): String {
        val out = StringBuilder(text.length + 2)
        out.append('"')
        text.forEach { char -> out.append(escape(char)) }
        out.append('"')
        return out.toString()
    }

    private fun escape(char: Char): String = when {
        char == '"' -> "\\\""
        char == '\\' -> "\\\\"
        char == '\n' -> "\\n"
        char == '\r' -> "\\r"
        char == '\t' -> "\\t"
        char < ' ' -> "\\u%04x".format(char.code)
        else -> char.toString()
    }
}

/**
 * A recursive-descent reader over the same grammar.
 *
 * Only structural reads skip whitespace, through [current]. Inside a string the characters are
 * taken raw: a space in an operator's note is part of the note, not a token separator.
 */
internal class JsonReader(private val text: String) {

    private var at = 0

    /** The next character, whitespace already skipped. */
    private val current: Char
        get() {
            skipSpace()
            if (at >= text.length) fail("unexpected end of input")
            return text[at]
        }

    /** Parses the whole of [text] as one value, and refuses anything trailing it. */
    fun parse(): JsonValue {
        val value = readValue()
        skipSpace()
        if (at != text.length) fail("trailing text at offset $at")
        return value
    }

    private fun readValue(): JsonValue = when (current) {
        '{' -> readObject()
        '[' -> readArray()
        '"' -> JsonValue.Text(readString())
        't' -> JsonValue.Bool(readLiteral("true", true))
        'f' -> JsonValue.Bool(readLiteral("false", false))
        'n' -> readLiteral("null", JsonValue.Null)
        else -> JsonValue.Number(readNumber())
    }

    private fun readObject(): JsonValue.Object {
        at++ // the '{' current already matched
        val fields = LinkedHashMap<String, JsonValue>()
        if (current == '}') {
            at++
            return JsonValue.Object(fields)
        }
        while (true) {
            if (current != '"') fail("expected a field name at offset $at")
            val name = readString()
            if (current != ':') fail("expected ':' after \"$name\"")
            at++
            fields[name] = readValue()
            val separator = current
            at++
            if (separator == '}') return JsonValue.Object(fields)
            if (separator != ',') fail("expected ',' or '}' but found '$separator'")
        }
    }

    private fun readArray(): JsonValue.Array {
        at++ // the '[' current already matched
        val items = mutableListOf<JsonValue>()
        if (current == ']') {
            at++
            return JsonValue.Array(items)
        }
        while (true) {
            items += readValue()
            val separator = current
            at++
            if (separator == ']') return JsonValue.Array(items)
            if (separator != ',') fail("expected ',' or ']' but found '$separator'")
        }
    }

    private fun readString(): String {
        at++ // the opening quote current already matched
        val out = StringBuilder()
        while (true) {
            if (at >= text.length) fail("unterminated string")
            when (val char = text[at++]) {
                '"' -> return out.toString()
                '\\' -> out.append(readEscape())
                else -> out.append(char)
            }
        }
    }

    private fun readEscape(): Char {
        if (at >= text.length) fail("unterminated escape")
        return when (val marker = text[at++]) {
            '"', '\\', '/' -> marker
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                if (at + UNICODE_DIGITS > text.length) fail("truncated \\u escape")
                val digits = text.substring(at, at + UNICODE_DIGITS)
                at += UNICODE_DIGITS
                (digits.toIntOrNull(radix = HEX_RADIX) ?: fail("bad \\u escape $digits")).toChar()
            }

            else -> fail("unknown escape \\$marker")
        }
    }

    private fun readNumber(): Long {
        skipSpace()
        val start = at
        if (at < text.length && text[at] == '-') at++
        while (at < text.length && text[at].isDigit()) at++
        val digits = text.substring(start, at)
        return digits.toLongOrNull() ?: fail("expected a whole number at offset $start")
    }

    private fun <T> readLiteral(word: String, value: T): T {
        skipSpace()
        if (!text.startsWith(word, at)) fail("expected $word at offset $at")
        at += word.length
        return value
    }

    private fun skipSpace() {
        while (at < text.length && text[at].isWhitespace()) at++
    }

    private fun fail(message: String): Nothing = throw JsonException(message)

    private companion object {
        const val UNICODE_DIGITS = 4
        const val HEX_RADIX = 16
    }
}
