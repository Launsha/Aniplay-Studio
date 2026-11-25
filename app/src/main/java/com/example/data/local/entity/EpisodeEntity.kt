package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "episodes",
  foreignKeys = [
    ForeignKey(
      entity = AnimeEntity::class,
      parentColumns = ["id"],
      childColumns = ["animeId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["animeId"]),
    Index(value = ["fileUri"], unique = true)
  ]
)
data class EpisodeEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val animeId: Long,
  val number: Int,
  val fileUri: String,
  val fileName: String,
  val folderPath: String,
  val durationMs: Long = 0L,
  val lastPositionMs: Long = 0L,
  val isWatched: Boolean = false,
  val subtitleUri: String? = null,
  val lastPlayedAt: Long = 0L,
  val introStartMs: Long = -1L,
  val introEndMs: Long = -1L,
  val outroStartMs: Long = -1L,
  val outroEndMs: Long = -1L,
  val introOutroSource: String = ""
)
