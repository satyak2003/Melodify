package com.melodify.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.melodify.desktop.di.desktopModule
import com.melodify.desktop.ui.MelodifyDesktopApp
import org.koin.core.context.startKoin
import javax.imageio.ImageIO

fun main() = application {
    startKoin {
        modules(desktopModule)
    }

    val iconPainter = runCatching {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")
        if (stream != null) {
            BitmapPainter(ImageIO.read(stream).toComposeImageBitmap())
        } else null
    }.getOrNull()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Melodify",
        icon = iconPainter,
        state = rememberWindowState(width = 1280.dp, height = 820.dp)
    ) {
        MelodifyDesktopApp()
    }
}



