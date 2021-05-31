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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.*
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup
import com.sedmelluq.lava.extensions.youtuberotator.planner.*
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv4Block
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetAddress
import java.util.function.Predicate

/**
 * Created by napster on 05.03.18.
 */
@Configuration
class AudioPlayerConfiguration {

  private val log = LoggerFactory.getLogger(AudioPlayerConfiguration::class.java)

  @Bean
  fun audioPlayerManagerSupplier(
    sources: AudioSourcesConfig,
    lavaplayerProps: LavaplayerConfigProperties,
    routePlanner: AbstractRoutePlanner?
  ): AudioPlayerManager {
    val audioPlayerManager = DefaultAudioPlayerManager()

    if (lavaplayerProps.isGcWarnings) {
      audioPlayerManager.enableGcMonitoring()
    }

    if (sources.youtube) {
      val youtube = YoutubeAudioSourceManager(lavaplayerProps.isYoutubeSearchEnabled)
      if (routePlanner != null) {
        val retryLimit = lavaplayerProps.ratelimit?.retryLimit ?: -1
        when {
          retryLimit < 0 -> YoutubeIpRotatorSetup(routePlanner).forSource(youtube).setup()
          retryLimit == 0 -> YoutubeIpRotatorSetup(routePlanner).forSource(youtube).withRetryLimit(Int.MAX_VALUE)
            .setup()
          else -> YoutubeIpRotatorSetup(routePlanner).forSource(youtube).withRetryLimit(retryLimit).setup()
        }
      }

      val playlistLoadLimit = lavaplayerProps.youtubePlaylistLoadLimit
      if (playlistLoadLimit != null) {
        youtube.setPlaylistPageCount(playlistLoadLimit)
      }

      audioPlayerManager.registerSourceManager(youtube)
    }

    if (sources.soundcloud) {
      val dataReader = DefaultSoundCloudDataReader()
      val htmlDataLoader = DefaultSoundCloudHtmlDataLoader()
      val formatHandler = DefaultSoundCloudFormatHandler()

      audioPlayerManager.registerSourceManager(
        SoundCloudAudioSourceManager(
          lavaplayerProps.isSoundcloudSearchEnabled,
          dataReader,
          htmlDataLoader,
          formatHandler,
          DefaultSoundCloudPlaylistLoader(htmlDataLoader, dataReader, formatHandler)
        )
      )
    }

    if (sources.bandcamp) audioPlayerManager.registerSourceManager(BandcampAudioSourceManager())
    if (sources.twitch) audioPlayerManager.registerSourceManager(TwitchStreamAudioSourceManager())
    if (sources.vimeo) audioPlayerManager.registerSourceManager(VimeoAudioSourceManager())
    if (sources.http) audioPlayerManager.registerSourceManager(HttpAudioSourceManager())
    if (sources.local) audioPlayerManager.registerSourceManager(LocalAudioSourceManager())

    audioPlayerManager.configuration.isFilterHotSwapEnabled = true
    audioPlayerManager.frameBufferDuration = lavaplayerProps.frameBufferDuration
    if (lavaplayerProps.nonAllocating) {
      log.info("Using the non-allocating frame buffer.")
      audioPlayerManager.configuration.setFrameBufferFactory(::NonAllocatingAudioFrameBuffer)
    }

    return audioPlayerManager
  }

  @Bean
  fun routePlanner(lavaplayerProps: LavaplayerConfigProperties): AbstractRoutePlanner? {
    val rateLimitConfig = lavaplayerProps.ratelimit
    if (rateLimitConfig == null) {
      log.debug("No rate limit config block found, skipping setup of route planner")
      return null
    }

    val ipBlockList = rateLimitConfig.ipBlocks
    if (ipBlockList.isEmpty()) {
      log.info("List of ip blocks is empty, skipping setup of route planner")
      return null
    }

    val blacklisted = rateLimitConfig.excludedIps.map { InetAddress.getByName(it) }
    val filter = Predicate<InetAddress> { !blacklisted.contains(it) }
    val ipBlocks = ipBlockList.map {
      when {
        Ipv4Block.isIpv4CidrBlock(it) -> Ipv4Block(it)
        Ipv6Block.isIpv6CidrBlock(it) -> Ipv6Block(it)
        else -> throw RuntimeException("Invalid IP Block '$it', make sure to provide a valid CIDR notation")
      }
    }

    return when (rateLimitConfig.strategy.lowercase().trim()) {
      "rotateonban" -> RotatingIpRoutePlanner(ipBlocks, filter, rateLimitConfig.searchTriggersFail)
      "loadbalance" -> BalancingIpRoutePlanner(ipBlocks, filter, rateLimitConfig.searchTriggersFail)
      "nanoswitch" -> NanoIpRoutePlanner(ipBlocks, rateLimitConfig.searchTriggersFail)
      "rotatingnanoswitch" -> RotatingNanoIpRoutePlanner(ipBlocks, filter, rateLimitConfig.searchTriggersFail)
      else -> throw RuntimeException("Unknown strategy!")
    }
  }

}
