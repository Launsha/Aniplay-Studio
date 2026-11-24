package com.example.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimeTitleParserTest {

  @Test
  fun `parses fansub bracketed format into title and episode`() {
    val result = AnimeTitleParser.parse(
      fileName = "[SubsPlease] Sousou no Frieren - 05 [1080p].mkv",
      parentFolderName = "Download"
    )
    assertEquals("Sousou no Frieren", result.title)
    assertEquals(5, result.episodeNumber)
  }

  @Test
  fun `parses release year and genre tags when present`() {
    val result = AnimeTitleParser.parse(
      fileName = "[Group] Jujutsu Kaisen (2023) - Ep 12 [Action][720p].mp4",
      parentFolderName = "Download"
    )
    assertEquals("Jujutsu Kaisen", result.title)
    assertEquals(12, result.episodeNumber)
    assertEquals(2023, result.releaseYear)
    assertEquals("Action", result.genre)
  }

  @Test
  fun `parses season and episode dot format`() {
    val result = AnimeTitleParser.parse(
      fileName = "Attack.on.Titan.S04E28.1080p.WEB-DL.mkv",
      parentFolderName = "Download"
    )
    assertEquals("Attack on Titan S4", result.title)
    assertEquals(28, result.episodeNumber)
  }

  @Test
  fun `falls back to parent folder name when filename lacks title`() {
    val result = AnimeTitleParser.parse(
      fileName = "05.mkv",
      parentFolderName = "Chainsaw Man"
    )
    assertEquals("Chainsaw Man", result.title)
    assertEquals(5, result.episodeNumber)
    assertNull(result.releaseYear)
  }

  @Test
  fun `falls back to parent folder name and episode 1 when unparseable`() {
    val result = AnimeTitleParser.parse(
      fileName = "ova_special.webm",
      parentFolderName = "Solo Leveling"
    )
    assertEquals("Solo Leveling", result.title)
    assertEquals(1, result.episodeNumber)
  }
}
