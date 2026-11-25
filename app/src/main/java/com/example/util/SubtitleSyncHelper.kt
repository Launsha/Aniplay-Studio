package com.example.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.util.Locale

object SubtitleSyncHelper {

  private val SRT_TIMESTAMP_REGEX = Regex("""(\d{2}):(\d{2}):(\d{2})([,.])(\d{3})""")
  private val ASS_TIMESTAMP_REGEX = Regex("""(\d+):(\d{2}):(\d{2})\.(\d{2})""")

  fun shiftSubtitleContent(content: String, isAss: Boolean, offsetMs: Long): String {
    if (offsetMs == 0L || content.isBlank()) return content
    return if (isAss) {
      ASS_TIMESTAMP_REGEX.replace(content) { match ->
        val h = match.groupValues[1].toLong()
        val m = match.groupValues[2].toLong()
        val s = match.groupValues[3].toLong()
        val cs = match.groupValues[4].toLong()
        val originalMs = ((h * 3600L) + (m * 60L) + s) * 1000L + cs * 10L
        val shifted = (originalMs + offsetMs).coerceAtLeast(0L)
        val nh = shifted / 3_600_000L
        val nm = (shifted % 3_600_000L) / 60_000L
        val ns = (shifted % 60_000L) / 1000L
        val ncs = (shifted % 1000L) / 10L
        String.format(Locale.US, "%d:%02d:%02d.%02d", nh, nm, ns, ncs)
      }
    } else {
      SRT_TIMESTAMP_REGEX.replace(content) { match ->
        val h = match.groupValues[1].toLong()
        val m = match.groupValues[2].toLong()
        val s = match.groupValues[3].toLong()
        val sep = match.groupValues[4]
        val ms = match.groupValues[5].toLong()
        val originalMs = ((h * 3600L) + (m * 60L) + s) * 1000L + ms
        val shifted = (originalMs + offsetMs).coerceAtLeast(0L)
        val nh = shifted / 3_600_000L
        val nm = (shifted % 3_600_000L) / 60_000L
        val ns = (shifted % 60_000L) / 1000L
        val nms = shifted % 1000L
        String.format(Locale.US, "%02d:%02d:%02d%s%03d", nh, nm, ns, sep, nms)
      }
    }
  }

  fun createShiftedSubtitleUri(
    context: Context,
    contentResolver: ContentResolver,
    sourceUri: String,
    isAss: Boolean,
    episodeId: Long,
    offsetMs: Long
  ): String? {
    if (offsetMs == 0L) return sourceUri
    return runCatching {
      val rawText = contentResolver.openInputStream(Uri.parse(sourceUri))?.use { stream ->
        stream.bufferedReader().readText()
      } ?: return null
      val shifted = shiftSubtitleContent(rawText, isAss = isAss, offsetMs = offsetMs)
      val syncDir = File(context.cacheDir, "sub_sync_cache").apply { mkdirs() }
      val ext = if (isAss) "ass" else "srt"
      val outFile = File(syncDir, "sub_${episodeId}_${offsetMs}.$ext")
      outFile.writeText(shifted, Charsets.UTF_8)
      Uri.fromFile(outFile).toString()
    }.getOrNull()
  }
}
