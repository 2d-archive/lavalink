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

package lavalink.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class RequestAuthorizationFilter implements HandlerInterceptor, WebMvcConfigurer {

  private static final Logger log = LoggerFactory.getLogger(RequestAuthorizationFilter.class);
  private final ServerConfig serverConfig;
  private final MetricsPrometheusConfigProperties metricsConfig;

  public RequestAuthorizationFilter(ServerConfig serverConfig, MetricsPrometheusConfigProperties metricsConfig) {
    this.serverConfig = serverConfig;
    this.metricsConfig = metricsConfig;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    // Collecting metrics is anonymous
    if (!metricsConfig.getEndpoint().isEmpty()
      && request.getServletPath().equals(metricsConfig.getEndpoint())) return true;

    if (request.getServletPath().equals("/error")) return true;

    String authorization = request.getHeader("Authorization");

    if (authorization == null || !authorization.equals(serverConfig.getPassword())) {
      String method = request.getMethod();
      String path = request.getRequestURI().substring(request.getContextPath().length());
      String ip = request.getRemoteAddr();

      if (authorization == null) {
        log.warn("Authorization missing for {} on {} {}", ip, method, path);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        return false;
      }
      log.warn("Authorization failed for {} on {} {}", ip, method, path);
      response.setStatus(HttpStatus.FORBIDDEN.value());
      return false;
    }

    return true;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this);
  }
}
