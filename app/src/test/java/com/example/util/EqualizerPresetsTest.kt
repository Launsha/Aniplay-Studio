package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerPresetsTest {

  @Test
  fun presets_containsActionSliceOfLifeOstHorrorAndMecha() {
    val keys = EqualizerPresets.presets.map { it.key }.toSet()
    assertTrue("action" in keys)
    assertTrue("slice_of_life" in keys)
    assertTrue("ost" in keys)
    assertTrue("horror_mystery" in keys)
    assertTrue("mecha_scifi" in keys)
  }

  @Test
  fun actionPreset_emphasizesSubBassAndTrebleOverMids() {
    val bass = EqualizerPresets.bandRatio("action", 0, 5)
    val mid = EqualizerPresets.bandRatio("action", 2, 5)
    val treble = EqualizerPresets.bandRatio("action", 4, 5)

    assertTrue(bass > mid)
    assertTrue(treble > mid)
  }

  @Test
  fun sliceOfLifePreset_emphasizesVocalMidrangeOverSubBassAndTreble() {
    val bass = EqualizerPresets.bandRatio("slice_of_life", 0, 5)
    val vocalMid = EqualizerPresets.bandRatio("slice_of_life", 2, 5)
    val treble = EqualizerPresets.bandRatio("slice_of_life", 4, 5)

    assertTrue(vocalMid > bass)
    assertTrue(vocalMid > treble)
  }

  @Test
  fun computeBandLevel_respectsHardwareBounds() {
    val level = EqualizerPresets.computeBandLevel(
      presetKey = "action",
      bandIndex = 0,
      bandCount = 5,
      minLevel = -1500,
      maxLevel = 1500
    )
    assertTrue(level in -1500..1500)
    assertTrue(level > 0)
    assertEquals(0, EqualizerPresets.computeBandLevel("normal", 2, 5, -1500, 1500))
  }
}
