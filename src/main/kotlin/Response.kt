import Status.Status200
import Status.Status201
import Status.Status400
import Status.Status404
import io.ktor.utils.io.*

enum class Status(val msg: String) {
    Status200("200 OK"),
    Status201("201 Created"),
    Status400("400 Bad Request"),
    Status404("404 Not Found"),
}

sealed class Response(val status: String, val body: ByteArray?) {
    abstract val type: String
    val length: Int
        get() = body?.size ?: 0
}

class PlainText(status: Status, body: String) : Response(status.msg, body.toByteArray()) {
    override val type: String
        get() = "text/plain"
}

class Bytes(status: Status, body: ByteArray) : Response(status.msg, body) {
    override val type: String
        get() = "application/octet-stream"
}

class Empty(status: Status) : Response(status.msg, null) {
    override val type: String
        get() = error("This should be unreachable!!!")
}

suspend fun respond200(writer: ByteWriteChannel) {
    respond(writer, Empty(Status200))
}

suspend fun respond201(writer: ByteWriteChannel) {
    respond(writer, Empty(Status201))
}

suspend fun respond400(writer: ByteWriteChannel) {
    respond(writer, Empty(Status400))
}

suspend fun respond404(writer: ByteWriteChannel) {
    respond(writer, Empty(Status404))
}

suspend fun respond(writer: ByteWriteChannel, response: Response) {
    writer.writeByteArray("HTTP/1.1 ${response.status}\r\n".toByteArray())
    if (response.body == null) {
        writer.writeByteArray("\r\n".toByteArray())
        return
    }
    writer.writeByteArray("Content-Type: ${response.type}\r\n".toByteArray())
    writer.writeByteArray("Content-Length: ${response.length}\r\n\r\n".toByteArray())
    writer.writeByteArray(response.body)
}
