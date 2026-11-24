package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormattersTest {

  @Test
  fun `formatDuration formats mm_ss and h_mm_ss`() {
    assertEquals("00:00", TimeFormatters.formatDuration(0L))
    assertEquals("24:05", TimeFormatters.formatDuration((24 * 60 + 5) * 1000L))
    assertEquals("1:05:30", TimeFormatters.formatDuration((3600 + 5 * 60 + 30) * 1000L))
  }
}
