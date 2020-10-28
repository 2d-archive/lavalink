package lavalink.server.config

import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollDatagramChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollSocketChannel
import moe.kyokobot.koe.KoeOptions
import moe.kyokobot.koe.codec.udpqueue.UdpQueueFramePollerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KoeConfiguration(val configProperties: KoeConfigProperties) {

  private val log: Logger = LoggerFactory.getLogger(KoeConfiguration::class.java)

  @Bean
  fun koeOptions(): KoeOptions = KoeOptions.builder().apply {
    log.info("OS: " + System.getProperty("os.name") + ", Arch: " + System.getProperty("os.arch"))
    val os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")

    setHighPacketPriority(configProperties.highPacketPriority)

    /* JDA-NAS */
    // Maybe add Windows natives back?
    val nasSupported = os.contains("linux", ignoreCase = true)
      && arch.equals("amd64", ignoreCase = true)

    if (nasSupported) {
      log.info("Enabling JDA-NAS")

      var bufferSize = configProperties.bufferDurationMs ?: UdpQueueFramePollerFactory.DEFAULT_BUFFER_DURATION
      if (bufferSize <= 0) {
        log.warn("Buffer size of ${bufferSize}ms is illegal. Defaulting to ${UdpQueueFramePollerFactory.DEFAULT_BUFFER_DURATION}")
        bufferSize = UdpQueueFramePollerFactory.DEFAULT_BUFFER_DURATION
      }

      setFramePollerFactory(UdpQueueFramePollerFactory(bufferSize, Runtime.getRuntime().availableProcessors()))
    } else {
      log.warn("This system and architecture appears to not support native audio sending! "
        + "GC pauses may cause your bot to stutter during playback.")
    }

    /* Epoll Transport */
    if (configProperties.useEpoll && Epoll.isAvailable()) {
      log.info("Using Epoll Transport.")
      setEventLoopGroup(EpollEventLoopGroup())
      setDatagramChannelClass(EpollDatagramChannel::class.java)
      setSocketChannelClass(EpollSocketChannel::class.java)
    }

    /* Byte Buf Allocator */
    var custom = true
    when (configProperties.byteBufAllocator) {
      "netty-default" -> setByteBufAllocator(ByteBufAllocator.DEFAULT)
      "default", "pooled" -> setByteBufAllocator(PooledByteBufAllocator.DEFAULT)
      "unpooled" -> setByteBufAllocator(UnpooledByteBufAllocator.DEFAULT)
      else -> {
        log.warn("Invalid byte buf allocator \"${configProperties.byteBufAllocator}\", defaulting to the 'pooled' byte buf allocator.")
        custom = false
      }
    }

    if (custom) {
      log.info("Using the '${configProperties.byteBufAllocator}' byte buf allocator")
    }
  }.create()
}
