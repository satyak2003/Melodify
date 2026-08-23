package com.melodify.shared.di

import com.melodify.shared.api.innertube.InnerTubeApi
import com.melodify.shared.api.innertube.InnerTubeParser
import com.melodify.shared.api.lyrics.LyricsApi
import com.melodify.shared.api.spotify.SpotifyApi
import com.melodify.shared.api.deezer.DeezerApi
import com.melodify.shared.api.lastfm.LastFmApi
import com.melodify.shared.api.radio.FmRadioApi
import com.melodify.shared.data.MusicRepository
import com.melodify.shared.presentation.HomeViewModel
import com.melodify.shared.presentation.LibraryViewModel
import com.melodify.shared.presentation.PlayerViewModel
import com.melodify.shared.presentation.SearchViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Shared Koin module — platform-agnostic bindings.
 * Platform-specific bindings (AudioPlayer, DiscordRpc, DatabaseDriverFactory)
 * are declared in the platform-specific appModule / desktopModule.
 */
val sharedModule = module {
    // HTTP Client (shared by all APIs)
    single {
        HttpClient {
            expectSuccess = true
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 10000
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                    explicitNulls = false
                })
            }
            install(Logging) { level = LogLevel.NONE }
        }
    }

    // APIs
    single { InnerTubeApi(get()) }
    single { SpotifyApi(get()) }
    single { LyricsApi(get()) }
    single { DeezerApi(get()) }
    single { LastFmApi(get()) }
    single { FmRadioApi(get()) }

    // Parser (object — no need to inject, but listed for clarity)
    // InnerTubeParser is an object singleton

    // Repositories
    single { MusicRepository(get(), InnerTubeParser, get(), get(), get()) }

    // ViewModels
    viewModel { PlayerViewModel(get(), get(), get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { LibraryViewModel(get(), get()) }
}

