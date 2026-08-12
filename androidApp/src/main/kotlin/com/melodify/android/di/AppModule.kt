package com.melodify.android.di

import com.melodify.shared.api.innertube.InnerTubeApi
import com.melodify.shared.api.innertube.InnerTubeParser
import com.melodify.shared.api.lyrics.LyricsApi
import com.melodify.shared.api.spotify.SpotifyApi
import com.melodify.shared.data.MusicRepository
import com.melodify.shared.domain.discord.DiscordRpc
import com.melodify.shared.domain.player.AudioPlayer
import com.melodify.shared.presentation.HomeViewModel
import com.melodify.shared.presentation.LibraryViewModel
import com.melodify.shared.presentation.PlayerViewModel
import com.melodify.shared.presentation.SearchViewModel

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Android-specific AudioPlayer using ExoPlayer
    single { AudioPlayer(androidContext()) }
    single { DiscordRpc() }
    
    // HTTP client

    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(Logging) { level = LogLevel.INFO }
            defaultRequest {
                header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Pixel 5) AppleWebKit/537.36")
            }
        }
    }
    
    // APIs
    single { InnerTubeApi(get()) }
    single { SpotifyApi(get()) }
    single { LyricsApi(get()) }
    
    // Repository
    single { MusicRepository(get(), InnerTubeParser) }
    
    // ViewModels
    viewModel { PlayerViewModel(get(), get(), get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { LibraryViewModel(get(), get()) }
}
