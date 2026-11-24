package com.example.data.scanner

import android.content.ContentResolver
import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ScannedVideoFile(
  val fileUri: String,
  val fileName: String,
  val folderPath: String,
  val parentFolderName: String?,
  val durationMs: Long,
  val subtitleUri: String?,
  val parsedInfo: ParsedAnimeInfo
)

@Singleton
class MediaStoreScanner @Inject constructor(
  private val contentResolver: ContentResolver
) {

  private val supportedVideoExtensions = setOf("mp4", "mkv", "avi", "webm")
  private val supportedSubtitleExtensions = setOf("srt", "ass")

  suspend fun scanDownloadVideos(): List<ScannedVideoFile> = withContext(Dispatchers.IO) {
    val subtitleIndex = scanSubtitlesInDownload()
    val results = mutableListOf<ScannedVideoFile>()

    val projection = buildList {
      add(MediaStore.Video.Media._ID)
      add(MediaStore.Video.Media.DISPLAY_NAME)
      add(MediaStore.Video.Media.DURATION)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(MediaStore.Video.Media.RELATIVE_PATH)
      }
      @Suppress("DEPRECATION")
      add(MediaStore.Video.Media.DATA)
    }.toTypedArray()

    val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

    contentResolver.query(
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
      projection,
      null,
      null,
      sortOrder
    )?.use { cursor ->
      val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
      val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
      val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
      val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
      } else {
        -1
      }
      @Suppress("DEPRECATION")
      val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

      while (cursor.moveToNext()) {
        val id = cursor.getLong(idCol)
        val displayName = cursor.getString(nameCol) ?: continue
        val extension = displayName.substringAfterLast('.', "").lowercase()
        if (extension !in supportedVideoExtensions) continue

        val relativePath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
        val rawDataPath = if (dataCol >= 0) cursor.getString(dataCol) else null

        if (!isInsideDownloadFolder(relativePath, rawDataPath)) continue

        val contentUri = ContentUris.withAppendedId(
          MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
          id
        )

        val folderKey = normalizeFolderKey(relativePath, rawDataPath)
        val parentFolderName = extractParentFolderName(relativePath, rawDataPath)

        var durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
        if (durationMs == 0L) {
          durationMs = extractDurationFallback(contentUri)
        }

        val videoBaseName = displayName.substringBeforeLast('.').lowercase()
        val matchedSubtitle = findMatchingSubtitle(
          subtitleIndex = subtitleIndex,
          folderKey = folderKey,
          videoBaseName = videoBaseName,
          rawDataPath = rawDataPath
        )

        val parsed = AnimeTitleParser.parse(displayName, parentFolderName)

        results.add(
          ScannedVideoFile(
            fileUri = contentUri.toString(),
            fileName = displayName,
            folderPath = folderKey,
            parentFolderName = parentFolderName,
            durationMs = durationMs,
            subtitleUri = matchedSubtitle,
            parsedInfo = parsed
          )
        )
      }
    }

    results
  }

  private fun scanSubtitlesInDownload(): Map<String, List<Pair<String, String>>> {
    val map = mutableMapOf<String, MutableList<Pair<String, String>>>()
    val filesUri = MediaStore.Files.getContentUri("external")

    val projection = buildList {
      add(MediaStore.Files.FileColumns._ID)
      add(MediaStore.Files.FileColumns.DISPLAY_NAME)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(MediaStore.MediaColumns.RELATIVE_PATH)
      }
      @Suppress("DEPRECATION")
      add(MediaStore.MediaColumns.DATA)
    }.toTypedArray()

    runCatching {
      contentResolver.query(filesUri, projection, null, null, null)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
          -1
        }
        @Suppress("DEPRECATION")
        val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idCol)
          val displayName = cursor.getString(nameCol) ?: continue
          val ext = displayName.substringAfterLast('.', "").lowercase()
          if (ext !in supportedSubtitleExtensions) continue

          val relPath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
          val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
          if (!isInsideDownloadFolder(relPath, dataPath)) continue

          val folderKey = normalizeFolderKey(relPath, dataPath)
          val baseName = displayName.substringBeforeLast('.').lowercase()
          val uri = ContentUris.withAppendedId(filesUri, id).toString()

          map.getOrPut(folderKey) { mutableListOf() }.add(baseName to uri)
        }
      }
    }

    return map
  }

  private fun findMatchingSubtitle(
    subtitleIndex: Map<String, List<Pair<String, String>>>,
    folderKey: String,
    videoBaseName: String,
    rawDataPath: String?
  ): String? {
    val candidates = subtitleIndex[folderKey].orEmpty()
    // Exact base-name match first (e.g., episode01.srt / episode01.ass)
    candidates.firstOrNull { it.first == videoBaseName }?.let { return it.second }
    // Language suffix match (e.g., episode01.id.srt / episode01.en.ass)
    candidates.firstOrNull { it.first.startsWith("$videoBaseName.") }?.let { return it.second }

    // Direct filesystem fallback if accessible in the same folder
    if (!rawDataPath.isNullOrBlank()) {
      val videoFile = File(rawDataPath)
      val parentDir = videoFile.parentFile
      if (parentDir != null && parentDir.exists()) {
        val base = videoFile.nameWithoutExtension
        for (ext in supportedSubtitleExtensions) {
          val subFile = File(parentDir, "$base.$ext")
          if (subFile.exists() && subFile.canRead()) {
            return Uri.fromFile(subFile).toString()
          }
        }
      }
    }

    return null
  }

  private fun isInsideDownloadFolder(relativePath: String?, dataPath: String?): Boolean {
    if (!relativePath.isNullOrBlank()) {
      val normalized = relativePath.trimStart('/')
      if (normalized.startsWith("Download/", ignoreCase = true) ||
        normalized.startsWith("Downloads/", ignoreCase = true) ||
        normalized.equals("Download", ignoreCase = true) ||
        normalized.equals("Downloads", ignoreCase = true)
      ) {
        return true
      }
    }
    if (!dataPath.isNullOrBlank()) {
      return dataPath.contains("/Download/", ignoreCase = true) ||
        dataPath.contains("/Downloads/", ignoreCase = true)
    }
    return false
  }

  private fun normalizeFolderKey(relativePath: String?, dataPath: String?): String {
    if (!relativePath.isNullOrBlank()) {
      return relativePath.trim('/').lowercase()
    }
    if (!dataPath.isNullOrBlank()) {
      val parent = File(dataPath).parent ?: ""
      val idx = parent.indexOf("Download", ignoreCase = true)
      return if (idx >= 0) parent.substring(idx).trim('/').lowercase() else parent.lowercase()
    }
    return "download"
  }

  private fun extractParentFolderName(relativePath: String?, dataPath: String?): String? {
    if (!relativePath.isNullOrBlank()) {
      val segments = relativePath.trim('/').split('/').filter { it.isNotBlank() }
      return segments.lastOrNull()
    }
    if (!dataPath.isNullOrBlank()) {
      return File(dataPath).parentFile?.name
    }
    return null
  }

  private fun extractDurationFallback(uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(contentResolver.openFileDescriptor(uri, "r")?.use { it.fileDescriptor })
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (_: Exception) {
      0L
    } finally {
      runCatching { retriever.release() }
    }
  }
}
