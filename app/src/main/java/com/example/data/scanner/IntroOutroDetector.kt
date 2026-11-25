package com.example.data.scanner

import android.content.ContentResolver
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.example.data.local.entity.EpisodeEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class IntroOutroSegment(
  val introStartMs: Long = -1L,
  val introEndMs: Long = -1L,
  val outroStartMs: Long = -1L,
  val outroEndMs: Long = -1L,
  val source: String = "",
  val confidencePercent: Int = 0
) {
  val hasIntro: Boolean get() = introStartMs >= 0L && introEndMs > introStartMs + 10_000L
  val hasOutro: Boolean get() = outroStartMs >= 0L && outroEndMs > outroStartMs + 10_000L
  val hasAny: Boolean get() = hasIntro || hasOutro
}

data class ChapterEntry(
  val title: String,
  val startMs: Long,
  val endMs: Long
)

@Singleton
class IntroOutroDetector @Inject constructor(
  @ApplicationContext private val context: Context,
  private val contentResolver: ContentResolver
) {

  fun detectFastMetadataOnly(episode: EpisodeEntity): IntroOutroSegment {
    val mkvSegment = detectFromMkvChapters(episode.fileUri, episode.durationMs)
    if (mkvSegment.hasIntro && mkvSegment.hasOutro) return mkvSegment

    val subSegment = episode.subtitleUri?.let { subUri ->
      detectFromSubtitleUri(subUri, episode.durationMs)
    }
    return mergeSegments(mkvSegment, subSegment)
  }

  fun detectFull(
    episode: EpisodeEntity,
    siblingEpisode: EpisodeEntity? = null
  ): IntroOutroSegment {
    val fast = detectFastMetadataOnly(episode)
    if (fast.hasIntro && fast.hasOutro) return fast

    val audioSegment = detectFromAudioAnalysis(
      episode = episode,
      siblingEpisode = siblingEpisode
    )
    return mergeSegments(fast, audioSegment)
  }

  private fun mergeSegments(primary: IntroOutroSegment, secondary: IntroOutroSegment?): IntroOutroSegment {
    if (secondary == null || !secondary.hasAny) return primary
    val introStart = if (primary.hasIntro) primary.introStartMs else secondary.introStartMs
    val introEnd = if (primary.hasIntro) primary.introEndMs else secondary.introEndMs
    val outroStart = if (primary.hasOutro) primary.outroStartMs else secondary.outroStartMs
    val outroEnd = if (primary.hasOutro) primary.outroEndMs else secondary.outroEndMs
    val src = when {
      primary.source.isNotBlank() && (primary.hasIntro || primary.hasOutro) -> primary.source
      else -> secondary.source
    }
    val conf = max(primary.confidencePercent, secondary.confidencePercent)
    return IntroOutroSegment(
      introStartMs = introStart,
      introEndMs = introEnd,
      outroStartMs = outroStart,
      outroEndMs = outroEnd,
      source = src,
      confidencePercent = conf
    )
  }

  private fun detectFromMkvChapters(fileUri: String, totalDurationMs: Long): IntroOutroSegment {
    val uri = Uri.parse(fileUri)
    val looksLikeMkvOrWebm = fileUri.lowercase().let {
      it.contains(".mkv") || it.contains(".webm")
    }
    val headBytes = readChunk(uri, fromEnd = false, maxBytes = 512 * 1024)
    val headChapters = headBytes?.let { parseMkvChapters(it) }.orEmpty()
    val tailChapters = if (headChapters.isEmpty() && (looksLikeMkvOrWebm || headBytes != null)) {
      readChunk(uri, fromEnd = true, maxBytes = 256 * 1024)?.let { parseMkvChapters(it) }.orEmpty()
    } else {
      emptyList()
    }
    val chapters = if (headChapters.isNotEmpty()) headChapters else tailChapters
    return classifyChapterEntries(chapters, totalDurationMs)
  }

  private fun readChunk(uri: Uri, fromEnd: Boolean, maxBytes: Int): ByteArray? {
    return runCatching {
      if (fromEnd) {
        contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
          val size = pfd.statSize
          if (size <= 0L) return null
          FileInputStream(pfd.fileDescriptor).use { fis ->
            val channel = fis.channel
            val startPos = max(0L, size - maxBytes.toLong())
            channel.position(startPos)
            val toRead = min(maxBytes.toLong(), size - startPos).toInt()
            val buf = ByteArray(toRead)
            var offset = 0
            while (offset < toRead) {
              val read = fis.read(buf, offset, toRead - offset)
              if (read <= 0) break
              offset += read
            }
            if (offset == toRead) buf else buf.copyOf(offset)
          }
        }
      } else {
        contentResolver.openInputStream(uri)?.use { stream ->
          val buf = ByteArray(maxBytes)
          var offset = 0
          while (offset < maxBytes) {
            val read = stream.read(buf, offset, maxBytes - offset)
            if (read <= 0) break
            offset += read
          }
          if (offset > 0) buf.copyOf(offset) else null
        }
      }
    }.getOrNull()
  }

  private fun detectFromSubtitleUri(subtitleUri: String, totalDurationMs: Long): IntroOutroSegment? {
    val content = runCatching {
      contentResolver.openInputStream(Uri.parse(subtitleUri))?.use { stream ->
        stream.bufferedReader().readText()
      }
    }.getOrNull() ?: return null
    val parsed = parseSubtitleForIntroOutro(content, totalDurationMs)
    return if (parsed.hasAny) parsed else null
  }

  private fun detectFromAudioAnalysis(
    episode: EpisodeEntity,
    siblingEpisode: EpisodeEntity?
  ): IntroOutroSegment? {
    val durationMs = episode.durationMs.takeIf { it > 0L } ?: probeDurationMs(episode.fileUri)
    if (durationMs < 180_000L) return null

    // Compare 1s RMS audio energy buckets between this episode and a sibling episode if present
    val ep1Intro = extractRmsProfile(episode.fileUri, startMs = 0L, windowMs = 240_000L)
    val ep1OutroStart = max(0L, durationMs - 240_000L)
    val ep1Outro = extractRmsProfile(episode.fileUri, startMs = ep1OutroStart, windowMs = 240_000L)

    if (siblingEpisode != null && siblingEpisode.id != episode.id) {
      val sibDuration = siblingEpisode.durationMs.takeIf { it > 0L } ?: probeDurationMs(siblingEpisode.fileUri)
      if (sibDuration >= 180_000L) {
        val ep2Intro = extractRmsProfile(siblingEpisode.fileUri, startMs = 0L, windowMs = 240_000L)
        val ep2Outro = extractRmsProfile(
          siblingEpisode.fileUri,
          startMs = max(0L, sibDuration - 240_000L),
          windowMs = 240_000L
        )
        val matched = matchAudioProfiles(
          ep1Intro = ep1Intro,
          ep2Intro = ep2Intro,
          ep1Outro = ep1Outro,
          ep2Outro = ep2Outro,
          ep1OutroWindowStartMs = ep1OutroStart
        )
        if (matched.hasAny) return matched
      }
    }

    // Single-episode fallback: detect prolonged music/silence boundary dips in OP/ED windows
    return detectSingleEpisodeAudioBoundaries(ep1Intro, ep1Outro, ep1OutroStart, durationMs)
  }

  private fun probeDurationMs(fileUri: String): Long {
    val extractor = MediaExtractor()
    return runCatching {
      extractor.setDataSource(context, Uri.parse(fileUri), null)
      for (i in 0 until extractor.trackCount) {
        val fmt = extractor.getTrackFormat(i)
        if (fmt.containsKey(MediaFormat.KEY_DURATION)) {
          return@runCatching fmt.getLong(MediaFormat.KEY_DURATION) / 1000L
        }
      }
      0L
    }.getOrDefault(0L).also {
      runCatching { extractor.release() }
    }
  }

  /**
   * Decodes audio inside [startMs, startMs + windowMs] into 1-second RMS energy buckets (1..N).
   * Designed to be lightweight for 2GB RAM devices by decoding at most ~300ms per 1s bucket.
   */
  private fun extractRmsProfile(fileUri: String, startMs: Long, windowMs: Long): DoubleArray {
    val bucketCount = (windowMs / 1000L).toInt().coerceIn(1, 240)
    val buckets = DoubleArray(bucketCount)
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    return runCatching {
      extractor.setDataSource(context, Uri.parse(fileUri), null)
      var audioTrack = -1
      var format: MediaFormat? = null
      for (i in 0 until extractor.trackCount) {
        val fmt = extractor.getTrackFormat(i)
        val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
        if (mime.startsWith("audio/")) {
          audioTrack = i
          format = fmt
          break
        }
      }
      if (audioTrack < 0 || format == null) return DoubleArray(0)
      val mime = format.getString(MediaFormat.KEY_MIME) ?: return DoubleArray(0)
      extractor.selectTrack(audioTrack)

      codec = MediaCodec.createDecoderByType(mime).apply {
        configure(format, null, null, 0)
        start()
      }

      val bufferInfo = MediaCodec.BufferInfo()
      for (sec in 0 until bucketCount) {
        val targetUs = (startMs + sec * 1000L) * 1000L
        extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        runCatching { codec?.flush() }

        var sampleSumSq = 0.0
        var sampleCount = 0
        var attempts = 0
        while (attempts < 12 && sampleCount < 4096) {
          attempts++
          val inIdx = codec!!.dequeueInputBuffer(4_000L)
          if (inIdx >= 0) {
            val inBuf = codec!!.getInputBuffer(inIdx)
            val size = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
            if (size < 0) {
              codec!!.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            } else {
              codec!!.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
              extractor.advance()
            }
          }

          val outIdx = codec!!.dequeueOutputBuffer(bufferInfo, 4_000L)
          if (outIdx >= 0) {
            val outBuf = codec!!.getOutputBuffer(outIdx)
            if (outBuf != null && bufferInfo.size > 0) {
              outBuf.position(bufferInfo.offset)
              outBuf.limit(bufferInfo.offset + bufferInfo.size)
              outBuf.order(ByteOrder.LITTLE_ENDIAN)
              val shortBuf = outBuf.asShortBuffer()
              while (shortBuf.hasRemaining() && sampleCount < 4096) {
                val s = shortBuf.get().toDouble() / 32768.0
                sampleSumSq += s * s
                sampleCount++
              }
            }
            codec!!.releaseOutputBuffer(outIdx, false)
          } else if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
            continue
          }
        }
        buckets[sec] = if (sampleCount > 0) sqrt(sampleSumSq / sampleCount) else 0.0
      }
      buckets
    }.getOrDefault(DoubleArray(0)).also {
      runCatching { codec?.stop() }
      runCatching { codec?.release() }
      runCatching { extractor.release() }
    }
  }

  companion object {
    private val OP_KEYWORDS = listOf(
      "opening", "intro", "op ", " op", "[op", "(op", "op1", "op2", "op3",
      "pembuka", "lagu pembuka", "credit"
    )
    private val ED_KEYWORDS = listOf(
      "ending", "outro", "ed ", " ed", "[ed", "(ed", "ed1", "ed2", "ed3",
      "penutup", "lagu penutup", "preview", "next episode", "episode berikutnya"
    )

    private val ASS_TIME_REGEX = Regex("""(\d+):(\d{2}):(\d{2})[.](\d{2})""")
    private val SRT_TIME_REGEX = Regex("""(\d{2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})[,.](\d{3})""")

    fun classifyChapterEntries(chapters: List<ChapterEntry>, totalDurationMs: Long): IntroOutroSegment {
      if (chapters.isEmpty()) return IntroOutroSegment()
      val sorted = chapters.sortedBy { it.startMs }
      var introStart = -1L
      var introEnd = -1L
      var outroStart = -1L
      var outroEnd = -1L

      for (i in sorted.indices) {
        val ch = sorted[i]
        val nextStart = sorted.getOrNull(i + 1)?.startMs ?: totalDurationMs.takeIf { it > ch.startMs } ?: -1L
        val effectiveEnd = if (ch.endMs > ch.startMs) ch.endMs else nextStart
        val lower = " ${ch.title.lowercase()} "
        val dur = effectiveEnd - ch.startMs
        if (introStart < 0L &&
          ch.startMs <= 360_000L &&
          dur in 40_000L..140_000L &&
          (OP_KEYWORDS.any { lower.contains(it) } || ch.title.equals("OP", ignoreCase = true))
        ) {
          introStart = ch.startMs
          introEnd = effectiveEnd
        } else if (outroStart < 0L &&
          (totalDurationMs <= 0L || ch.startMs >= totalDurationMs / 2L) &&
          dur in 40_000L..160_000L &&
          (ED_KEYWORDS.any { lower.contains(it) } || ch.title.equals("ED", ignoreCase = true))
        ) {
          outroStart = ch.startMs
          outroEnd = effectiveEnd
        }
      }

      val hasAny = introStart >= 0L || outroStart >= 0L
      return IntroOutroSegment(
        introStartMs = introStart,
        introEndMs = introEnd,
        outroStartMs = outroStart,
        outroEndMs = outroEnd,
        source = if (hasAny) "chapter" else "",
        confidencePercent = if (hasAny) 96 else 0
      )
    }

    /**
     * Parses Matroska EBML bytes for ChapterAtom blocks containing ChapterTimeStart (0x91),
     * ChapterTimeEnd (0x92), and ChapString (0x85).
     */
    fun parseMkvChapters(bytes: ByteArray): List<ChapterEntry> {
      if (bytes.size < 16) return emptyList()
      val chapters = mutableListOf<ChapterEntry>()
      var idx = 0
      while (idx <= bytes.size - 4) {
        // Look for ChapterAtom EBML ID: 0xB6
        if ((bytes[idx].toInt() and 0xFF) == 0xB6) {
          val (atomLen, lenBytes) = readEbmlVint(bytes, idx + 1)
          if (atomLen in 8L..2048L && idx + 1 + lenBytes + atomLen <= bytes.size) {
            val atomStart = idx + 1 + lenBytes
            val atomEnd = (atomStart + atomLen.toInt()).coerceAtMost(bytes.size)
            val parsed = parseSingleChapterAtom(bytes, atomStart, atomEnd)
            if (parsed != null) {
              chapters.add(parsed)
              idx = atomEnd
              continue
            }
          }
        }
        idx++
      }
      return chapters
    }

    private fun parseSingleChapterAtom(bytes: ByteArray, start: Int, end: Int): ChapterEntry? {
      var cursor = start
      var startNs = -1L
      var endNs = -1L
      var title = ""

      while (cursor < end) {
        val b0 = bytes[cursor].toInt() and 0xFF
        val idLen = when {
          (b0 and 0x80) != 0 -> 1
          (b0 and 0x40) != 0 -> 2
          (b0 and 0x20) != 0 -> 3
          (b0 and 0x10) != 0 -> 4
          else -> return null
        }
        if (cursor + idLen > end) break
        var idVal = 0L
        for (k in 0 until idLen) {
          idVal = (idVal shl 8) or (bytes[cursor + k].toInt() and 0xFF).toLong()
        }
        cursor += idLen
        val (elemSize, sizeBytes) = readEbmlVint(bytes, cursor)
        if (elemSize < 0 || sizeBytes == 0 || cursor + sizeBytes + elemSize > end) break
        cursor += sizeBytes
        val payloadEnd = cursor + elemSize.toInt()

        when (idVal) {
          0x91L -> startNs = readUnsignedBigEndian(bytes, cursor, elemSize.toInt())
          0x92L -> endNs = readUnsignedBigEndian(bytes, cursor, elemSize.toInt())
          0x80L -> {
            // ChapterDisplay container: scan inside for 0x85 ChapString
            val displayTitle = parseChapterDisplayTitle(bytes, cursor, payloadEnd)
            if (displayTitle.isNotBlank() && title.isBlank()) {
              title = displayTitle
            }
          }
        }
        cursor = payloadEnd
      }

      if (startNs < 0L || title.isBlank()) return null
      val startMs = startNs / 1_000_000L
      val endMs = if (endNs > startNs) endNs / 1_000_000L else -1L
      return ChapterEntry(title = title.trim(), startMs = startMs, endMs = endMs)
    }

    private fun parseChapterDisplayTitle(bytes: ByteArray, start: Int, end: Int): String {
      var cursor = start
      while (cursor < end) {
        val b0 = bytes[cursor].toInt() and 0xFF
        val idLen = when {
          (b0 and 0x80) != 0 -> 1
          (b0 and 0x40) != 0 -> 2
          (b0 and 0x20) != 0 -> 3
          (b0 and 0x10) != 0 -> 4
          else -> return ""
        }
        if (cursor + idLen > end) break
        var idVal = 0L
        for (k in 0 until idLen) {
          idVal = (idVal shl 8) or (bytes[cursor + k].toInt() and 0xFF).toLong()
        }
        cursor += idLen
        val (elemSize, sizeBytes) = readEbmlVint(bytes, cursor)
        if (elemSize < 0 || sizeBytes == 0 || cursor + sizeBytes + elemSize > end) break
        cursor += sizeBytes
        if (idVal == 0x85L) {
          return bytes.copyOfRange(cursor, cursor + elemSize.toInt()).toString(Charsets.UTF_8)
        }
        cursor += elemSize.toInt()
      }
      return ""
    }

    private fun readEbmlVint(bytes: ByteArray, offset: Int): Pair<Long, Int> {
      if (offset >= bytes.size) return -1L to 0
      val first = bytes[offset].toInt() and 0xFF
      if (first == 0) return -1L to 0
      var mask = 0x80
      var length = 1
      while (length <= 8 && (first and mask) == 0) {
        mask = mask shr 1
        length++
      }
      if (length > 8 || offset + length > bytes.size) return -1L to 0
      var value = (first and (mask - 1)).toLong()
      for (i in 1 until length) {
        value = (value shl 8) or (bytes[offset + i].toInt() and 0xFF).toLong()
      }
      return value to length
    }

    private fun readUnsignedBigEndian(bytes: ByteArray, offset: Int, len: Int): Long {
      var v = 0L
      for (i in 0 until min(len, 8)) {
        v = (v shl 8) or (bytes[offset + i].toInt() and 0xFF).toLong()
      }
      return v
    }

    /**
     * Parses `.ass`/`.ssa` or `.srt` subtitle files for OP / ED style tags or cue ranges.
     */
    fun parseSubtitleForIntroOutro(content: String, totalDurationMs: Long): IntroOutroSegment {
      if (content.isBlank()) return IntroOutroSegment()
      val isAss = content.contains("[Events]", ignoreCase = true) || content.contains("Dialogue:", ignoreCase = true)
      val cues = if (isAss) extractAssCues(content) else extractSrtCues(content)
      if (cues.isEmpty()) return IntroOutroSegment()

      val opTimes = mutableListOf<Pair<Long, Long>>()
      val edTimes = mutableListOf<Pair<Long, Long>>()

      for (cue in cues) {
        val styleLower = " ${cue.style.lowercase()} "
        val textLower = " ${cue.text.lowercase()} "
        val matchesOp = OP_KEYWORDS.any { styleLower.contains(it) || textLower.contains(it) } ||
          cue.style.equals("OP", ignoreCase = true) ||
          cue.style.equals("OP_Romaji", ignoreCase = true) ||
          cue.style.equals("OP_Kanji", ignoreCase = true)
        val matchesEd = ED_KEYWORDS.any { styleLower.contains(it) || textLower.contains(it) } ||
          cue.style.equals("ED", ignoreCase = true) ||
          cue.style.equals("ED_Romaji", ignoreCase = true) ||
          cue.style.equals("ED_Kanji", ignoreCase = true)

        if (matchesOp && cue.startMs <= 420_000L) {
          opTimes.add(cue.startMs to cue.endMs)
        } else if (matchesEd && (totalDurationMs <= 0L || cue.startMs >= totalDurationMs / 2L)) {
          edTimes.add(cue.startMs to cue.endMs)
        }
      }

      val (introStart, introEnd) = collapseWindow(opTimes)
      val (outroStart, outroEnd) = collapseWindow(edTimes)

      val hasAny = introStart >= 0L || outroStart >= 0L
      return IntroOutroSegment(
        introStartMs = introStart,
        introEndMs = introEnd,
        outroStartMs = outroStart,
        outroEndMs = outroEnd,
        source = if (hasAny) "subtitle" else "",
        confidencePercent = if (hasAny) 90 else 0
      )
    }

    private fun collapseWindow(ranges: List<Pair<Long, Long>>): Pair<Long, Long> {
      if (ranges.isEmpty()) return -1L to -1L
      val sorted = ranges.sortedBy { it.first }
      val start = sorted.first().first
      val end = sorted.maxOf { it.second }
      val span = end - start
      return if (span in 35_000L..140_000L) {
        start to end
      } else if (sorted.size == 1 && span < 15_000L && start >= 0L) {
        // Single explicit marker cue like "[Opening]" -> standard 85s anime OP/ED span
        start to (start + 85_000L)
      } else {
        -1L to -1L
      }
    }

    private data class SubCue(val startMs: Long, val endMs: Long, val style: String, val text: String)

    private fun extractAssCues(content: String): List<SubCue> {
      val list = mutableListOf<SubCue>()
      var startIdx = 1
      var endIdx = 2
      var styleIdx = 3
      var textIdx = 9

      for (rawLine in content.lineSequence()) {
        val line = rawLine.trim()
        if (line.startsWith("Format:", ignoreCase = true)) {
          val cols = line.substringAfter(':').split(',').map { it.trim().lowercase() }
          cols.indexOf("start").takeIf { it >= 0 }?.let { startIdx = it }
          cols.indexOf("end").takeIf { it >= 0 }?.let { endIdx = it }
          cols.indexOf("style").takeIf { it >= 0 }?.let { styleIdx = it }
          cols.indexOf("text").takeIf { it >= 0 }?.let { textIdx = it }
        } else if (line.startsWith("Dialogue:", ignoreCase = true)) {
          val parts = line.substringAfter(':').split(',', limit = textIdx + 1)
          if (parts.size > max(styleIdx, max(startIdx, endIdx))) {
            val sMs = parseAssMs(parts[startIdx].trim()) ?: continue
            val eMs = parseAssMs(parts[endIdx].trim()) ?: continue
            val style = parts[styleIdx].trim()
            val text = parts.getOrNull(textIdx).orEmpty().replace(Regex("""\{[^}]*\}"""), "").trim()
            list.add(SubCue(sMs, eMs, style, text))
          }
        }
      }
      return list
    }

    private fun extractSrtCues(content: String): List<SubCue> {
      val list = mutableListOf<SubCue>()
      val blocks = content.replace("\r\n", "\n").split(Regex("""\n\s*\n"""))
      for (block in blocks) {
        val match = SRT_TIME_REGEX.find(block) ?: continue
        val g = match.groupValues
        val sMs = toMs(g[1].toLong(), g[2].toLong(), g[3].toLong(), g[4].toLong())
        val eMs = toMs(g[5].toLong(), g[6].toLong(), g[7].toLong(), g[8].toLong())
        val text = block.substring(match.range.last + 1).trim()
        list.add(SubCue(sMs, eMs, "", text))
      }
      return list
    }

    private fun parseAssMs(raw: String): Long? {
      val match = ASS_TIME_REGEX.matchEntire(raw) ?: return null
      val g = match.groupValues
      return toMs(g[1].toLong(), g[2].toLong(), g[3].toLong(), g[4].toLong() * 10L)
    }

    private fun toMs(h: Long, m: Long, s: Long, ms: Long): Long =
      ((h * 3600L) + (m * 60L) + s) * 1000L + ms

    /**
     * Cross-episode audio energy fingerprint correlation across 1-second RMS buckets.
     * Finds the best 85-second matching window between Episode A and Episode B.
     */
    fun matchAudioProfiles(
      ep1Intro: DoubleArray,
      ep2Intro: DoubleArray,
      ep1Outro: DoubleArray,
      ep2Outro: DoubleArray,
      ep1OutroWindowStartMs: Long
    ): IntroOutroSegment {
      val opSec = findBestMatchingWindow(ep1Intro, ep2Intro, windowSec = 85)
      val edSec = findBestMatchingWindow(ep1Outro, ep2Outro, windowSec = 85)

      val introStart = if (opSec >= 0) opSec * 1000L else -1L
      val introEnd = if (opSec >= 0) (opSec + 85) * 1000L else -1L
      val outroStart = if (edSec >= 0) ep1OutroWindowStartMs + edSec * 1000L else -1L
      val outroEnd = if (edSec >= 0) outroStart + 85_000L else -1L

      val hasAny = introStart >= 0L || outroStart >= 0L
      return IntroOutroSegment(
        introStartMs = introStart,
        introEndMs = introEnd,
        outroStartMs = outroStart,
        outroEndMs = outroEnd,
        source = if (hasAny) "audio_scan" else "",
        confidencePercent = if (hasAny) 84 else 0
      )
    }

    private fun findBestMatchingWindow(a: DoubleArray, b: DoubleArray, windowSec: Int): Int {
      if (a.size < windowSec + 5 || b.size < windowSec + 5) return -1
      var bestCorr = 0.0
      var bestOffsetA = -1
      val maxStartA = min(a.size - windowSec, 150)
      val maxStartB = min(b.size - windowSec, 150)

      for (offA in 0..maxStartA step 2) {
        for (offB in max(0, offA - 30)..min(maxStartB, offA + 30) step 2) {
          val corr = pearsonCorrelation(a, offA, b, offB, windowSec)
          if (corr > bestCorr) {
            bestCorr = corr
            bestOffsetA = offA
          }
        }
      }
      return if (bestCorr >= 0.72) bestOffsetA else -1
    }

    private fun pearsonCorrelation(
      a: DoubleArray,
      startA: Int,
      b: DoubleArray,
      startB: Int,
      len: Int
    ): Double {
      var sumA = 0.0
      var sumB = 0.0
      for (i in 0 until len) {
        sumA += a[startA + i]
        sumB += b[startB + i]
      }
      val meanA = sumA / len
      val meanB = sumB / len
      var num = 0.0
      var denA = 0.0
      var denB = 0.0
      for (i in 0 until len) {
        val da = a[startA + i] - meanA
        val db = b[startB + i] - meanB
        num += da * db
        denA += da * da
        denB += db * db
      }
      val denom = sqrt(denA * denB)
      return if (denom > 1e-6) num / denom else 0.0
    }

    private fun detectSingleEpisodeAudioBoundaries(
      introProfile: DoubleArray,
      outroProfile: DoubleArray,
      outroWindowStartMs: Long,
      durationMs: Long
    ): IntroOutroSegment? {
      if (introProfile.size < 100 && outroProfile.size < 100) return null
      var introStart = -1L
      var introEnd = -1L
      if (introProfile.size >= 110) {
        val avg = introProfile.average().coerceAtLeast(0.01)
        // Look for a silence dip before OP and sustained music energy for ~85s
        val candidateStart = (0..min(110, introProfile.size - 90))
          .firstOrNull { sec ->
            introProfile[sec] < avg * 0.45 && introProfile[sec + 4] > avg * 0.85
          }
        if (candidateStart != null) {
          introStart = candidateStart * 1000L
          introEnd = (candidateStart + 85) * 1000L
        }
      }

      var outroStart = -1L
      var outroEnd = -1L
      if (outroProfile.size >= 110) {
        val avg = outroProfile.average().coerceAtLeast(0.01)
        val candidateEd = (20..min(outroProfile.size - 90, 140))
          .firstOrNull { sec ->
            abs(outroProfile[sec]) < avg * 0.45 && outroProfile[sec + 4] > avg * 0.85
          }
        if (candidateEd != null) {
          outroStart = outroWindowStartMs + candidateEd * 1000L
          outroEnd = min(durationMs, outroStart + 85_000L)
        }
      }

      if (introStart < 0L && outroStart < 0L) return null
      return IntroOutroSegment(
        introStartMs = introStart,
        introEndMs = introEnd,
        outroStartMs = outroStart,
        outroEndMs = outroEnd,
        source = "audio_scan",
        confidencePercent = 70
      )
    }
  }
}
