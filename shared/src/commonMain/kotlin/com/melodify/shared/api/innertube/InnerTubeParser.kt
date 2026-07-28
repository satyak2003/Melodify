package com.melodify.shared.api.innertube

import com.melodify.shared.domain.model.Artist
import com.melodify.shared.domain.model.SearchResult
import com.melodify.shared.domain.model.Track
import com.melodify.shared.domain.model.TrackSource

object InnerTubeParser {

    fun parseSearchResults(response: SearchResponse): SearchResult {
        val tracks = mutableListOf<Track>()
        
        // The path to contents can vary based on tabbed vs two column
        val sectionListContents = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
            ?: response.contents?.twoColumnSearchResultsRenderer?.primaryContents?.sectionListRenderer?.contents
            
        sectionListContents?.forEach { sectionContent ->
            // 1. Check musicShelfRenderer
            sectionContent.musicShelfRenderer?.contents?.forEach { shelfContent ->
                shelfContent.musicResponsiveListItemRenderer?.let { renderer ->
                    parseTrackFromRenderer(renderer)?.let { track -> tracks.add(track) }
                }
            }
            
            // 2. Check itemSectionRenderer
            sectionContent.itemSectionRenderer?.contents?.forEach { itemSectionContent ->
                // sometimes it's nested in a shelf
                itemSectionContent.musicShelfRenderer?.contents?.forEach { shelfContent ->
                    shelfContent.musicResponsiveListItemRenderer?.let { renderer ->
                        parseTrackFromRenderer(renderer)?.let { track -> tracks.add(track) }
                    }
                }
                // but usually it's direct!
                itemSectionContent.musicResponsiveListItemRenderer?.let { renderer ->
                    parseTrackFromRenderer(renderer)?.let { track -> tracks.add(track) }
                }
            }
            
            // 3. Top result card (musicCardShelfRenderer)
            sectionContent.musicCardShelfRenderer?.title?.runs?.firstOrNull()?.let { titleRun ->
                // For simplicity, we just rely on the other results since musicCardShelfRenderer 
                // requires a different parsing strategy. But we can extract it if needed.
            }
        }
        
        return SearchResult(tracks = tracks)
    }

    fun parseBrowseResults(response: BrowseResponse): List<Track> {
        val tracks = mutableListOf<Track>()
        val sectionListContents = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
        
        sectionListContents?.forEach { sectionContent ->
            sectionContent.musicShelfRenderer?.contents?.forEach { shelfContent ->
                shelfContent.musicResponsiveListItemRenderer?.let { renderer ->
                    parseTrackFromRenderer(renderer)?.let { track ->
                        tracks.add(track)
                    }
                }
            }
        }
        return tracks
    }

    fun parseNextResults(response: NextResponse): List<Track> {
        val tracks = mutableListOf<Track>()
        val contents = response.contents?.singleColumnMusicWatchNextResultsRenderer?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.musicQueueRenderer?.content?.playlistPanelRenderer?.contents
        contents?.forEach { panelContent ->
            panelContent.playlistPanelVideoRenderer?.let { renderer ->
                parseTrackFromRenderer(renderer)?.let { track ->
                    tracks.add(track)
                }
            }
        }
        return tracks
    }

    fun parseBestStreamUrl(playerResponse: PlayerResponse): String? {
        val formats = playerResponse.streamingData?.adaptiveFormats ?: return null
        val best = getHighestQualityStream(formats) ?: return null
        val rawUrl = best.url ?: return null
        // Append &file=audio.m4a for mp4/AAC formats so JavaFX Media recognizes the extension
        return if (best.mimeType?.contains("mp4") == true || best.itag in listOf(141, 140, 139)) {
            if (!rawUrl.contains("&file=")) "$rawUrl&file=audio.m4a" else rawUrl
        } else {
            rawUrl
        }
    }

