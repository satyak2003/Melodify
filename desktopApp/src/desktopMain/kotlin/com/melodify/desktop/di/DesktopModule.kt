package com.melodify.desktop.di

import com.melodify.shared.db.DatabaseDriverFactory
import com.melodify.shared.di.sharedModule
import com.melodify.shared.domain.discord.DiscordRpc
import com.melodify.shared.domain.player.AudioPlayer
import org.koin.dsl.module

val desktopModule = module {
    includes(sharedModule)
    
    single { DatabaseDriverFactory() }
    single { AudioPlayer() }
    single { DiscordRpc() }
}
