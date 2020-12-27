package lavalink.server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "lavalink.server.lavaplayer")
@Component
class LavaplayerConfigProperties {
  var youtubePlaylistLoadLimit: Int? = null
  var isGcWarnings = true
  var isYoutubeSearchEnabled = true
  var isSoundcloudSearchEnabled = true
  var ratelimit: RateLimitConfig? = null
  var nonAllocating: Boolean = false
  var frameBufferDuration: Int = 5000
}