package lavalink.server.player.filters.configs

import com.github.natanbc.lavadsp.lowpass.LowPassPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

class LowPassConfig(private val smoothing: Float) : FilterConfig() {
  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter? = if (isEnabled())
    LowPassPcmAudioFilter(output, format.channelCount)
      .setSmoothing(smoothing)
  else null

  override fun isEnabled(): Boolean = isSet(smoothing, 20f)
}