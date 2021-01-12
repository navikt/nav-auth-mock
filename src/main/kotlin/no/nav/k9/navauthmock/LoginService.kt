package no.nav.k9.navauthmock

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.httpMethod
import io.ktor.request.uri
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.util.getOrFail
import no.nav.helse.dusseldorf.testsupport.http.LoginRequest
import no.nav.helse.dusseldorf.testsupport.http.LoginServiceLogin
import no.nav.helse.dusseldorf.testsupport.http.LoginServiceWellKnown
import no.nav.helse.dusseldorf.testsupport.jws.LoginService
import org.slf4j.LoggerFactory
import java.net.URI

private val logger = LoggerFactory.getLogger("no.nav.k9.navauthmock.LoginService")


private object LoginServiceKonstanter {
    const val basePath = "/login-service/v1.0"
    const val wellKnownPath = "$basePath/.well-known/openid-configuration"
    const val loginPath = "$basePath/login"
    const val jwksPath = "$basePath/jwks"
}

internal fun Routing.loginService() {
    get(LoginServiceKonstanter.loginPath) {
        logger.info("${call.request.httpMethod.value}@${call.request.uri}")
        val loginRequest = KtorLoginRequest(call.request)
        val loginResponse = LoginServiceLogin.login(loginRequest)

        call.response.cookies.append(Cookie(
                name = loginResponse.cookie.name,
                value = loginResponse.cookie.value,
                domain = loginResponse.cookie.domain,
                path = loginResponse.cookie.path,
                secure = loginResponse.cookie.secure,
                httpOnly = loginResponse.cookie.httpOnly,
                encoding = CookieEncoding.RAW
        ))

        call.respondRedirect(loginResponse.location.toString(), false)
    }

    get(LoginServiceKonstanter.jwksPath) {
        logger.info("${call.request.httpMethod.value}@${call.request.uri}")
        call.respondText(
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
                text = LoginService.V1_0.getPublicJwk()
        )
    }

    get(LoginServiceKonstanter.wellKnownPath) {
        logger.info("${call.request.httpMethod.value}@${call.request.uri}")
        val baseUrl = call.request.baseUrl()
        val wellKnownResponse = LoginServiceWellKnown.response(
                issuer = "$baseUrl${LoginServiceKonstanter.basePath}",
                jwksUri = "$baseUrl${LoginServiceKonstanter.jwksPath}"
        )
        call.respondText(
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.OK,
                text = wellKnownResponse
        )
    }
}

private data class KtorLoginRequest(
        val request: ApplicationRequest
) : LoginRequest {
    override fun cookieName(): String? = request.queryParameters["cookieName"]
    override fun fnr(): String?  = request.queryParameters["fnr"]
    override fun level(): Int? = request.queryParameters["level"]?.toInt()
    override fun redirect(): URI = URI.create(request.queryParameters.getOrFail("redirect"))
}