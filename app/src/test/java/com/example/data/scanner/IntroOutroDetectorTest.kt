package com.example.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.math.sin

class IntroOutroDetectorTest {

  @Test
  fun parseSubtitleForIntroOutro_detectsAssOpAndEdStyles() {
    val assContent = """
      [Script Info]
      Title: Test Anime

      [Events]
      Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
      Dialogue: 0,0:01:30.00,0:02:15.00,OP_Romaji,,0,0,0,,Line 1
      Dialogue: 0,0:02:15.00,0:02:55.00,OP_Romaji,,0,0,0,,Line 2
      Dialogue: 0,0:10:00.00,0:10:05.00,Default,,0,0,0,,Normal dialogue
      Dialogue: 0,0:21:00.00,0:21:45.00,ED,,0,0,0,,Outro line 1
      Dialogue: 0,0:21:45.00,0:22:25.00,ED,,0,0,0,,Outro line 2
    """.trimIndent()

    val result = IntroOutroDetector.parseSubtitleForIntroOutro(
      content = assContent,
      totalDurationMs = 24 * 60_000L
    )

    assertTrue(result.hasIntro)
    assertTrue(result.hasOutro)
    assertEquals("subtitle", result.source)
    assertEquals(90_000L, result.introStartMs)
    assertEquals(175_000L, result.introEndMs)
    assertEquals(1_260_000L, result.outroStartMs)
    assertEquals(1_345_000L, result.outroEndMs)
  }

  @Test
  fun parseSubtitleForIntroOutro_detectsSrtMarkersAndExpandsSingleCue() {
    val srtContent = """
      1
      00:01:20,000 --> 00:01:25,000
      [Opening] Lagu Pembuka

      2
      00:12:00,000 --> 00:12:04,000
      Percakapan biasa

      3
      00:21:30,000 --> 00:21:35,000
      [Ending] Lagu Penutup
    """.trimIndent()

    val result = IntroOutroDetector.parseSubtitleForIntroOutro(
      content = srtContent,
      totalDurationMs = 24 * 60_000L
    )

    assertTrue(result.hasIntro)
    assertTrue(result.hasOutro)
    assertEquals(80_000L, result.introStartMs)
    assertEquals(165_000L, result.introEndMs)
    assertEquals(1_290_000L, result.outroStartMs)
    assertEquals(1_375_000L, result.outroEndMs)
  }

  @Test
  fun classifyChapterEntries_identifiesOpAndEd() {
    val chapters = listOf(
      ChapterEntry("Prologue", 0L, 90_000L),
      ChapterEntry("OP", 90_000L, 180_000L),
      ChapterEntry("Part A", 180_000L, 660_000L),
      ChapterEntry("Part B", 660_000L, 1_260_000L),
      ChapterEntry("Ending Theme", 1_260_000L, 1_350_000L),
      ChapterEntry("Preview", 1_350_000L, 1_380_000L)
    )

    val result = IntroOutroDetector.classifyChapterEntries(chapters, totalDurationMs = 1_380_000L)

    assertTrue(result.hasIntro)
    assertTrue(result.hasOutro)
    assertEquals("chapter", result.source)
    assertEquals(90_000L, result.introStartMs)
    assertEquals(180_000L, result.introEndMs)
    assertEquals(1_260_000L, result.outroStartMs)
    assertEquals(1_350_000L, result.outroEndMs)
  }

  @Test
  fun parseMkvChapters_decodesSyntheticEbmlChapterAtoms() {
    val bytes = ByteArrayOutputStream().apply {
      write(buildChapterAtom(title = "Prologue", startMs = 0L, endMs = 60_000L))
      write(buildChapterAtom(title = "Opening", startMs = 60_000L, endMs = 150_000L))
      write(buildChapterAtom(title = "Ending", startMs = 1_260_000L, endMs = 1_350_000L))
    }.toByteArray()

    val chapters = IntroOutroDetector.parseMkvChapters(bytes)

    assertEquals(3, chapters.size)
    assertEquals("Opening", chapters[1].title)
    assertEquals(60_000L, chapters[1].startMs)
    assertEquals(150_000L, chapters[1].endMs)

    val classified = IntroOutroDetector.classifyChapterEntries(chapters, totalDurationMs = 1_400_000L)
    assertTrue(classified.hasIntro)
    assertTrue(classified.hasOutro)
    assertEquals(60_000L, classified.introStartMs)
    assertEquals(1_260_000L, classified.outroStartMs)
  }

