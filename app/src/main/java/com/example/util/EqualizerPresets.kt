package com.example.util

import androidx.annotation.StringRes
import com.example.R
import kotlin.math.roundToInt

data class EqualizerPreset(
  val key: String,
  @StringRes val labelRes: Int,
  @StringRes val descriptionRes: Int
)

object EqualizerPresets {

  val presets: List<EqualizerPreset> = listOf(
    EqualizerPreset("normal", R.string.settings_eq_normal, R.string.settings_eq_desc_normal),
    EqualizerPreset("anime", R.string.settings_eq_anime, R.string.settings_eq_desc_anime),
    EqualizerPreset("action", R.string.settings_eq_action, R.string.settings_eq_desc_action),
    EqualizerPreset("slice_of_life", R.string.settings_eq_slice_of_life, R.string.settings_eq_desc_slice_of_life),
    EqualizerPreset("ost", R.string.settings_eq_ost, R.string.settings_eq_desc_ost),
    EqualizerPreset("horror_mystery", R.string.settings_eq_horror, R.string.settings_eq_desc_horror),
    EqualizerPreset("mecha_scifi", R.string.settings_eq_mecha, R.string.settings_eq_desc_mecha),
    EqualizerPreset("bass", R.string.settings_eq_bass, R.string.settings_eq_desc_bass),
    EqualizerPreset("cinema", R.string.settings_eq_cinema, R.string.settings_eq_desc_cinema),
    EqualizerPreset("night", R.string.settings_eq_night, R.string.settings_eq_desc_night)
  )

  fun findByKey(key: String): EqualizerPreset {
    return presets.firstOrNull { it.key == key } ?: presets.first()
  }

  /**
   * Computes normalized equalizer gain ratio (-1.0..+1.0) for [bandIndex] out of [bandCount].
   * Band position 0.0 represents sub-bass, 0.5 represents vocal midrange, 1.0 represents treble.
   */
  fun bandRatio(presetKey: String, bandIndex: Int, bandCount: Int): Float {
    if (bandCount <= 0) return 0f
    val pos = if (bandCount == 1) 0.5f else bandIndex.toFloat() / (bandCount - 1).toFloat()

    return when (presetKey) {
      "anime" -> when {
        pos <= 0.15f -> 0.10f
        pos < 0.85f -> 0.55f
        else -> 0.45f
      }
      "action" -> when {
        pos <= 0.15f -> 0.82f // Sub-bass explosions & heavy impacts
        pos <= 0.38f -> 0.58f // Mid-bass punch
        pos <= 0.65f -> 0.25f // Clean mids so SFX never muddy dialogue
        else -> 0.65f         // Crisp sword clashes & action treble
      }
      "slice_of_life" -> when {
        pos <= 0.15f -> -0.10f // Trim distracting rumble
        pos <= 0.38f -> 0.25f  // Warm intimate room tone
        pos <= 0.72f -> 0.68f  // Prominent, natural seiyuu vocals & dialogue
        else -> 0.22f          // Soft non-fatiguing treble
      }
      "ost" -> when {
        pos <= 0.15f -> 0.48f // Rich bass foundation
        pos <= 0.38f -> 0.25f // Clean low-mids
        pos <= 0.72f -> 0.42f // Expressive strings & vocals
        else -> 0.72f         // Sparkling J-Pop & orchestral highs
      }
      "horror_mystery" -> when {
        pos <= 0.15f -> 0.76f // Deep suspenseful rumble
        pos <= 0.60f -> 0.10f // Scooped mids for tense atmosphere
        else -> 0.78f         // Sharp ambient whispers & creaks
      }
      "mecha_scifi" -> when {
        pos <= 0.15f -> 0.70f // Tight electronic sub-bass
        pos <= 0.38f -> 0.48f // Mechanical low-mid impact
        pos <= 0.72f -> 0.55f // Synth & comms clarity
        else -> 0.68f         // Laser & metallic sheen
      }
      "bass" -> if (pos <= 0.35f) 0.85f else 0.10f
      "cinema" -> if (pos <= 0.15f || pos >= 0.85f) 0.65f else 0.30f
      "night" -> if (pos <= 0.15f) -0.30f else 0.35f
      else -> 0f
    }
  }

  fun computeBandLevel(
    presetKey: String,
    bandIndex: Int,
    bandCount: Int,
    minLevel: Int,
    maxLevel: Int
  ): Int {
    val ratio = bandRatio(presetKey, bandIndex, bandCount)
    return (ratio * (maxLevel - minLevel) * 0.45f).roundToInt().coerceIn(minLevel, maxLevel)
  }
}
