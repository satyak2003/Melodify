package com.melodify.shared.api.innertube

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class InnerTubeContext(
    val client: InnerTubeClient
)

@Serializable
data class InnerTubeClient(
    val clientName: String,
    val clientVersion: String,
    val gl: String? = null,
    val hl: String? = null,
    val visitorData: String? = null,
    val androidSdkVersion: Int? = null
)

@Serializable
data class InnerTubeRequest(
    val context: InnerTubeContext,
    val query: String? = null,
    val videoId: String? = null,
    val browseId: String? = null,
    val playlistId: String? = null
)

// --- Player Response Models ---
@Serializable
data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus? = null,
    val streamingData: StreamingData? = null,
    val videoDetails: VideoDetails? = null
)

@Serializable
data class PlayabilityStatus(
    val status: String? = null
)

@Serializable
data class StreamingData(
    val adaptiveFormats: List<AdaptiveFormat>? = null,
    val formats: List<AdaptiveFormat>? = null
)

@Serializable
data class AdaptiveFormat(
    val itag: Int? = null,
    val url: String? = null,
    val signatureCipher: String? = null,
    val mimeType: String? = null,
    val bitrate: Int? = null,
    val contentLength: String? = null
)

@Serializable
data class VideoDetails(
    val videoId: String? = null,
    val title: String? = null,
    val author: String? = null,
    val lengthSeconds: String? = null,
    val thumbnail: Thumbnails? = null
)

@Serializable
data class Thumbnails(
    val thumbnails: List<ThumbnailItem>? = null
)

@Serializable
data class ThumbnailItem(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

// --- Search / Browse Response Models ---
@Serializable
data class SearchResponse(
    val contents: Contents? = null
)

@Serializable
data class BrowseResponse(
    val contents: Contents? = null
)

@Serializable
data class NextResponse(
    val contents: Contents? = null
)

@Serializable
data class Contents(
    val tabbedSearchResultsRenderer: TabbedSearchResultsRenderer? = null,
    val twoColumnSearchResultsRenderer: TwoColumnSearchResultsRenderer? = null,
    val singleColumnBrowseResultsRenderer: SingleColumnBrowseResultsRenderer? = null,
    val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer? = null
)

@Serializable
data class TabbedSearchResultsRenderer(
    val tabs: List<Tab>? = null
)

@Serializable
data class TwoColumnSearchResultsRenderer(
    val primaryContents: PrimaryContents? = null
)

@Serializable
data class SingleColumnBrowseResultsRenderer(
    val tabs: List<Tab>? = null
)

@Serializable
data class SingleColumnMusicWatchNextResultsRenderer(
    val tabbedRenderer: TabbedRenderer? = null
)

@Serializable
data class TabbedRenderer(
    val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer? = null
)

@Serializable
data class WatchNextTabbedResultsRenderer(
    val tabs: List<Tab>? = null
)

@Serializable
data class Tab(
    val tabRenderer: TabRenderer? = null
)

@Serializable
data class TabRenderer(
    val content: TabContent? = null
)

@Serializable
data class PrimaryContents(
    val sectionListRenderer: SectionListRenderer? = null
)

@Serializable
data class TabContent(
    val sectionListRenderer: SectionListRenderer? = null,
    val musicQueueRenderer: MusicQueueRenderer? = null
)

@Serializable
data class MusicQueueRenderer(
    val content: MusicQueueContent? = null
)

@Serializable
data class MusicQueueContent(
    val playlistPanelRenderer: PlaylistPanelRenderer? = null
)

@Serializable
data class PlaylistPanelRenderer(
    val contents: List<PlaylistPanelContent>? = null
)

@Serializable
data class PlaylistPanelContent(
    val playlistPanelVideoRenderer: MusicResponsiveListItemRenderer? = null
)

@Serializable
data class SectionListRenderer(
    val contents: List<SectionListContent>? = null
)

@Serializable
data class SectionListContent(
    val musicShelfRenderer: MusicShelfRenderer? = null,
    val musicCardShelfRenderer: MusicCardShelfRenderer? = null,
    val itemSectionRenderer: ItemSectionRenderer? = null
)

@Serializable
data class ItemSectionRenderer(
    val contents: List<ItemSectionContent>? = null
)

@Serializable
data class ItemSectionContent(
    val musicShelfRenderer: MusicShelfRenderer? = null,
    val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null
)

@Serializable
data class MusicShelfRenderer(
    val title: Runs? = null,
    val contents: List<MusicShelfContent>? = null
)

@Serializable
data class MusicCardShelfRenderer(
    val title: Runs? = null,
    val subtitle: Runs? = null,
    val titleEndpoint: NavigationEndpoint? = null
)

@Serializable
data class MusicShelfContent(
    val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null
)

@Serializable
data class MusicResponsiveListItemRenderer(
    val flexColumns: List<FlexColumn>? = null,
    val fixedColumns: List<FixedColumn>? = null,
    val thumbnail: MusicThumbnailRendererContainer? = null,
    val playlistItemData: PlaylistItemData? = null
)

@Serializable
data class PlaylistItemData(
    val videoId: String? = null
)

@Serializable
data class MusicThumbnailRendererContainer(
    val musicThumbnailRenderer: MusicThumbnailRenderer? = null
)

@Serializable
data class MusicThumbnailRenderer(
    val thumbnail: Thumbnails? = null
)

@Serializable
data class FlexColumn(
    val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer? = null
)

@Serializable
data class FixedColumn(
    val musicResponsiveListItemFixedColumnRenderer: MusicResponsiveListItemFixedColumnRenderer? = null
)

@Serializable
data class MusicResponsiveListItemFlexColumnRenderer(
    val text: Runs? = null
)

@Serializable
data class MusicResponsiveListItemFixedColumnRenderer(
    val text: Runs? = null
)

@Serializable
data class Runs(
    val runs: List<Run>? = null
)

@Serializable
data class Run(
    val text: String? = null,
    val navigationEndpoint: NavigationEndpoint? = null
)

@Serializable
data class NavigationEndpoint(
    val watchEndpoint: WatchEndpoint? = null,
    val browseEndpoint: BrowseEndpoint? = null
)

@Serializable
data class WatchEndpoint(
    val videoId: String? = null,
    val playlistId: String? = null
)

@Serializable
data class BrowseEndpoint(
    val browseId: String? = null
)
