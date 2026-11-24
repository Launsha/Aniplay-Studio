package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AnimePlayApp : Application(), ImageLoaderFactory {

  override fun newImageLoader(): ImageLoader {
    return ImageLoader.Builder(this)
      .components {
        add(VideoFrameDecoder.Factory())
      }
      .memoryCache {
        MemoryCache.Builder(this)
          .maxSizePercent(0.15)
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve("video_thumbs_cache"))
          .maxSizeBytes(32L * 1024L * 1024L)
          .build()
      }
      .crossfade(true)
      .build()
  }
}
