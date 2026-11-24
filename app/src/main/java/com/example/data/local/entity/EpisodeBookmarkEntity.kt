package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "episode_bookmarks",
  foreignKeys = [
    ForeignKey(
      entity = EpisodeEntity::class,
      parentColumns = ["id"],
      childColumns = ["episodeId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index(value = ["episodeId"])]
)
data class EpisodeBookmarkEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val episodeId: Long,
  val animeId: Long,
  val episodeNumber: Int,
  val positionMs: Long,
  val label: String,
  val createdAt: Long = System.currentTimeMillis()
)
