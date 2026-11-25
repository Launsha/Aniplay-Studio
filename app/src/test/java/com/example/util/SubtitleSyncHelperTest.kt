package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleSyncHelperTest {

  @Test
  fun shiftSubtitleContent_shiftsPositiveOffsetOnSrt() {
    val srt = """
      1
      00:00:10,500 --> 00:00:14,250
      Halo dunia
    """.trimIndent()

    val shifted = SubtitleSyncHelper.shiftSubtitleContent(srt, isAss = false, offsetMs = 1500L)

    val expected = """
      1
      00:00:12,000 --> 00:00:15,750
      Halo dunia
    """.trimIndent()
    assertEquals(expected, shifted)
  }

  @Test
  fun shiftSubtitleContent_clampsNegativeOffsetAtZeroOnSrt() {
    val srt = """
      1
      00:00:01,000 --> 00:00:04,500
      Pembuka
    """.trimIndent()

    val shifted = SubtitleSyncHelper.shiftSubtitleContent(srt, isAss = false, offsetMs = -2000L)

    val expected = """
      1
      00:00:00,000 --> 00:00:02,500
      Pembuka
    """.trimIndent()
    assertEquals(expected, shifted)
  }

  @Test
  fun shiftSubtitleContent_shiftsOffsetOnAss() {
    val ass = "Dialogue: 0,0:01:00.50,0:01:05.00,Default,,0,0,0,,Kalimat uji"

    val shifted = SubtitleSyncHelper.shiftSubtitleContent(ass, isAss = true, offsetMs = -500L)

    assertEquals("Dialogue: 0,0:01:00.00,0:01:04.50,Default,,0,0,0,,Kalimat uji", shifted)
  }

  @Test
  fun shiftSubtitleContent_zeroOffsetReturnsOriginal() {
    val raw = "1\n00:00:02,000 --> 00:00:03,000\nTes"
    assertEquals(raw, SubtitleSyncHelper.shiftSubtitleContent(raw, isAss = false, offsetMs = 0L))
  }
}
