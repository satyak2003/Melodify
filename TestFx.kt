import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun main() {
    JFXPanel() // Init JavaFX
    val latch = CountDownLatch(1)

    // Test ITAG 140 URL with &file=audio.m4a appended
    val url = "https://rr2---sn-gwpa-caged.googlevideo.com/videoplayback?expire=17852...&file=audio.m4a"
    println("Testing JavaFX Media with URL...")
    
    try {
        val media = Media(url)
        val player = MediaPlayer(media)
        player.setOnReady {
            println("SUCCESS! JavaFX Media Ready! Duration: \")
            latch.countDown()
        }
        player.setOnError {
            println("ERROR: \")
            latch.countDown()
        }
    } catch (e: Exception) {
        println("EXCEPTION: \")
        latch.countDown()
    }
    
    latch.await(10, TimeUnit.SECONDS)
}
