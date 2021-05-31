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

package lavalink.server.io

import lavalink.server.config.ServerConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.stereotype.Controller
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

@Controller
class HandshakeInterceptorImpl @Autowired
constructor(private val serverConfig: ServerConfig, private val socketServer: SocketServer) : HandshakeInterceptor {

  companion object {
    private val log = LoggerFactory.getLogger(HandshakeInterceptorImpl::class.java)
  }

  /**
   * Checks credentials and sets the Lavalink version header
   *
   * @return true if authenticated
   */
  override fun beforeHandshake(
    request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler,
    attributes: Map<String, Any>
  ): Boolean {
    val matches = if (serverConfig.password.isNullOrBlank()) {
      true
    } else {
      val password = request.headers.getFirst("Authorization")
      val matches = password == serverConfig.password

      if (matches) {
        log.info("Incoming connection from " + request.remoteAddress)
      } else {
        log.error("Authentication failed from " + request.remoteAddress)
        response.setStatusCode(HttpStatus.UNAUTHORIZED)
      }

      matches
    }

    /* no point in handling resuming if the password doesn't mach */
    if (matches) {
      val resumeKey = request.headers.getFirst("Resume-Key")
      val resuming = resumeKey != null && socketServer.canResume(resumeKey)
      response.headers.add("Session-Resumed", resuming.toString())
    }

    return matches
  }

  // No action required
  override fun afterHandshake(
    request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler,
    exception: Exception?
  ) {
  }
}
