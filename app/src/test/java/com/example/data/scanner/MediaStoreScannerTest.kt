package com.example.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MediaStoreScannerTest {

  @Test
  fun `buildVolumeAwareFolderKey handles primary storage root and subfolders`() {
    assertEquals("", MediaStoreScanner.buildVolumeAwareFolderKey("primary:"))
    assertEquals("anime/frieren", MediaStoreScanner.buildVolumeAwareFolderKey("primary:Anime/Frieren"))
    assertEquals("anime/frieren", MediaStoreScanner.buildVolumeAwareFolderKey("PRIMARY:/Anime/Frieren/"))
  }

  @Test
  fun `buildVolumeAwareFolderKey handles sd card root and subfolders`() {
    assertEquals("sd_1a2b-3c4d", MediaStoreScanner.buildVolumeAwareFolderKey("1A2B-3C4D:"))
    assertEquals(
      "sd_1a2b-3c4d/anime/frieren",
      MediaStoreScanner.buildVolumeAwareFolderKey("1A2B-3C4D:Anime/Frieren")
    )
  }

  @Test
  fun `buildVolumeAwareFolderKey avoids collision between internal and sd card folders with same name`() {
    val internalKey = MediaStoreScanner.buildVolumeAwareFolderKey("primary:Anime")
    val sdCardKey = MediaStoreScanner.buildVolumeAwareFolderKey("1A2B-3C4D:Anime")
    assertNotEquals(internalKey, sdCardKey)
    assertEquals("anime", internalKey)
    assertEquals("sd_1a2b-3c4d/anime", sdCardKey)
  }
}
