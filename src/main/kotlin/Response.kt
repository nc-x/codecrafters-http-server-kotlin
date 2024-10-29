import Status.Status200
import Status.Status201
import Status.Status400
import Status.Status404
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

val SUPPORTED_ENCODINGS = setOf("gzip")

enum class Status(val msg: String) {
    Status200("200 OK"),
    Status201("201 Created"),
    Status400("400 Bad Request"),
    Status404("404 Not Found"),
}

sealed class Response(val status: String, val body: ByteArray?) {
    abstract val type: String
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

suspend fun respond200(writer: ByteWriteChannel, request: Request) {
    respond(writer, request, Empty(Status200))
}

suspend fun respond201(writer: ByteWriteChannel, request: Request) {
    respond(writer, request, Empty(Status201))
}

suspend fun respond400(writer: ByteWriteChannel, request: Request) {
    respond(writer, request, Empty(Status400))
}

suspend fun respond404(writer: ByteWriteChannel, request: Request) {
    respond(writer, request, Empty(Status404))
}

suspend fun respond(writer: ByteWriteChannel, request: Request, response: Response) {
    writer.writeByteArray("HTTP/1.1 ${response.status}\r\n".toByteArray())
    if (response.body == null) {
        writer.writeByteArray("\r\n".toByteArray())
        return
    }
    val body =
        request.headers["Accept-Encoding"]?.let { compressIfPossible(writer, it, response.body) } ?: response.body
    writer.writeByteArray("Content-Type: ${response.type}\r\n".toByteArray())
    writer.writeByteArray("Content-Length: ${body.size}\r\n\r\n".toByteArray())
    writer.writeByteArray(body)
}

suspend fun compressIfPossible(writer: ByteWriteChannel, clientEncodings: String, body: ByteArray): ByteArray? {
    val availableEncodings = clientEncodings.split(",").map(String::trim).filter { it in SUPPORTED_ENCODINGS }
    if ("gzip" !in availableEncodings) return null
    writer.writeByteArray("Content-Encoding: gzip\r\n".toByteArray())
    val gzipped = ByteArrayOutputStream()
    withContext(Dispatchers.IO) {
        GZIPOutputStream(gzipped).use { it.write(body) }
    }
    return gzipped.toByteArray()
}
