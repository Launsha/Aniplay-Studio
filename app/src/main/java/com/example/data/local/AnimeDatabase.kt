package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.AnimeDao
import com.example.data.local.dao.EpisodeDao
import com.example.data.local.entity.AnimeEntity
import com.example.data.local.entity.EpisodeEntity

@Database(
  entities = [AnimeEntity::class, EpisodeEntity::class],
  version = 1,
  exportSchema = false
)
abstract class AnimeDatabase : RoomDatabase() {
  abstract fun animeDao(): AnimeDao
  abstract fun episodeDao(): EpisodeDao
}
