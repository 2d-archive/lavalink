package lavalink.server.player.filters.configs

import com.github.natanbc.lavadsp.vibrato.VibratoPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

class VibratoConfig(
  private val frequency: Float = 2.0f,
  private val depth: Float = 0.5f
) : FilterConfig() {
  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): VibratoPcmAudioFilter {
    return VibratoPcmAudioFilter(output, format.channelCount, format.sampleRate)
      .setDepth(depth)
      .setFrequency(frequency)
  }

  override fun isEnabled(): Boolean = depth != 0.0f
}