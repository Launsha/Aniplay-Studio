package com.example.data.scanner

data class ParsedAnimeInfo(
  val title: String,
  val episodeNumber: Int,
  val releaseYear: Int? = null,
  val genre: String = "Anime"
)

object AnimeTitleParser {

  private val bracketTagRegex = Regex("""\[([^\]]*)\]""")
  private val parenTagRegex = Regex("""\(([^)]*)\)""")
  private val yearRegex = Regex("""\b(19[89]\d|20[0-2]\d)\b""")
  private val techTokenRegex = Regex(
    """(?i)\b(1080p|720p|480p|360p|2160p|4k|8bit|10bit|hi10p|hevc|h264|h265|x264|x265|av1|aac|flac|opus|web-dl|webrip|bdrip|bluray|dvdrip|hdrip|multi-sub|multisub|sub\s*indo|indo\s*sub|raw|uncensored|dual\s*audio)\b"""
  )
  private val knownGenres = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror",
    "Isekai", "Mecha", "Mystery", "Psychological", "Romance",
    "Sci-Fi", "Slice of Life", "Sports", "Supernatural", "Thriller"
  )

  // Matches e.g. "Title - 05", "Title - Ep 05", "Title Episode 12", "Title S01E05", "Title #05"
  private val seasonEpisodeRegex = Regex("""(?i)^(.*?)[.\s_-]*S(\d{1,2})E(\d{1,4})(?:v\d+)?(?:\b.*)?$""")
  private val dashEpisodeRegex = Regex("""(?i)^(.*?)\s*[-–—]\s*(?:ep(?:isode)?\.?\s*)?(\d{1,4})(?:v\d+)?(?:\s+.*)?$""")
  private val explicitEpisodeRegex = Regex("""(?i)^(.*?)[.\s_-]*(?:episode|eps?|#)\s*0*(\d{1,4})(?:v\d+)?(?:\b.*)?$""")
  private val trailingNumberRegex = Regex("""^(.*?)\s+0*(\d{1,4})(?:v\d+)?$""")
  private val standaloneNumberRegex = Regex("""^(?:ep(?:isode)?\.?\s*)?0*(\d{1,4})(?:v\d+)?$""", RegexOption.IGNORE_CASE)

  fun parse(fileName: String, parentFolderName: String?): ParsedAnimeInfo {
    val baseName = fileName.substringBeforeLast('.')
    val folderClean = parentFolderName
      ?.trim()
      ?.takeIf { it.isNotBlank() && !isRootDownloadFolder(it) }

    val allTags = mutableListOf<String>()
    bracketTagRegex.findAll(baseName).forEach { allTags.add(it.groupValues[1]) }
    parenTagRegex.findAll(baseName).forEach { allTags.add(it.groupValues[1]) }
    folderClean?.let { allTags.add(it) }

    val detectedYear = extractYear(allTags, baseName)
    val detectedGenre = extractGenre(allTags)

    // Remove bracketed/parenthesized metadata tags to isolate core title & episode
    var core = baseName
      .replace(bracketTagRegex, " ")
      .replace(parenTagRegex, " ")
      .replace('_', ' ')
      .replace(techTokenRegex, " ")
      .replace(Regex("""\s+"""), " ")
      .trim()
      .trim('-', '.', '_')
      .trim()

    // Normalize dots when title uses dot-separated naming like "Attack.on.Titan.S01E02"
    if (!core.contains(' ') && core.contains('.')) {
      core = core.replace('.', ' ').replace(Regex("""\s+"""), " ").trim()
    }

    var parsedTitle: String? = null
    var parsedEpisode: Int? = null

    val seasonMatch = seasonEpisodeRegex.find(core)
    if (seasonMatch != null) {
      val rawTitle = cleanTitleSegment(seasonMatch.groupValues[1])
      val seasonNum = seasonMatch.groupValues[2].toIntOrNull()
      val epNum = seasonMatch.groupValues[3].toIntOrNull()
      if (epNum != null) {
        parsedEpisode = epNum
        parsedTitle = if (rawTitle.isNotBlank()) {
          if (seasonNum != null && seasonNum > 1) "$rawTitle S$seasonNum" else rawTitle
        } else null
      }
    }

    if (parsedEpisode == null) {
      val dashMatch = dashEpisodeRegex.find(core)
      if (dashMatch != null) {
        val rawTitle = cleanTitleSegment(dashMatch.groupValues[1])
        val epNum = dashMatch.groupValues[2].toIntOrNull()
        if (epNum != null && rawTitle.isNotBlank()) {
          parsedTitle = rawTitle
          parsedEpisode = epNum
        }
      }
    }

    if (parsedEpisode == null) {
      val explicitMatch = explicitEpisodeRegex.find(core)
      if (explicitMatch != null) {
        val rawTitle = cleanTitleSegment(explicitMatch.groupValues[1])
        val epNum = explicitMatch.groupValues[2].toIntOrNull()
        if (epNum != null && rawTitle.isNotBlank()) {
          parsedTitle = rawTitle
          parsedEpisode = epNum
        }
      }
    }

    if (parsedEpisode == null) {
      val trailingMatch = trailingNumberRegex.find(core)
      if (trailingMatch != null) {
        val rawTitle = cleanTitleSegment(trailingMatch.groupValues[1])
        val num = trailingMatch.groupValues[2].toIntOrNull()
        // Avoid misinterpreting a release year (e.g. 2024) as episode 2024
        if (num != null && num in 1..1900 && rawTitle.isNotBlank()) {
          parsedTitle = rawTitle
          parsedEpisode = num
        }
      }
    }

    if (parsedEpisode == null) {
      val standaloneMatch = standaloneNumberRegex.find(core)
      if (standaloneMatch != null) {
        val num = standaloneMatch.groupValues[1].toIntOrNull()
        if (num != null && num in 1..1900) {
          parsedEpisode = num
          // Title missing from filename -> fall back to parent folder name
          parsedTitle = folderClean
        }
      }
    }

    // Fallback rule: if parsing fails, use parent folder name as title (or cleaned filename if directly in Download)
    val finalTitle = parsedTitle?.takeIf { it.isNotBlank() }
      ?: folderClean
      ?: cleanTitleSegment(core).ifBlank { baseName }

    val finalEpisode = (parsedEpisode ?: 1).coerceAtLeast(1)

    return ParsedAnimeInfo(
      title = finalTitle,
      episodeNumber = finalEpisode,
      releaseYear = detectedYear,
      genre = detectedGenre
    )
  }

  private fun cleanTitleSegment(raw: String): String {
    return raw
      .replace('.', ' ')
      .replace('_', ' ')
      .replace(techTokenRegex, " ")
      .replace(Regex("""\s+"""), " ")
      .trim()
      .trim('-', '–', '—', '_')
      .trim()
  }

  private fun extractYear(tags: List<String>, rawName: String): Int? {
    for (tag in tags) {
      val match = yearRegex.find(tag)
      if (match != null) return match.groupValues[1].toIntOrNull()
    }
    val rawMatch = yearRegex.find(rawName)
    return rawMatch?.groupValues?.get(1)?.toIntOrNull()
  }

  private fun extractGenre(tags: List<String>): String {
    val joined = tags.joinToString(" ")
    val matched = knownGenres.firstOrNull { genre ->
      joined.contains(genre, ignoreCase = true)
    }
    return matched ?: "Anime"
  }

  private fun isRootDownloadFolder(name: String): Boolean {
    return name.equals("Download", ignoreCase = true) ||
      name.equals("Downloads", ignoreCase = true)
  }
}
