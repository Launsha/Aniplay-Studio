package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AnimeDao
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.EpisodeDao
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.WatchActivityDao
import com.example.data.local.entity.AnimeEntity
import com.example.data.local.entity.EpisodeBookmarkEntity
import com.example.data.local.entity.EpisodeEntity
import com.example.data.local.entity.PlaylistAnimeCrossRef
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.WatchActivityEntity

@Database(
  entities = [
    AnimeEntity::class,
    EpisodeEntity::class,
    WatchActivityEntity::class,
    EpisodeBookmarkEntity::class,
    PlaylistEntity::class,
    PlaylistAnimeCrossRef::class
  ],
  version = 4,
  exportSchema = false
)
abstract class AnimeDatabase : RoomDatabase() {
  abstract fun animeDao(): AnimeDao
  abstract fun episodeDao(): EpisodeDao
  abstract fun watchActivityDao(): WatchActivityDao
  abstract fun bookmarkDao(): BookmarkDao
  abstract fun playlistDao(): PlaylistDao

  companion object {
    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE anime ADD COLUMN synopsis TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE anime ADD COLUMN releaseDate TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE anime ADD COLUMN customCoverUri TEXT")
        db.execSQL("ALTER TABLE anime ADD COLUMN isUserEdited INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE anime ADD COLUMN malId INTEGER")
        db.execSQL("ALTER TABLE anime ADD COLUMN malScore REAL")
        db.execSQL("ALTER TABLE anime ADD COLUMN malStatus TEXT NOT NULL DEFAULT 'watching'")
        db.execSQL("ALTER TABLE anime ADD COLUMN malUserScore INTEGER")
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS watch_activity (
            dateKey TEXT NOT NULL PRIMARY KEY,
            dayOfWeek INTEGER NOT NULL,
            watchedMs INTEGER NOT NULL DEFAULT 0
          )
          """.trimIndent()
        )
      }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE anime ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS episode_bookmarks (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            episodeId INTEGER NOT NULL,
            animeId INTEGER NOT NULL,
            episodeNumber INTEGER NOT NULL,
            positionMs INTEGER NOT NULL,
            label TEXT NOT NULL,
            createdAt INTEGER NOT NULL,
            FOREIGN KEY(episodeId) REFERENCES episodes(id) ON DELETE CASCADE
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_episode_bookmarks_episodeId ON episode_bookmarks(episodeId)")
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE episodes ADD COLUMN introStartMs INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE episodes ADD COLUMN introEndMs INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE episodes ADD COLUMN outroStartMs INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE episodes ADD COLUMN outroEndMs INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE episodes ADD COLUMN introOutroSource TEXT NOT NULL DEFAULT ''")
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS playlists (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            createdAt INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS playlist_anime (
            playlistId INTEGER NOT NULL,
            animeId INTEGER NOT NULL,
            addedAt INTEGER NOT NULL,
            PRIMARY KEY(playlistId, animeId),
            FOREIGN KEY(playlistId) REFERENCES playlists(id) ON DELETE CASCADE,
            FOREIGN KEY(animeId) REFERENCES anime(id) ON DELETE CASCADE
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_anime_playlistId ON playlist_anime(playlistId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_anime_animeId ON playlist_anime(animeId)")
      }
    }
  }
}
