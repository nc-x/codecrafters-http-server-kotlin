import io.ktor.utils.io.*
import java.io.File

enum class Method {
    GET
}

data class Request(
    val method: Method,
    val path: String,
    val httpVersion: String,
    val headers: Map<String, String>
) {
    companion object {
        suspend fun parse(reader: ByteReadChannel): Request {
            val requestLine = reader.readUTF8Line()
            val info = requestLine!!.split("\\s+".toRegex())

            val headers = mutableMapOf<String, String>()
            var headerLine: String
            while (true) {
                headerLine = reader.readUTF8Line()!!
                if (headerLine.isEmpty()) break
                val (header, value) = headerLine.split(":")
                headers[header.trim()] = value.trim()
            }

            return Request(
                method = Method.valueOf(info[0]),
                path = info[1],
                httpVersion = info[2],
                headers = headers
            )
        }

    }
}

suspend fun handleRequest(request: Request, writer: ByteWriteChannel, directory: String?) {
    val path = request.path
    when {
        path == "/" -> {
            respond200(writer, null)
        }

        path == "/user-agent" -> {
            respond200(writer, request.headers["User-Agent"]!!)
        }

        path.startsWith("/echo/") -> {
            respond200(writer, path.substringAfter("/echo/"))
        }

        path.startsWith("/files/") -> {
            if (directory == null) return respond404(writer)
            val fileName = path.substringAfter("/files/")
            val file = File("$directory/$fileName")
            if (!file.exists()) return respond404(writer)
            respond200(writer, file.readText(), "application/octet-stream")
        }


        else -> {
            respond404(writer)
        }
    }
}

suspend fun respond200(writer: ByteWriteChannel, message: String?, contentType: String = "text/plain") {
    writer.writeByteArray("HTTP/1.1 200 OK\r\n".toByteArray())
    if (message == null) {
        writer.writeByteArray("\r\n".toByteArray())
        return
    }
    writer.writeByteArray("Content-Type: ${contentType}\r\n".toByteArray())
    writer.writeByteArray("Content-Length: ${message.length}\r\n\r\n".toByteArray())
    writer.writeByteArray(message.toByteArray())
}

suspend fun respond404(writer: ByteWriteChannel) {
    writer.writeByteArray("HTTP/1.1 404 Not Found\r\n\r\n".toByteArray())
}
