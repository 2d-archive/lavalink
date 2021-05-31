/*
 *  Copyright (c) 2021 Freya Arbjerg and contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */

package lavalink.server.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class RequestAuthorizationFilter(
  private val serverConfig: ServerConfig,
  private val metricsConfig: MetricsPrometheusConfigProperties
) : HandlerInterceptor, WebMvcConfigurer {

  companion object {
    private val log = LoggerFactory.getLogger(RequestAuthorizationFilter::class.java)
  }

  init {
    if (serverConfig.password.isNullOrBlank()) {
      log.warn("No configured password, this is a possible security risk.")
    }
  }

  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    return when {
      // collecting metrics is anonymous
      metricsConfig.endpoint.isNotBlank() && request.servletPath == metricsConfig.endpoint ->
        true

      request.servletPath == "/error" ->
        true

      serverConfig.password.isNullOrBlank() ->
        true

      else -> {
        val authorization = request.getHeader("Authorization")
        if (authorization?.takeUnless { it == serverConfig.password } == null) {
          /* check if the authorization is missing or just incorrect. */
          val missing = authorization == null

          /* log the failed request heheh */
          log.warn(buildString {
            append("Authorization ${if (missing) "missing" else "failed"} for ${request.remoteAddr} on ${request.method}")
            append(" ")
            append(request.requestURI.substring(request.contextPath.length))
          })

          /* set the response status */
          response.status = (if (missing) HttpStatus.UNAUTHORIZED else HttpStatus.FORBIDDEN).value()

          /* yessir */
          return false
        }

        return true
      }
    }
  }

  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(this)
  }
}
