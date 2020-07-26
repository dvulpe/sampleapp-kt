package sampleapp

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.MetricFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Undertow
import org.http4k.server.asServer
import java.util.*

fun main() {
    val rnd = Random(System.currentTimeMillis())
    val successRate = System.getenv("SUCCESS_RATE")?.toInt() ?: 100
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val router = routes(
        "/" bind GET to {
            if (rnd.nextInt(100) > successRate)
                Response(Status.SERVICE_UNAVAILABLE).body("fail")
            else
                Response(OK).body("OK")
        }
    )
    val app = metricsTemplate.RequestCounter(registry, "http.requests.total")
        .then(router)
    startMetricsServer(registry)
    println("Server starting")

    val server = app.asServer(Undertow(8080))
    server.start()
    server.block()
}

val metricsTemplate = MetricFilters.FiltersTemplate(
    "http.server.request.latency" to "Timing of server requests",
    "http.requests.total" to "Total number of server requests"
) {
    it.copy(
        labels = mapOf(
            "code" to it.response.status.code.toString()
        )
    )
}

fun startMetricsServer(registry: PrometheusMeterRegistry) {
    val router = routes(
        "/liveness" bind GET to { Response(OK) },
        "/readiness" bind GET to { Response(OK) },
        "/metrics" bind GET to { Response(OK).body(registry.scrape()) }
    )
    println("Starting metrics server")
    router.asServer(Undertow(8000)).start()
}
