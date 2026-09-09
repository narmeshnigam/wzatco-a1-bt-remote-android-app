package com.narmeshnigam.a1remote.data

import com.narmeshnigam.a1remote.hid.RemoteFunction
import com.narmeshnigam.a1remote.hid.ReportKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The findings file against the schema in KEY_LAB.md, and back again. */
class FindingsJsonTest {

    private val sample = Findings(
        appVersion = "1.0.0",
        phone = "OnePlus KB2003 / Android 13",
        host = "WZATCO A1",
        recordedAt = "2026-09-06T21:14:00+05:30",
        results = listOf(
            Finding(
                function = RemoteFunction.POWER,
                report = ReportKind.CONSUMER,
                usage = 0x0030,
                usageName = "Power",
                verdict = Verdict.MAPPED,
                note = "the lamp went out and the fan ran on",
            ),
            Finding(
                function = RemoteFunction.POWER,
                report = ReportKind.KEYBOARD,
                usage = 0x66,
                usageName = "Power",
                verdict = Verdict.NO_EFFECT,
                note = null,
            ),
        ),
    )

    @Test
    fun `a findings file survives a round trip unchanged`() {
        assertEquals(sample, FindingsJson.decode(FindingsJson.encode(sample)))
    }

    @Test
    fun `encoding twice through a round trip is byte for byte stable`() {
        val once = FindingsJson.encode(sample)
        assertEquals(once, FindingsJson.encode(FindingsJson.decode(once)))
    }

    @Test
    fun `the field names and verdict vocabulary are the schema's`() {
        val json = FindingsJson.encode(sample)
        listOf(
            "\"app_version\": \"1.0.0\"",
            "\"phone\": \"OnePlus KB2003 / Android 13\"",
            "\"host\": \"WZATCO A1\"",
            "\"recorded_at\": \"2026-09-06T21:14:00+05:30\"",
            "\"results\"",
            "\"function\": \"POWER\"",
            "\"report\": \"consumer\"",
            "\"usage\": \"0x0030\"",
            "\"usage_name\": \"Power\"",
            "\"verdict\": \"mapped\"",
            "\"note\": null",
            "\"usage\": \"0x66\"",
            "\"verdict\": \"no_effect\"",
        ).forEach { fragment -> assertTrue(fragment, json.contains(fragment)) }
    }

    @Test
    fun `the schema's own example decodes`() {
        val decoded = FindingsJson.decode(SCHEMA_EXAMPLE)
        assertEquals("1.0.0", decoded.appVersion)
        assertEquals("WZATCO A1", decoded.host)
        assertEquals(2, decoded.results.size)
        assertEquals(Verdict.MAPPED, decoded.results[0].verdict)
        assertEquals(0x0030, decoded.results[0].usage)
        assertEquals(ReportKind.KEYBOARD, decoded.results[1].report)
        assertNull(decoded.results[1].note)
    }

    @Test
    fun `all four verdicts round trip`() {
        val results = Verdict.entries.map { verdict ->
            Finding(RemoteFunction.POWER, ReportKind.KEYBOARD, 0x66, "Power", verdict)
        }
        assertEquals(results, FindingsJson.decodeResults(FindingsJson.encodeResults(results)))
    }

    @Test
    fun `an operator's note survives quotes, newlines and backslashes`() {
        val note = "said \"sleep\"\n\tthen a back\\slash — and ünïcode"
        val results =
            listOf(Finding(RemoteFunction.POWER, ReportKind.CONSUMER, 0x32, "Sleep", Verdict.SIDE_EFFECT, note))
        assertEquals(note, FindingsJson.decodeResults(FindingsJson.encodeResults(results)).single().note)
    }

    @Test
    fun `just the results array round trips, which is what persistence stores`() {
        assertEquals(sample.results, FindingsJson.decodeResults(FindingsJson.encodeResults(sample.results)))
        assertEquals(emptyList<Finding>(), FindingsJson.decodeResults(FindingsJson.encodeResults(emptyList())))
    }

    @Test(expected = JsonException::class)
    fun `an unknown verdict is refused rather than guessed`() {
        FindingsJson.decodeResults(
            """[{"function":"POWER","report":"keyboard","usage":"0x66","usage_name":"Power","verdict":"maybe"}]""",
        )
    }

    @Test(expected = JsonException::class)
    fun `a missing field is refused rather than defaulted`() {
        FindingsJson.decodeResults("""[{"function":"POWER","report":"keyboard","usage":"0x66"}]""")
    }

    @Test(expected = JsonException::class)
    fun `text after the object is refused`() {
        FindingsJson.decodeResults("[] and then some")
    }

    private companion object {
        /** The example printed in KEY_LAB.md, verbatim. */
        val SCHEMA_EXAMPLE = """
            {
              "app_version": "1.0.0",
              "phone": "<manufacturer> <model> / Android <release>",
              "host": "WZATCO A1",
              "recorded_at": "2026-09-06T21:14:00+05:30",
              "results": [
                {
                  "function": "POWER",
                  "report": "consumer",
                  "usage": "0x0030",
                  "usage_name": "Power",
                  "verdict": "mapped",
                  "note": "the lamp went out and the fan ran on"
                },
                {
                  "function": "POWER",
                  "report": "keyboard",
                  "usage": "0x66",
                  "usage_name": "Power",
                  "verdict": "no_effect",
                  "note": null
                }
              ]
            }
        """.trimIndent()
    }
}
