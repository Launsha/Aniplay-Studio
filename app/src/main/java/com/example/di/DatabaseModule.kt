package com.example.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.example.data.local.AnimeDatabase
import com.example.data.local.dao.AnimeDao
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.EpisodeDao
import com.example.data.local.dao.PlaylistDao
import com.example.data.local.dao.WatchActivityDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideAnimeDatabase(@ApplicationContext context: Context): AnimeDatabase {
    return Room.databaseBuilder(
      context,
      AnimeDatabase::class.java,
      "animeplay.db"
    )
      .addMigrations(
        AnimeDatabase.MIGRATION_1_2,
        AnimeDatabase.MIGRATION_2_3,
        AnimeDatabase.MIGRATION_3_4
      )
      .build()
  }

  @Provides
  fun provideAnimeDao(database: AnimeDatabase): AnimeDao = database.animeDao()

  @Provides
  fun provideEpisodeDao(database: AnimeDatabase): EpisodeDao = database.episodeDao()

  @Provides
  fun provideWatchActivityDao(database: AnimeDatabase): WatchActivityDao = database.watchActivityDao()

  @Provides
  fun provideBookmarkDao(database: AnimeDatabase): BookmarkDao = database.bookmarkDao()

  @Provides
  fun providePlaylistDao(database: AnimeDatabase): PlaylistDao = database.playlistDao()

  @Provides
  fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
    context.contentResolver
}
