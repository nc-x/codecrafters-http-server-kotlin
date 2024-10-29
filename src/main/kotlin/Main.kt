import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val server = aSocket(selectorManager)
        .configure { reuseAddress = true }
        .tcp()
        .bind("127.0.0.1", 4221)

    while (true) {
        val socket = server.accept()
        launch {
            socket.use { socket ->
                val reader = socket.openReadChannel()
                val request = Request.parse(reader)
                val writer = socket.openWriteChannel(autoFlush = true)
                handleRequest(request, writer)
            }
        }
    }
}
