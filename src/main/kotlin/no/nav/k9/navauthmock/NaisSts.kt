package no.nav.k9.navauthmock

import io.ktor.application.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.helse.dusseldorf.testsupport.http.NaisStsToken
import no.nav.helse.dusseldorf.testsupport.http.NaisStsTokenRequest
import no.nav.helse.dusseldorf.testsupport.http.NaisStsWellKnown
import no.nav.helse.dusseldorf.testsupport.jws.NaisSts
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("no.nav.k9.navauthmock.NavSts")

private object NavStsKonstanter {
    const val basePath = "/nais-sts"
    const val wellKnownPath = "$basePath/.well-known/openid-configuration"
    const val tokenPath = "$basePath/token"
    const val jwksPath = "$basePath/jwks"
}

internal fun Routing.naisSts() {
    val tokenBody: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
        logger.info("${call.request.httpMethod.value}@${call.request.uri}")
        val baseUrl = call.request.baseUrl()

        val tokenResponse = NaisStsToken.response(
                request = KtorNaisStsTokenReqeust(call.request),
                issuer = "$baseUrl${NavStsKonstanter.basePath}"
        )

        call.respondText(
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
                text = tokenResponse
        )
    }
    get(NavStsKonstanter.tokenPath, tokenBody)

    post (NavStsKonstanter.tokenPath, tokenBody)

    get(NavStsKonstanter.jwksPath) {
        logger.info("${call.request.httpMethod.value}@${call.request.uri}")
        call.respondText(
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
                text = NaisSts.getPublicJwk()
        )
    }

    get(NavStsKonstanter.wellKnownPath) {
        logger.info("${call.request.httpMethod.value}@${call.request.uri}")
        val baseUrl = call.request.baseUrl()
        val wellKnownResponse = NaisStsWellKnown.response(
                issuer = "$baseUrl${NavStsKonstanter.basePath}",
                tokenEndpoint = "$baseUrl${NavStsKonstanter.tokenPath}",
                jwksUri = "$baseUrl${NavStsKonstanter.jwksPath}"
        )
        call.respondText(
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
                text = wellKnownResponse
        )
    }
}

private data class KtorNaisStsTokenReqeust(
        val request: ApplicationRequest
) : NaisStsTokenRequest {
    override fun authorizationHeader(): String = request.header(HttpHeaders.Authorization)!!
}
