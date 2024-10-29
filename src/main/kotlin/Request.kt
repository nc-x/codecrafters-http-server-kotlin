import Status.Status200
import io.ktor.utils.io.*
import java.io.File

enum class Method {
    GET,
    POST,
}

data class Request(
    val method: Method,
    val path: String,
    val httpVersion: String,
    val headers: Map<String, String>,
    val body: ByteArray?,
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

            var body: ByteArray? = null
            headers["Content-Length"]?.let {
                body = reader.readByteArray(it.toInt())
            }

            return Request(
                method = Method.valueOf(info[0]),
                path = info[1],
                httpVersion = info[2],
                headers = headers,
                body = body,
            )
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Request

        if (method != other.method) return false
        if (path != other.path) return false
        if (httpVersion != other.httpVersion) return false
        if (headers != other.headers) return false
        if (body != null) {
            if (other.body == null) return false
            if (!body.contentEquals(other.body)) return false
        } else if (other.body != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + httpVersion.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }
}

suspend fun handleRequest(request: Request, writer: ByteWriteChannel, directory: String?) {
    val path = request.path
    val method = request.method
    when {
        path == "/" -> {
            respond200(writer)
        }

        path == "/user-agent" -> {
            respond(writer, PlainText(Status200, request.headers["User-Agent"]!!))
        }

        path.startsWith("/echo/") -> {
            respond(writer, PlainText(Status200, path.substringAfter("/echo/")))
        }

        method == Method.GET && path.startsWith("/files/") -> {
            if (directory == null) return respond404(writer)
            val fileName = path.substringAfter("/files/")
            val file = File("$directory/$fileName")
            if (!file.exists()) return respond404(writer)
            respond200(writer)
            respond(writer, Bytes(Status200, file.readBytes()))
        }

        method == Method.POST && path.startsWith("/files/") -> {
            if (directory == null) return respond404(writer)
            val fileName = path.substringAfter("/files/")
            val file = File("$directory/$fileName")
            if (request.body == null) return respond400(writer)
            file.writeBytes(request.body)
            respond201(writer)
        }

        else -> {
            respond404(writer)
        }
    }
}
