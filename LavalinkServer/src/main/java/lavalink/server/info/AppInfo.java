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

package lavalink.server.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by napster on 25.06.18.
 * <p>
 * Requires app.properties to be populated with values during the gradle build
 */
@Component
public class AppInfo {

  private static final Logger log = LoggerFactory.getLogger(AppInfo.class);

  private final String version;
  private final String groupId;
  private final String artifactId;
  private final String buildNumber;
  private final long buildTime;

  public AppInfo() {
    InputStream resourceAsStream = this.getClass().getResourceAsStream("/app.properties");
    Properties prop = new Properties();
    try {
      prop.load(resourceAsStream);
    } catch (IOException e) {
      log.error("Failed to load app.properties", e);
    }
    this.version = prop.getProperty("version");
    this.groupId = prop.getProperty("groupId");
    this.artifactId = prop.getProperty("artifactId");
    this.buildNumber = prop.getProperty("buildNumber");
    long bTime = -1L;
    try {
      bTime = Long.parseLong(prop.getProperty("buildTime"));
    } catch (NumberFormatException ignored) {
    }
    this.buildTime = bTime;
  }

  public String getVersion() {
    return this.version;
  }

  public String getGroupId() {
    return this.groupId;
  }

  public String getArtifactId() {
    return this.artifactId;
  }

  public String getBuildNumber() {
    return this.buildNumber;
  }

  public long getBuildTime() {
    return this.buildTime;
  }

  public String getVersionBuild() {
    return this.version + "_" + this.buildNumber;
  }
}