  @Test
  fun matchAudioProfiles_findsCorrelatedOpAndEdWindowsAcrossEpisodes() {
    val ep1Intro = DoubleArray(180) { i -> 0.1 + 0.02 * sin(i * 0.7) }
    val ep2Intro = DoubleArray(180) { i -> 0.1 + 0.02 * sin(i * 1.3 + 1.0) }
    // Identical OP signature of 85s starting at 40s in ep1 and 44s in ep2
    for (k in 0 until 85) {
      val opEnergy = 0.45 + 0.35 * sin(k * 0.25) + 0.15 * sin(k * 0.8)
      ep1Intro[40 + k] = opEnergy
      ep2Intro[44 + k] = opEnergy
    }

    val ep1Outro = DoubleArray(180) { i -> 0.08 + 0.02 * sin(i * 0.9) }
    val ep2Outro = DoubleArray(180) { i -> 0.09 + 0.02 * sin(i * 0.5) }
    // Identical ED signature of 85s starting at offset 60s in outro window
    for (k in 0 until 85) {
      val edEnergy = 0.50 + 0.30 * sin(k * 0.33)
      ep1Outro[60 + k] = edEnergy
      ep2Outro[56 + k] = edEnergy
    }

    val result = IntroOutroDetector.matchAudioProfiles(
      ep1Intro = ep1Intro,
      ep2Intro = ep2Intro,
      ep1Outro = ep1Outro,
      ep2Outro = ep2Outro,
      ep1OutroWindowStartMs = 1_200_000L
    )

    assertTrue(result.hasIntro)
    assertTrue(result.hasOutro)
    assertEquals("audio_scan", result.source)
    assertEquals(40_000L, result.introStartMs)
    assertEquals(125_000L, result.introEndMs)
    assertEquals(1_260_000L, result.outroStartMs)
    assertEquals(1_345_000L, result.outroEndMs)
  }

  @Test
  fun parseSubtitleForIntroOutro_returnsEmptyForOrdinaryDialogue() {
    val srt = """
      1
      00:00:10,000 --> 00:00:14,000
      Halo semuanya!
    """.trimIndent()
    val result = IntroOutroDetector.parseSubtitleForIntroOutro(srt, 1_400_000L)
    assertFalse(result.hasAny)
  }

  private fun buildChapterAtom(title: String, startMs: Long, endMs: Long): ByteArray {
    val titleBytes = title.toByteArray(Charsets.UTF_8)
    val chapStringElem = byteArrayOf(0x85.toByte(), (0x80 or titleBytes.size).toByte()) + titleBytes
    val displayElem = byteArrayOf(0x80.toByte(), (0x80 or chapStringElem.size).toByte()) + chapStringElem
    val startNs = startMs * 1_000_000L
    val endNs = endMs * 1_000_000L
    val startElem = byteArrayOf(0x91.toByte(), 0x88.toByte()) + longToBigEndian8(startNs)
    val endElem = byteArrayOf(0x92.toByte(), 0x88.toByte()) + longToBigEndian8(endNs)
    val body = startElem + endElem + displayElem
    return byteArrayOf(0xB6.toByte(), (0x80 or body.size).toByte()) + body
  }

  private fun longToBigEndian8(value: Long): ByteArray {
    return ByteArray(8) { idx -> ((value ushr ((7 - idx) * 8)) and 0xFFL).toByte() }
  }
}
