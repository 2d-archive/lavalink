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

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import io.prometheus.client.Collector;
import io.prometheus.client.Histogram;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import static com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION;
import static com.sun.management.GarbageCollectionNotificationInfo.from;

/**
 * Created by napster on 21.05.18.
 * <p>
 * General idea taken from {@link com.sedmelluq.discord.lavaplayer.tools.GarbageCollectionMonitor}, thanks!
 */
public class GcNotificationListener implements NotificationListener {

  private final Histogram gcPauses = Histogram.build()
    .name("lavalink_gc_pauses_seconds")
    .help("Garbage collection pauses by buckets")
    .buckets(0.025, 0.050, 0.100, 0.200, 0.400, 0.800, 1.600)
    .register();

  @Override
  public void handleNotification(Notification notification, Object handback) {
    if (GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
      GarbageCollectionNotificationInfo notificationInfo = from((CompositeData) notification.getUserData());
      GcInfo info = notificationInfo.getGcInfo();

      if (info != null && !"No GC".equals(notificationInfo.getGcCause())) {
        gcPauses.observe(info.getDuration() / Collector.MILLISECONDS_PER_SECOND);
      }
    }
  }
}
