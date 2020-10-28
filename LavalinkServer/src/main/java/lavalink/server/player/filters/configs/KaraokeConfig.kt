package lavalink.server.player.filters.configs

import com.github.natanbc.lavadsp.karaoke.KaraokePcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

class KaraokeConfig(
  private val level: Float = 1.0f,
  private val monoLevel: Float = 1.0f,
  private val filterBand: Float = 220.0f,
  private val filterWidth: Float = 100.0f
) : FilterConfig() {
  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter {
    return KaraokePcmAudioFilter(output, format.channelCount, format.sampleRate)
      .setLevel(level)
      .setMonoLevel(monoLevel)
      .setFilterBand(filterBand)
      .setFilterWidth(filterWidth)
  }

  override fun isEnabled(): Boolean = isSet(level, 1f) || isSet(monoLevel, 1f) ||
    isSet(filterBand, 220f) || isSet(filterWidth, 100f);
}