package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JikanSearchResponse(
  val data: List<JikanAnimeDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JikanAnimeDto(
  @Json(name = "mal_id") val malId: Int,
  val url: String? = null,
  val title: String,
  @Json(name = "title_english") val titleEnglish: String? = null,
  val synopsis: String? = null,
  val score: Double? = null,
  val year: Int? = null,
  val episodes: Int? = null,
  val aired: JikanAiredDto? = null,
  val images: JikanImagesDto? = null,
  val genres: List<JikanNamedItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JikanAiredDto(
  val from: String? = null,
  val string: String? = null
)

@JsonClass(generateAdapter = true)
data class JikanImagesDto(
  val jpg: JikanImageUrlsDto? = null
)

@JsonClass(generateAdapter = true)
data class JikanImageUrlsDto(
  @Json(name = "image_url") val imageUrl: String? = null,
  @Json(name = "large_image_url") val largeImageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class JikanNamedItem(
  val name: String
)

data class MalAnimeCandidate(
  val malId: Int,
  val title: String,
  val coverUrl: String?,
  val synopsis: String,
  val genre: String,
  val releaseDate: String,
  val releaseYear: Int?,
  val score: Double?
)
