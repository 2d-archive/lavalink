package lavalink.server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "lavalink.server.koe")
@Component
class KoeConfigProperties {
  var useEpoll: Boolean = true
  var highPacketPriority: Boolean = true
  var bufferDurationMs: Int? = null
  var byteBufAllocator: String = "pooled"
}