    fun parseVideoDetails(playerResponse: PlayerResponse): Track? {
        val details = playerResponse.videoDetails ?: return null
        val videoId = details.videoId ?: return null
        val title = details.title ?: "Unknown Title"
        val author = details.author ?: "Unknown Artist"
        val lengthSeconds = details.lengthSeconds?.toIntOrNull() ?: 0
        val thumbnailUrl = details.thumbnail?.thumbnails?.maxByOrNull { it.width ?: 0 }?.url

        return Track(
            id = videoId,
            title = title,
            artists = listOf(Artist(id = author, name = author, thumbnailUrl = null)),
            album = null,
            thumbnailUrl = thumbnailUrl,
            durationMs = lengthSeconds * 1000L,
            youtubeVideoId = videoId,
            source = TrackSource.YOUTUBE
        )
    }

    fun parseTrackFromRenderer(renderer: MusicResponsiveListItemRenderer): Track? {
        // Extract videoId from navigationEndpoint or playlistItemData
        var videoId: String? = renderer.playlistItemData?.videoId
        if (videoId == null) {
             videoId = renderer.flexColumns?.firstOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                ?.navigationEndpoint?.watchEndpoint?.videoId
        }
        if (videoId == null) return null
        
        // Extract Title
        val title = renderer.flexColumns?.getOrNull(0)
            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text ?: "Unknown Title"
            
        // Extract Artist from second flex column
        val artistRuns = renderer.flexColumns?.getOrNull(1)
            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
            
        val artist = artistRuns?.firstOrNull { it.navigationEndpoint?.browseEndpoint != null }?.text 
            ?: artistRuns?.firstOrNull()?.text ?: "Unknown Artist"
            
        // Extract Duration from fixed columns (or it could be in second flex column for some layouts)
        val durationStr = renderer.fixedColumns?.firstOrNull()
            ?.musicResponsiveListItemFixedColumnRenderer?.text?.runs?.firstOrNull()?.text
            ?: artistRuns?.lastOrNull()?.text ?: "0:00"
            
        val durationSeconds = parseDurationToSeconds(durationStr)
        
        val thumbnailUrl = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.maxByOrNull { it.width ?: 0 }?.url
        
        return Track(
            id = videoId,
            title = title,
            artists = listOf(Artist(id = artist, name = artist, thumbnailUrl = thumbnailUrl)),
            album = null,
            thumbnailUrl = thumbnailUrl,
            durationMs = durationSeconds * 1000L,
            youtubeVideoId = videoId,
            source = TrackSource.YOUTUBE
        )
    }

    private fun getHighestQualityStream(formats: List<AdaptiveFormat>): AdaptiveFormat? {
        // Prioritize AAC (mp4 container: itags 141, 140, 139) for native media player compatibility (JavaFX Media / WMF / ExoPlayer)
        val preferredItags = listOf(
            InnerTubeConstants.ITAG_AAC_256, // 141 (AAC 256k)
            InnerTubeConstants.ITAG_AAC_128, // 140 (AAC 128k)
            139,                             // AAC 48k
            InnerTubeConstants.ITAG_OPUS_160,// 251 (Opus)
            InnerTubeConstants.ITAG_OPUS_70, // 250 (Opus)
            249                              // Opus
        )
        
        for (itag in preferredItags) {
            val format = formats.find { it.itag == itag }
            if (format?.url != null) {
                return format
            }
        }
        
        return formats.firstOrNull { it.url != null && it.mimeType?.contains("mp4") == true }
            ?: formats.firstOrNull { it.url != null }
    }

    
    private fun parseDurationToSeconds(duration: String): Int {
        val parts = duration.split(":")
        return when (parts.size) {
            2 -> {
                val (minutes, seconds) = parts
                (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
            }
            3 -> {
                val (hours, minutes, seconds) = parts
                (hours.toIntOrNull() ?: 0) * 3600 + (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0)
            }
            else -> 0
        }
    }
}
