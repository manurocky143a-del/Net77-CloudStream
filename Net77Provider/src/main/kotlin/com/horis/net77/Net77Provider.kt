package com.horis.net77

import android.content.Context
import com.horis.net77.entities.*
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import kotlinx.coroutines.delay
import org.jsoup.nodes.Element

class Net77Provider : MainAPI() {

    override var name = "Net77Provider"
    override var lang = "hi"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override var mainUrl = "https://net52.cc"
    private val bypassUrl = "https://net22.cc"

    private val mobileUA =
        "Mozilla/5.0 (Linux; Android 13; Pixel 5 Build/TQ3A.230901.001; wv) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/144.0.7559.132 " +
        "Safari/537.36 /OS.Gatu v3.0"

    private val commonHeaders = mapOf(
        "Accept"                    to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language"           to "en-IN,en-US;q=0.9,en;q=0.8",
        "Cache-Control"             to "max-age=0",
        "Connection"                to "keep-alive",
        "sec-ch-ua"                 to "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\"",
        "sec-ch-ua-mobile"          to "?0",
        "sec-ch-ua-platform"        to "\"Android\"",
        "Sec-Fetch-Dest"            to "document",
        "Sec-Fetch-Mode"            to "navigate",
        "Sec-Fetch-Site"            to "same-origin",
        "Sec-Fetch-User"            to "?1",
        "Upgrade-Insecure-Requests" to "1",
        "User-Agent"                to mobileUA,
        "X-Requested-With"        to "XMLHttpRequest"
    )

    private var cookieValue = ""

    override val mainPage = mainPageOf(
        "$mainUrl/mobile/home?app=1&ott=nf" to "Netflix Mirror",
        "$mainUrl/mobile/home?app=1&ott=hs" to "Hotstar Mirror",
        "$mainUrl/mobile/home?app=1&ott=pv" to "Prime Video Mirror"
    )

    private suspend fun getCookieCached(): String {
        val (saved, ts) = Net77Storage.getCookie()
        val maxAge = 15 * 60 * 60 * 1000L
        if (!saved.isNullOrEmpty() && ts != null &&
            System.currentTimeMillis() - ts < maxAge) {
            return saved
        }
        return try {
            val fresh = fetchBypassCookie()
            if (fresh.isNotEmpty()) Net77Storage.saveCookie(fresh)
            fresh
        } catch (e: Exception) {
            e.printStackTrace()
            saved ?: ""
        }
    }

