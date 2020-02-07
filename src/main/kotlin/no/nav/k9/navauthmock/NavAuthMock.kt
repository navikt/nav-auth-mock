package no.nav.k9.navauthmock

import io.ktor.application.*
import io.ktor.features.origin
import io.ktor.request.*
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
fun Application.NavAuthMock() {
    install(Routing) {
        loginService()
        naisSts()
    }
}

internal fun ApplicationRequest.baseUrl() = "${origin.scheme}://${host()}:${port()}"