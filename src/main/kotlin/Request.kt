import io.ktor.utils.io.*

enum class Method {
    GET
}

data class Request(
    val method: Method,
    val path: String,
) {
    companion object {
        suspend fun parse(reader: ByteReadChannel): Request {
            val requestLine = reader.readUTF8Line()
            val elems = requestLine!!.split("\\s+".toRegex())
            return Request(
                method = Method.valueOf(elems[0]),
                path = elems[1]
            )
        }

    }
}


suspend fun handleRequest(request: Request, writer: ByteWriteChannel) {
    if (request.path == "/") {
        writer.writeByteArray("HTTP/1.1 200 OK\r\n\r\n".toByteArray())
    } else if (request.path.startsWith("/echo/")) {
        val str = request.path.substringAfter("/echo/")
        writer.writeByteArray("HTTP/1.1 200 OK\r\n".toByteArray())
        writer.writeByteArray("Content-Type: text/plain\r\n".toByteArray())
        writer.writeByteArray("Content-Length: ${str.length}\r\n\r\n".toByteArray())
        writer.writeByteArray(str.toByteArray())
    } else {
        writer.writeByteArray("HTTP/1.1 404 Not Found\r\n\r\n".toByteArray())
    }
}
