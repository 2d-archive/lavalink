package lavalink.server.info;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by napster on 08.03.19.
 * - Edited by melike2d on 01.13.21
 */
@RestController
class InfoRestHandler(private val appInfo: AppInfo) {
  @GetMapping("/version")
  fun version(): String {
    return appInfo.versionBuild;
  }
}
