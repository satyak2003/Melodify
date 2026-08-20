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
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.jpg")
        if (stream != null) {
            BitmapPainter(ImageIO.read(stream).toComposeImageBitmap())
        } else null
    }.getOrNull()

    val windowState = rememberWindowState(width = 1280.dp, height = 820.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Melodify",
        icon = iconPainter,
        state = windowState,
        undecorated = true,
        transparent = false
    ) {
        val isMaximized = windowState.placement == WindowPlacement.Maximized
        MelodifyDesktopApp(
            isMaximized = isMaximized,
            onClose = ::exitApplication,
            onMinimize = { windowState.isMinimized = true },
            onMaximize = {
                windowState.placement = if (isMaximized) {
                    WindowPlacement.Floating
                } else {
                    WindowPlacement.Maximized
                }
            }
        )
    }
}



