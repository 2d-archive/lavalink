package lavalink.server.player.filters.configs

import com.github.natanbc.lavadsp.distortion.DistortionPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

class DistortionConfig(
  private val sinOffset: Float = 0f,
  private val sinScale: Float = 1f,
  private val tanOffset: Float = 0f,
  private val tanScale: Float = 1f,
  private val cosOffset: Float = 0f,
  private val cosScale: Float = 1f
) : FilterConfig()  {
  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter {
    return DistortionPcmAudioFilter(output, format.channelCount)
      .setSinOffset(sinOffset)
      .setSinScale(sinScale)
      .setTanOffset(tanOffset)
      .setTanScale(tanScale)
      .setCosOffset(cosOffset)
      .setCosScale(cosScale)
  }

  private val offsets = listOf(sinOffset, tanOffset, cosOffset)
  private val scales = listOf(cosScale, sinScale, tanScale)

  override fun isEnabled(): Boolean
    = true // offsets.any { it != 0.0f } || scales.any { it != 1f }
}