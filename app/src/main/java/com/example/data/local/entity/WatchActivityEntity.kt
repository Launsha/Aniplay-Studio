package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_activity")
data class WatchActivityEntity(
  @PrimaryKey val dateKey: String, // Format yyyy-MM-dd
  val dayOfWeek: Int,              // 1 = Monday .. 7 = Sunday
  val watchedMs: Long = 0L
)
