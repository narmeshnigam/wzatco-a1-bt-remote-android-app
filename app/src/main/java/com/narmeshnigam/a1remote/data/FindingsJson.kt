package com.narmeshnigam.a1remote.data

import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.hid.ReportKind

/**
 * The findings file of KEY_LAB.md, encoded and decoded by hand.
 *
 * Field names, field order and the verdict vocabulary are the schema's, not this app's. The
 * decoder is the encoder read backwards and nothing more: a row it does not fully understand is
 * an error, because a findings file is a record of what the hardware did and a half-read row
 * would be a record of what it might have done.
 */
object FindingsJson {

    private const val APP_VERSION = "app_version"
    private const val PHONE = "phone"
    private const val HOST = "host"
    private const val RECORDED_AT = "recorded_at"
    private const val RESULTS = "results"

    private const val FUNCTION = "function"
    private const val REPORT = "report"
    private const val USAGE = "usage"
    private const val USAGE_NAME = "usage_name"
    private const val VERDICT = "verdict"
    private const val NOTE = "note"

    private const val HEX_PREFIX = "0x"
    private const val HEX_RADIX = 16

    /** The whole file, pretty-printed. */
    fun encode(findings: Findings): String = JsonWriter.write(
        JsonValue.Object(
            linkedMapOf(
                APP_VERSION to JsonValue.Text(findings.appVersion),
                PHONE to JsonValue.Text(findings.phone),
                HOST to JsonValue.Text(findings.host),
                RECORDED_AT to JsonValue.Text(findings.recordedAt),
                RESULTS to JsonValue.Array(findings.results.map(::encodeResult)),
            ),
        ),
    )

    /** Reads back what [encode] wrote. Throws [JsonException] on anything else. */
    fun decode(text: String): Findings {
        val root = JsonReader(text).parse() as? JsonValue.Object ?: fail("the file is not a JSON object")
        val results = root.array(RESULTS) ?: fail("no $RESULTS array")
        return Findings(
            appVersion = root.text(APP_VERSION) ?: fail("no $APP_VERSION"),
            phone = root.text(PHONE) ?: fail("no $PHONE"),
            host = root.text(HOST) ?: fail("no $HOST"),
            recordedAt = root.text(RECORDED_AT) ?: fail("no $RECORDED_AT"),
            results = results.map(::decodeResult),
        )
    }

    /**
     * Just the `results` array, which is the part that has to survive a restart.
     *
     * The four header fields are facts about the moment of export — the app version, the phone,
     * the clock — so they are gathered at export rather than frozen into storage.
     */
    fun encodeResults(results: List<Finding>): String = JsonWriter.write(JsonValue.Array(results.map(::encodeResult)))

    /** Reads back what [encodeResults] wrote. */
    fun decodeResults(text: String): List<Finding> {
        val array = JsonReader(text).parse() as? JsonValue.Array ?: fail("expected an array of results")
        return array.items.map(::decodeResult)
    }

    private fun encodeResult(finding: Finding): JsonValue = JsonValue.Object(
        linkedMapOf(
            FUNCTION to JsonValue.Text(finding.function.name),
            REPORT to JsonValue.Text(finding.report.wireName),
            USAGE to JsonValue.Text(finding.usageHex),
            USAGE_NAME to JsonValue.Text(finding.usageName),
            VERDICT to JsonValue.Text(finding.verdict.wireName),
            NOTE to finding.note?.let(JsonValue::Text).orNull(),
        ),
    )

    private fun decodeResult(value: JsonValue): Finding {
        val row = value as? JsonValue.Object ?: fail("a result is not an object")
        val functionName = row.text(FUNCTION) ?: fail("a result has no $FUNCTION")
        val reportName = row.text(REPORT) ?: fail("a result has no $REPORT")
        val verdictName = row.text(VERDICT) ?: fail("a result has no $VERDICT")
        val usage = row.text(USAGE) ?: fail("a result has no $USAGE")
        return Finding(
            function = RemoteFunction.entries.firstOrNull { it.name == functionName }
                ?: fail("unknown function $functionName"),
            report = ReportKind.byWireName(reportName) ?: fail("unknown report kind $reportName"),
            usage = usage.removePrefix(HEX_PREFIX).toIntOrNull(radix = HEX_RADIX) ?: fail("bad usage $usage"),
            usageName = row.text(USAGE_NAME) ?: fail("a result has no $USAGE_NAME"),
            verdict = Verdict.byWireName(verdictName) ?: fail("unknown verdict $verdictName"),
            note = row.text(NOTE),
        )
    }

    private fun JsonValue?.orNull(): JsonValue = this ?: JsonValue.Null

    private fun fail(message: String): Nothing = throw JsonException(message)
}
