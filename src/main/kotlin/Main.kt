import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val server = aSocket(selectorManager)
        .configure { reuseAddress = true }
        .tcp()
        .bind("127.0.0.1", 4221)

    server.accept().use {socket ->
        val writer = socket.openWriteChannel(autoFlush = true)
        writer.writeByteArray("HTTP/1.1 200 OK\r\n\r\n".toByteArray())
    }
}
