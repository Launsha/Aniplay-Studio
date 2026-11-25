package com.example.data.scanner

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
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

data class DetectedStorageVolume(
  val id: String,
  val label: String,
  val isPrimary: Boolean,
  val isRemovable: Boolean,
  val openDocumentTreeIntent: Intent?
)

@Singleton
class MediaStoreScanner @Inject constructor(
  private val contentResolver: ContentResolver
) {

  private val supportedVideoExtensions = setOf("mp4", "mkv", "avi", "webm")
  private val supportedSubtitleExtensions = setOf("srt", "ass")

  suspend fun scanConfiguredFolders(
    allowedFolders: Set<String>,
    treeUris: Set<String>
  ): List<ScannedVideoFile> = withContext(Dispatchers.IO) {
    val normalizedAllowed = allowedFolders
      .map { it.trim().trim('/').lowercase() }
      .filter { it.isNotBlank() }
      .toSet()

    // Extract relative folders from primary internal SAF tree URIs as well
    val treeRelativePaths = treeUris.mapNotNull { extractRelativePathFromTreeUri(it) }
    val allAllowedFolders = (normalizedAllowed + treeRelativePaths).toSet()

    if (allAllowedFolders.isEmpty() && treeUris.isEmpty()) {
      return@withContext emptyList()
    }

    val results = mutableListOf<ScannedVideoFile>()
    val seenKeys = mutableSetOf<String>()

    // 1. Scan MediaStore strictly filtered to configured primary-storage folders
    if (allAllowedFolders.isNotEmpty()) {
      val subtitleIndex = scanSubtitlesInFolders(allAllowedFolders)
      val projection = buildList {
        add(MediaStore.Video.Media._ID)
        add(MediaStore.Video.Media.DISPLAY_NAME)
        add(MediaStore.Video.Media.DURATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          add(MediaStore.Video.Media.RELATIVE_PATH)
          add(MediaStore.MediaColumns.VOLUME_NAME)
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
        val volCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          cursor.getColumnIndex(MediaStore.MediaColumns.VOLUME_NAME)
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

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && volCol >= 0) {
            val volName = cursor.getString(volCol)
            if (!volName.isNullOrBlank() && !volName.equals(MediaStore.VOLUME_EXTERNAL_PRIMARY, ignoreCase = true)) {
              continue
            }
          }

          val relativePath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
          val rawDataPath = if (dataCol >= 0) cursor.getString(dataCol) else null

          if (!matchesConfiguredFolders(relativePath, rawDataPath, allAllowedFolders)) continue

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
          val dedupKey = "$folderKey/${displayName.lowercase()}"
          seenKeys.add(dedupKey)

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
    }

    // 2. Scan SAF document trees for SD Cards, external drives, and primary subfolders
    for (treeUriString in treeUris) {
      runCatching {
        val treeUri = Uri.parse(treeUriString)
        scanDocumentTreeRecursive(
          treeUri = treeUri,
          parentDocId = DocumentsContract.getTreeDocumentId(treeUri),
          parentFolderName = extractTreeFolderName(treeUriString),
          seenKeys = seenKeys,
          out = results
        )
      }
    }

    results
  }

  private fun scanDocumentTreeRecursive(
    treeUri: Uri,
    parentDocId: String,
    parentFolderName: String?,
    seenKeys: MutableSet<String>,
    out: MutableList<ScannedVideoFile>
  ) {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
    val projection = arrayOf(
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME,
      DocumentsContract.Document.COLUMN_MIME_TYPE
    )

    val subFolders = mutableListOf<Pair<String, String>>()
    val subtitleFiles = mutableListOf<Pair<String, String>>()
    val videoEntries = mutableListOf<Pair<String, String>>()

    contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
      val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
      val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
      val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

      while (cursor.moveToNext()) {
        val docId = cursor.getString(idCol) ?: continue
        val name = cursor.getString(nameCol) ?: continue
        val mime = cursor.getString(mimeCol) ?: ""

        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
          subFolders.add(docId to name)
        } else {
          val ext = name.substringAfterLast('.', "").lowercase()
          val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId).toString()
          when {
            ext in supportedVideoExtensions -> videoEntries.add(name to docUri)
            ext in supportedSubtitleExtensions -> subtitleFiles.add(name.substringBeforeLast('.').lowercase() to docUri)
          }
        }
      }
    }

    val folderKey = buildVolumeAwareFolderKey(parentDocId)
    for ((name, docUri) in videoEntries) {
      val dedupKey = "$folderKey/${name.lowercase()}"
      if (dedupKey in seenKeys) continue
      seenKeys.add(dedupKey)

      val baseLower = name.substringBeforeLast('.').lowercase()
      val matchedSub = subtitleFiles.firstOrNull { it.first == baseLower }?.second
        ?: subtitleFiles.firstOrNull { it.first.startsWith("$baseLower.") }?.second

      val durationMs = extractDurationFallback(Uri.parse(docUri))
      val parsed = AnimeTitleParser.parse(name, parentFolderName)

      out.add(
        ScannedVideoFile(
          fileUri = docUri,
          fileName = name,
          folderPath = folderKey,
          parentFolderName = parentFolderName,
          durationMs = durationMs,
          subtitleUri = matchedSub,
          parsedInfo = parsed
        )
      )
    }

    for ((subDocId, subName) in subFolders) {
      scanDocumentTreeRecursive(
        treeUri = treeUri,
        parentDocId = subDocId,
        parentFolderName = subName,
        seenKeys = seenKeys,
        out = out
      )
    }
  }

  private fun scanSubtitlesInFolders(allowedFolders: Set<String>): Map<String, List<Pair<String, String>>> {
    val map = mutableMapOf<String, MutableList<Pair<String, String>>>()
    val filesUri = MediaStore.Files.getContentUri("external")

    val projection = buildList {
      add(MediaStore.Files.FileColumns._ID)
      add(MediaStore.Files.FileColumns.DISPLAY_NAME)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(MediaStore.MediaColumns.RELATIVE_PATH)
        add(MediaStore.MediaColumns.VOLUME_NAME)
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
        val volCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          cursor.getColumnIndex(MediaStore.MediaColumns.VOLUME_NAME)
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

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && volCol >= 0) {
            val volName = cursor.getString(volCol)
            if (!volName.isNullOrBlank() && !volName.equals(MediaStore.VOLUME_EXTERNAL_PRIMARY, ignoreCase = true)) {
              continue
            }
          }

          val relPath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
          val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
          if (!matchesConfiguredFolders(relPath, dataPath, allowedFolders)) continue

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
    candidates.firstOrNull { it.first == videoBaseName }?.let { return it.second }
    candidates.firstOrNull { it.first.startsWith("$videoBaseName.") }?.let { return it.second }

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

  private fun matchesConfiguredFolders(
    relativePath: String?,
    dataPath: String?,
    allowedFolders: Set<String>
  ): Boolean {
    if (!relativePath.isNullOrBlank()) {
      val normalizedRel = relativePath.trim('/').lowercase()
      if (allowedFolders.any { folder ->
          normalizedRel == folder || normalizedRel.startsWith("$folder/")
        }
      ) {
        return true
      }
    }
    if (!dataPath.isNullOrBlank()) {
      val parentDir = File(dataPath).parent?.replace('\\', '/')?.lowercase() ?: ""
      if (allowedFolders.any { folder ->
          parentDir.endsWith("/$folder") || parentDir.contains("/$folder/")
        }
      ) {
        return true
      }
    }
    return false
  }

  private fun normalizeFolderKey(relativePath: String?, dataPath: String?): String {
    if (!relativePath.isNullOrBlank()) {
      return relativePath.trim('/').lowercase()
    }
    if (!dataPath.isNullOrBlank()) {
      return (File(dataPath).parent ?: "").trim('/').lowercase()
    }
    return ""
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
      contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
        retriever.setDataSource(pfd.fileDescriptor)
      }
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    } catch (_: Exception) {
      0L
    } finally {
      runCatching { retriever.release() }
    }
  }

  companion object {
    fun buildVolumeAwareFolderKey(docId: String): String {
      val volume = docId.substringBefore(':', "primary").trim().lowercase()
      val relPart = docId.substringAfter(':', "").trim('/').lowercase()
      return when {
        volume == "primary" || volume.isBlank() -> relPart
        relPart.isBlank() -> "sd_$volume"
        else -> "sd_$volume/$relPart"
      }
    }

    fun isSdCardTreeUri(treeUriString: String): Boolean {
      return runCatching {
        val docId = DocumentsContract.getTreeDocumentId(Uri.parse(treeUriString))
        val volume = docId.substringBefore(':', "primary")
        volume.isNotBlank() && !volume.equals("primary", ignoreCase = true)
      }.getOrDefault(false)
    }

    fun extractRelativePathFromTreeUri(treeUriString: String): String? {
      return runCatching {
        val docId = DocumentsContract.getTreeDocumentId(Uri.parse(treeUriString))
        if (docId.startsWith("primary:", ignoreCase = true)) {
          docId.substringAfter(':').trim('/').lowercase().takeIf { it.isNotBlank() }
        } else {
          null
        }
      }.getOrNull()
    }

    fun extractDisplayFolderLabel(treeUriString: String): String {
      return runCatching {
        val docId = DocumentsContract.getTreeDocumentId(Uri.parse(treeUriString))
        val volume = docId.substringBefore(':', "")
        val path = docId.substringAfter(':', "").trim('/')
        when {
          volume.equals("primary", ignoreCase = true) -> {
            if (path.isBlank()) "Internal" else "Internal: $path"
          }
          volume.isNotBlank() -> {
            if (path.isBlank()) "SD Card ($volume)" else "SD Card ($volume): $path"
          }
          else -> path.ifBlank { treeUriString }
        }
      }.getOrElse { treeUriString }
    }

    fun extractTreeFolderName(treeUriString: String): String? {
      return runCatching {
        val docId = DocumentsContract.getTreeDocumentId(Uri.parse(treeUriString))
        docId.substringAfter(':', "").trim('/').substringAfterLast('/').takeIf { it.isNotBlank() }
      }.getOrNull()
    }

    fun detectAvailableStorageVolumes(context: Context): List<DetectedStorageVolume> {
      val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager ?: return emptyList()
      return runCatching {
        sm.storageVolumes.mapNotNull { vol ->
          if (vol.state != Environment.MEDIA_MOUNTED && vol.state != Environment.MEDIA_MOUNTED_READ_ONLY) {
            return@mapNotNull null
          }
          val desc = runCatching { vol.getDescription(context) }.getOrNull().orEmpty()
          val id = vol.uuid ?: if (vol.isPrimary) "primary" else desc
          val label = desc.ifBlank { vol.uuid ?: if (vol.isPrimary) "Internal" else "SD Card" }
          val treeIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { vol.createOpenDocumentTreeIntent() }.getOrNull()
          } else {
            null
          }
          DetectedStorageVolume(
            id = id,
            label = label,
            isPrimary = vol.isPrimary,
            isRemovable = vol.isRemovable,
            openDocumentTreeIntent = treeIntent
          )
        }
      }.getOrDefault(emptyList())
    }
  }
}
