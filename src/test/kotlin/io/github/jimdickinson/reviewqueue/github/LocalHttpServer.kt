package io.github.jimdickinson.reviewqueue.github

import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI

data class ReceivedRequest(val method: String, val path: String, val headers: Map<String, String>, val body: String)

class CannedResponse(
    val status: Int,
    val body: String = "",
    val headers: Map<String, String> = emptyMap(),
    val delayMillis: Long = 0,
)

/** A real HTTP server on a free local port that answers every request with [response] and records what it received. */
class LocalHttpServer(private val response: CannedResponse) : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    var received: ReceivedRequest? = null
        private set

    val graphQlUri: URI get() = URI("http://localhost:${server.address.port}/graphql")

    init {
        server.createContext("/") { exchange ->
            received = ReceivedRequest(
                method = exchange.requestMethod,
                path = exchange.requestURI.path,
                headers = exchange.requestHeaders.mapKeys { it.key.lowercase() }.mapValues { it.value.single() },
                body = exchange.requestBody.readBytes().decodeToString(),
            )
            Thread.sleep(response.delayMillis)
            response.headers.forEach { (name, value) -> exchange.responseHeaders.add(name, value) }
            val bytes = response.body.encodeToByteArray()
            exchange.sendResponseHeaders(response.status, if (bytes.isEmpty()) -1 else bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
    }

    override fun close() = server.stop(0)
}