    private suspend fun fetchBypassCookie(): String {
        var response = app.get(bypassUrl, headers = commonHeaders, timeout = 20)

        repeat(3) {
            val html = response.text
            val nextUrl = Regex("""content="\d+;url=([^"]+)"""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.getOrNull(1)
            if (nextUrl != null) {
                delay(500)
                response = app.get(nextUrl, headers = commonHeaders, timeout = 20)
            }
        }

        for (header in response.headers.values("set-cookie")) {
            val m = Regex("""t_hash_t=([^;]+)""").find(header)
            if (m != null) return m.groupValues[1]
        }

        val fromHtml = Regex("""t_hash_t['":\s=]+([A-Za-z0-9_\-]+)""")
            .find(response.text)
        if (fromHtml != null) return fromHtml.groupValues[1]

        val net77 = app.get("https://net77.cc/", headers = commonHeaders, timeout = 20)
        val fromNet77 = Regex("""t_hash_t=([^;]+)""")
            .find(net77.headers.values("set-cookie").joinToString(";"))
        return fromNet77?.groupValues?.getOrNull(1) ?: ""
    }

    private suspend fun buildCookies(ott: String = "nf"): Map<String, String> {
        cookieValue = cookieValue.ifEmpty { getCookieCached() }
        return mapOf(
            "t_hash_t" to cookieValue,
            "ott"      to ott,
            "hd"       to "on"
        )
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (page > 1) return null

        val ottMatch = Regex("""ott=(\w+)""").find(request.data)
        val ott = ottMatch?.groupValues?.getOrNull(1) ?: "nf"
        val cookies = buildCookies(ott)

        val document = app.get(
            request.data,
            cookies = cookies,
            headers = commonHeaders,
            referer = request.data
        ).document

        val sections = document
            .select(".tray-container, #top10")
            .mapNotNull { it.toHomePageList() }

        return newHomePageResponse(sections, hasNext = false)
    }

    private fun Element.toHomePageList(): HomePageList? {
        val sectionName = selectFirst("h2, .tray-title, span.heading")?.text()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val items = select("article, .top10-post, .post-card")
            .mapNotNull { it.toSearchResult() }

        return if (items.isEmpty()) null
        else HomePageList(sectionName, items, isHorizontalImages = true)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val id = attr("data-post")
            .ifBlank { selectFirst("a")?.attr("data-post") ?: "" }
            .ifBlank { return null }

        val title = selectFirst("h3, .post-title, .card-title")?.text()
            ?: selectFirst("a")?.attr("title")
            ?: selectFirst("img")?.attr("alt")
            ?: "Unknown"

        val poster = selectFirst("img")?.let {
            it.attr("data-src").ifBlank { it.attr("src") }
        }?.let { if (it.startsWith("//")) "https:$it" else it }

        val rawType = attr("data-type")
        val tvType = if (rawType.contains("series", true) ||
                         rawType.contains("anime",  true)) TvType.TvSeries
                     else TvType.Movie

        return newMovieSearchResponse(title, id, tvType) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val cookies = buildCookies()
        val url = "$mainUrl/mobile/search?s=${query.encUrlEncode()}&app=1"
        val res = app.get(url, cookies = cookies, headers = commonHeaders, referer = mainUrl)

        val data = tryParseJson<SearchData>(res.text) ?: return emptyList()
        return data.searchResult.orEmpty().mapNotNull { r ->
            val id    = r.id    ?: return@mapNotNull null
            val title = r.title ?: "Unknown"
            val tvType = if (r.type?.contains("series", true) == true ||
                             r.type?.contains("anime",  true) == true) TvType.TvSeries
                         else TvType.Movie
            newMovieSearchResponse(title, id, tvType) {
                this.posterUrl = r.image?.toHttps()
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val postId  = url.trim('/')
        val cookies = buildCookies()

        val apiUrl = "$mainUrl/mobile/post?id=$postId&app=1"
        val res    = app.get(apiUrl, cookies = cookies, headers = commonHeaders, referer = mainUrl)
        val post   = tryParseJson<PostData>(res.text) ?: return null

        val title   = post.title ?: "Unknown"
        val poster  = post.img?.toHttps()
        val year    = post.year?.toIntOrNull()
        val genres  = post.genre?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        val desc    = post.desc

        val isMovie = post.type?.contains("movie", ignoreCase = true) == true ||
                      (post.seasons.isNullOrEmpty() && (post.episodes?.size ?: 0) <= 1)

        if (isMovie) {
            val episodeId = post.episodes?.firstOrNull()?.id ?: postId

            return newMovieLoadResponse(title, episodeId, TvType.Movie, episodeId) {
                this.posterUrl = poster
                this.plot      = desc
                this.year      = year
                this.tags      = genres
                this.duration  = post.runtime?.let { convertRuntimeToMinutes(it) }
            }
        }

        val csEpisodes = mutableListOf<Episode>()

        val seasons = post.seasons?.filterNotNull() ?: emptyList()
        if (seasons.isNotEmpty()) {
            seasons.forEach { season ->
                val seasonNum = season.s?.toIntOrNull() ?: 1
                val eps = fetchSeasonEpisodes(season.id ?: return@forEach, cookies)
                eps.forEach { ep ->
                    buildCsEpisode(ep, seasonNum)?.let { csEpisodes.add(it) }
                }
            }
        } else {
            post.episodes?.filterNotNull()?.forEachIndexed { index, ep ->
                buildCsEpisode(ep, 1, index)?.let { csEpisodes.add(it) }
            }
        }

        return newTvSeriesLoadResponse(title, postId, TvType.TvSeries, csEpisodes) {
            this.posterUrl = poster
            this.plot      = desc
            this.year      = year
            this.tags      = genres
        }
    }

    private fun buildCsEpisode(ep: EpItem, season: Int, fallbackIdx: Int = 0): Episode? {
        val id  = ep.id ?: return null
        val num = ep.ep?.toIntOrNull() ?: (fallbackIdx + 1)
        return newEpisode(id) {
            this.name      = ep.t
            this.season    = season
            this.episode   = num
            this.posterUrl = ep.img?.toHttps()
        }
    }

    private suspend fun fetchSeasonEpisodes(
        seasonId: String,
        cookies: Map<String, String>
    ): List<EpItem> {
        val url = "$mainUrl/mobile/episodes?s=$seasonId&app=1"
        val res = app.get(url, cookies = cookies, headers = commonHeaders, referer = mainUrl)
        return tryParseJson<EpisodesData>(res.text)?.episodes ?: emptyList()
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val episodeId = data.trim('/')
        val cookies   = buildCookies()

        val url = "$mainUrl/mobile/playlist?id=$episodeId&app=1"
        val res = app.get(
            url,
            cookies = cookies,
            headers = commonHeaders,
            referer = "$mainUrl/"
        )

        val playlist = tryParseJson<PlayList>(res.text) ?: return false
        var foundAny = false

        playlist.sources?.forEach { source ->
            val fileUrl = source.file?.toHttps() ?: return@forEach
            val label   = source.label ?: "Auto"
            val quality = getQualityFromName(label)
            val isHls   = source.type?.equals("hls", ignoreCase = true) == true ||
                          fileUrl.contains(".m3u8", ignoreCase = true)

            callback(
                newExtractorLink(
                    source  = name,
                    name    = "$name [$label]",
                    url     = fileUrl,
                    type    = if (isHls) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                ) {
                    this.quality = quality
                    this.referer = "$mainUrl/"
                    this.headers = mapOf("Referer" to "$mainUrl/")
                }
            )
            foundAny = true
        }

        playlist.tracks?.forEach { track ->
            val fileUrl = track.file?.toHttps() ?: return@forEach
            val kind    = track.kind ?: "subtitles"
            if (kind.equals("captions", ignoreCase = true) ||
                kind.equals("subtitles", ignoreCase = true)) {
                subtitleCallback(SubtitleFile(track.label ?: "Unknown", fileUrl))
            }
        }

        return foundAny
    }

    private fun String.toHttps(): String = when {
        startsWith("//")    -> "https:$this"
        startsWith("http:") -> "https${substring(4)}"
        else                -> this
    }

    private fun String.encUrlEncode(): String =
        java.net.URLEncoder.encode(this, "UTF-8").replace("+", "%20")
}
