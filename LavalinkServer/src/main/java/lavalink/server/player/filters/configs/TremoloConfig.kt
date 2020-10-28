package lavalink.server.player.filters.configs

import com.github.natanbc.lavadsp.tremolo.TremoloPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

class TremoloConfig(
  private val frequency: Float = 2.0f,
  private val depth: Float = 0.5f
) : FilterConfig() {
  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter {
    return TremoloPcmAudioFilter(output, format.channelCount, format.sampleRate)
      .setFrequency(frequency)
      .setDepth(depth)
  }

  override fun isEnabled(): Boolean = isSet(frequency, 2f) || isSet(depth, 0.5f);

}