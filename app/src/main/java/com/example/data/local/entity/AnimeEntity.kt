package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "anime",
  indices = [Index(value = ["title"], unique = true)]
)
data class AnimeEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val title: String,
  val coverUri: String? = null,
  val episodeCount: Int = 0,
  val genre: String = "Anime",
  val releaseYear: Int? = null,
  val lastPlayedAt: Long = 0L,
  val synopsis: String = "",
  val releaseDate: String = "",
  val customCoverUri: String? = null,
  val isUserEdited: Boolean = false,
  val malId: Int? = null,
  val malScore: Double? = null,
  val malStatus: String = "watching",
  val malUserScore: Int? = null,
  val isFavorite: Boolean = false
) {
  val effectiveCoverUri: String?
    get() = customCoverUri?.takeIf { it.isNotBlank() } ?: coverUri
}
