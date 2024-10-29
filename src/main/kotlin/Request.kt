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
