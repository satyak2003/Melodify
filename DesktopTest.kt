import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery

fun main() {
    val discovery = NativeDiscovery()
    val discovered = discovery.discover()
    println("VLC Discovered: \")
    if (discovered) {
        println("VLC Path: \")
    }
}
