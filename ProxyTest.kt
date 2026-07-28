import java.net.ServerSocket
import java.net.URL
import java.net.HttpURLConnection
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun main() {
    val targetUrl = ""
    val server = ServerSocket(8089)
    println("Local Proxy started on port 8089...")

    thread {
        try {
            val client = server.accept()
            println("Proxy received connection from JavaFX Media!")
            val targetConn = URL(targetUrl).openConnection() as HttpURLConnection
            targetConn.requestMethod = "GET"
            targetConn.connect()

            val out = client.getOutputStream()
            val header = "HTTP/1.1 200 OK\r\nContent-Type: audio/mp4\r\nContent-Length: \\r\n\r\n"
            out.write(header.toByteArray())

            targetConn.inputStream.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    out.write(buffer, 0, bytesRead)
                }
            }
            out.flush()
            client.close()
        } catch (e: Exception) {
            println("Proxy error: \")
        }
    }

    JFXPanel() // Init JavaFX
    val latch = CountDownLatch(1)

    try {
        val mediaUrl = "http://127.0.0.1:8089/stream.m4a"
        println("Loading JavaFX Media URL: \")
        val media = Media(mediaUrl)
        val player = MediaPlayer(media)

        player.setOnReady {
            println("SUCCESS!!! JavaFX Media Ready! Duration: \")
            latch.countDown()
        }

        player.setOnError {
            println("ERROR: \")
            latch.countDown()
        }

        player.play()
    } catch (e: Exception) {
        println("EXCEPTION: \")
        latch.countDown()
    }

    latch.await(10, TimeUnit.SECONDS)
    server.close()
}
