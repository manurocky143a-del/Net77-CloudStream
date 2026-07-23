package com.horis.net77.entities

import com.fasterxml.jackson.annotation.JsonProperty

// ─────────────────────────────────────────────────────────────
// Search
// ─────────────────────────────────────────────────────────────

data class SearchData(
    @JsonProperty("head")         val head: String?,
    @JsonProperty("searchResult") val searchResult: List<SearchResult>?,
    @JsonProperty("nextPage")     val nextPage: Int?
)

data class SearchResult(
    @JsonProperty("id")   val id: String?,
    @JsonProperty("t")    val title: String?,
    @JsonProperty("i")    val image: String?,
    @JsonProperty("type") val type: String?
)

// ─────────────────────────────────────────────────────────────
// Post detail
// ─────────────────────────────────────────────────────────────

data class PostData(
    @JsonProperty("title")    val title: String?,
    @JsonProperty("desc")     val desc: String?,
    @JsonProperty("img")      val img: String?,
    @JsonProperty("year")     val year: String?,
    @JsonProperty("genre")    val genre: String?,
    @JsonProperty("imdb")     val imdb: String?,
    @JsonProperty("runtime")  val runtime: String?,
    @JsonProperty("type")     val type: String?,
    @JsonProperty("director") val director: String?,
    @JsonProperty("ua")       val ua: String?,
    @JsonProperty("seasons")  val seasons: List<SeasonItem?>?,
    @JsonProperty("episodes") val episodes: List<EpItem?>?,
    @JsonProperty("nextPage") val nextPage: Int?
)

// ─────────────────────────────────────────────────────────────
// Season / Episode
// ─────────────────────────────────────────────────────────────

/** A season entry — NOTE: renamed SeasonItem to avoid potential future conflicts */
data class SeasonItem(
    @JsonProperty("ep") val ep: String?,   // episode count
    @JsonProperty("id") val id: String?,   // season ID used for episodes API
    @JsonProperty("s")  val s: String?     // season number, e.g. "1"
)

data class EpisodesData(
    @JsonProperty("episodes")       val episodes: List<EpItem>?,
    @JsonProperty("nextPage")       val nextPage: Int?,
    @JsonProperty("nextPageSeason") val nextPageSeason: Int?
)

/**
 * NOTE: renamed to EpItem to avoid clash with CloudStream3's own Episode class.
 * CloudStream's com.lagradost.cloudstream3.Episode is used for the LoadResponse;
 * this class is only used for JSON deserialization from the API.
 */
data class EpItem(
    @JsonProperty("complate") val complate: String?,
    @JsonProperty("ep")       val ep: String?,
    @JsonProperty("id")       val id: String?,
    @JsonProperty("t")        val t: String?,
    @JsonProperty("img")      val img: String?
)

// ─────────────────────────────────────────────────────────────
// Playlist
// ─────────────────────────────────────────────────────────────

data class PlayList(
    @JsonProperty("sources") val sources: List<StreamSource>?,
    @JsonProperty("tracks")  val tracks: List<SubTrack>?
)

/** A video source — renamed StreamSource to avoid possible future conflict with CS3 */
data class StreamSource(
    @JsonProperty("file")  val file: String?,
    @JsonProperty("label") val label: String?,
    @JsonProperty("type")  val type: String?
)

/** A subtitle track — renamed SubTrack for clarity */
data class SubTrack(
    @JsonProperty("kind")    val kind: String?,
    @JsonProperty("file")    val file: String?,
    @JsonProperty("label")   val label: String?,
    @JsonProperty("default") val default: String?
)
