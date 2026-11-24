package com.example.util

import com.example.data.local.entity.AnimeEntity
import com.example.data.local.entity.AnimeWithStats
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MalXmlExporterTest {

  @Test
  fun generateMalExportXml_escapesSpecialCharactersAndMapsStatuses() {
    val animeList = listOf(
      AnimeWithStats(
        anime = AnimeEntity(
          id = 1L,
          title = "Fate/stay night & <UBW> \"Demo\" 'Test'",
          coverUri = null,
          episodeCount = 24,
          malId = 22297,
          malStatus = "completed",
          malUserScore = 9
        ),
        watchedCount = 24
      ),
      AnimeWithStats(
        anime = AnimeEntity(
          id = 2L,
          title = "Frieren",
          coverUri = null,
          episodeCount = 28,
          malId = null,
          malStatus = "plan_to_watch",
          malUserScore = null
        ),
        watchedCount = 0
      )
    )

    val xml = MalXmlExporter.generateMalExportXml(animeList)

    assertTrue(xml.contains("<user_total_anime>2</user_total_anime>"))
    assertTrue(xml.contains("<series_animedb_id>22297</series_animedb_id>"))
    assertTrue(xml.contains("<series_title>Fate/stay night &amp; &lt;UBW&gt; &quot;Demo&quot; &apos;Test&apos;</series_title>"))
    assertTrue(xml.contains("<my_status>Completed</my_status>"))
    assertTrue(xml.contains("<my_score>9</my_score>"))
    assertTrue(xml.contains("<my_status>Plan to Watch</my_status>"))
    assertTrue(xml.contains("<series_animedb_id>0</series_animedb_id>"))
    assertFalse(xml.contains("<series_title>Fate/stay night & <UBW>"))
  }
}
