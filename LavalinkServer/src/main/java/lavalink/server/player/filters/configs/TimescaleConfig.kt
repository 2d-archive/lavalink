package lavalink.server.player.filters.configs

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import lavalink.server.util.Util

class TimescaleConfig(
  private val speed: Float = 1f,
  private val pitch: Float = 1f,
  private val rate: Float = 1f
) : FilterConfig() {
  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter {
    return TimescalePcmAudioFilter(output, format.channelCount, format.sampleRate)
      .setSpeed(speed.toDouble())
      .setPitch(pitch.toDouble())
      .setRate(rate.toDouble())
  }

  override fun isEnabled(): Boolean = Util.isTimescaleLoaded() &&
    isSet(speed, 1.0f) || isSet(pitch, 1.0f) || isSet(rate, 1.0f)
}
