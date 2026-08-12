import java.io.File
import javax.imageio.ImageIO

val file = File("desktopApp/src/desktopMain/resources/icon.jpg")
if (!file.exists()) {
    println("File does not exist")
} else {
    try {
        val image = ImageIO.read(file)
        if (image != null) {
            println("Image read successfully: ${image.width}x${image.height}")
        } else {
            println("ImageIO returned null")
        }
    } catch (e: Exception) {
        println("Exception: ${e.message}")
    }
}
