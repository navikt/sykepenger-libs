package no.nav.sykepenger.libs.leaderelection

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

internal class TestElector : AutoCloseable {
    private val svar = LinkedBlockingQueue<Svar>()
    private val tilkoblet = CountDownLatch(1)
    private val executor = Executors.newVirtualThreadPerTaskExecutor()
    private val server =
        HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
            this.executor = this@TestElector.executor
            createContext("/sse") { exchange ->
                exchange.use {
                    tilkoblet.countDown()
                    val nesteSvar = checkNotNull(svar.poll(5, TimeUnit.SECONDS)) { "Mangler svar fra test-elector" }
                    exchange.responseHeaders.add("Content-Type", "text/event-stream")
                    exchange.sendResponseHeaders(nesteSvar.statuskode, 0)
                    exchange.responseBody.use { body ->
                        body.write(nesteSvar.innhold.toByteArray(Charsets.UTF_8))
                        body.flush()
                        nesteSvar.sendt.countDown()
                        nesteSvar.lukk.await()
                    }
                }
            }
            start()
        }

    val url = "http://localhost:${server.address.port}/sse"

    fun ventTilTilkoblet() {
        check(tilkoblet.await(5, TimeUnit.SECONDS)) { "Leader election koblet ikke til test-elector" }
    }

    fun send(
        innhold: String,
        statuskode: Int = 200,
        holdÅpen: Boolean = true,
    ): Svar = Svar(innhold, statuskode, holdÅpen).also(svar::add)

    override fun close() {
        server.stop(0)
        executor.shutdownNow()
        check(executor.awaitTermination(5, TimeUnit.SECONDS)) { "Test-elector stoppet ikke" }
    }

    class Svar(
        val innhold: String,
        val statuskode: Int,
        holdÅpen: Boolean,
    ) {
        val sendt = CountDownLatch(1)
        val lukk = CountDownLatch(if (holdÅpen) 1 else 0)

        fun ventTilSendt() {
            check(sendt.await(5, TimeUnit.SECONDS)) { "Test-elector sendte ikke svar" }
        }
    }
}
