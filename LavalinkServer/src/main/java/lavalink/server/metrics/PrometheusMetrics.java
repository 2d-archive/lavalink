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

package lavalink.server.metrics;

import ch.qos.logback.classic.LoggerContext;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.logback.InstrumentedAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.management.NotificationEmitter;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/**
 * Created by napster on 08.05.18.
 */
@Component
@ConditionalOnProperty("metrics.prometheus.enabled")
public class PrometheusMetrics {

  private static final Logger log = LoggerFactory.getLogger(PrometheusMetrics.class);

  public PrometheusMetrics() {

    InstrumentedAppender prometheusAppender = new InstrumentedAppender();
    //log metrics
    final LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
    final ch.qos.logback.classic.Logger root = factory.getLogger(Logger.ROOT_LOGGER_NAME);
    prometheusAppender.setContext(root.getLoggerContext());
    prometheusAppender.start();
    root.addAppender(prometheusAppender);

    //jvm (hotspot) metrics
    DefaultExports.initialize();

    //gc pause buckets
    final GcNotificationListener gcNotificationListener = new GcNotificationListener();
    for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
      if (gcBean instanceof NotificationEmitter) {
        ((NotificationEmitter) gcBean).addNotificationListener(gcNotificationListener, null, gcBean);
      }
    }

    log.info("Prometheus metrics set up");
  }
}
