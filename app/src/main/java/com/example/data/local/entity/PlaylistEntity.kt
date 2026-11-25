package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "playlist_anime",
  primaryKeys = ["playlistId", "animeId"],
  foreignKeys = [
    ForeignKey(
      entity = PlaylistEntity::class,
      parentColumns = ["id"],
      childColumns = ["playlistId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = AnimeEntity::class,
      parentColumns = ["id"],
      childColumns = ["animeId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["playlistId"]),
    Index(value = ["animeId"])
  ]
)
data class PlaylistAnimeCrossRef(
  val playlistId: Long,
  val animeId: Long,
  val addedAt: Long = System.currentTimeMillis()
)

data class PlaylistSummary(
  val id: Long,
  val name: String,
  val createdAt: Long,
  val animeCount: Int
)
