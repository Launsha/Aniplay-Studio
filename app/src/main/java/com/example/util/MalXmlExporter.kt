package com.example.util

import com.example.data.local.entity.AnimeWithStats

object MalXmlExporter {

  fun generateMalExportXml(animeList: List<AnimeWithStats>): String {
    val builder = StringBuilder()
    builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    builder.append("<myanimelist>\n")
    builder.append("  <myinfo>\n")
    builder.append("    <user_export_type>1</user_export_type>\n")
    builder.append("    <user_total_anime>${animeList.size}</user_total_anime>\n")
    builder.append("  </myinfo>\n")

    for (item in animeList) {
      val anime = item.anime
      val statusLabel = when (anime.malStatus) {
        "completed" -> "Completed"
        "on_hold" -> "On-Hold"
        "dropped" -> "Dropped"
        "plan_to_watch" -> "Plan to Watch"
        else -> "Watching"
      }
      builder.append("  <anime>\n")
      builder.append("    <series_animedb_id>${anime.malId ?: 0}</series_animedb_id>\n")
      builder.append("    <series_title>${escapeXml(anime.title)}</series_title>\n")
      builder.append("    <series_episodes>${anime.episodeCount}</series_episodes>\n")
      builder.append("    <my_watched_episodes>${item.watchedCount}</my_watched_episodes>\n")
      builder.append("    <my_score>${anime.malUserScore ?: 0}</my_score>\n")
      builder.append("    <my_status>$statusLabel</my_status>\n")
      builder.append("    <update_on_import>1</update_on_import>\n")
      builder.append("  </anime>\n")
    }

    builder.append("</myanimelist>\n")
    return builder.toString()
  }

  private fun escapeXml(input: String): String {
    return buildString(input.length) {
      for (ch in input) {
        when (ch) {
          '&' -> append("&amp;")
          '<' -> append("&lt;")
          '>' -> append("&gt;")
          '"' -> append("&quot;")
          '\'' -> append("&apos;")
          else -> append(ch)
        }
      }
    }
  }
}
