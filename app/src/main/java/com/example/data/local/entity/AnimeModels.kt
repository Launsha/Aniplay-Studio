package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class EpisodeWithAnime(
  @Embedded val episode: EpisodeEntity,
  @Relation(
    parentColumn = "animeId",
    entityColumn = "id"
  )
  val anime: AnimeEntity
)

data class AnimeWithStats(
  @Embedded val anime: AnimeEntity,
  val watchedCount: Int
)
