import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers

suspend fun main() {
    val selectorManager = SelectorManager(Dispatchers.IO)
    val server = aSocket(selectorManager)
        .configure { reuseAddress = true }
        .tcp()
        .bind("127.0.0.1", 4221)

    server.accept()
    println("accepted new connection")
}
