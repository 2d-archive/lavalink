package lavalink.server.player.filters.configs

import com.github.natanbc.lavadsp.channelmix.ChannelMixPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

class ChannelMixConfig(
  val leftToRight: Float,
  val rightToRight: Float,
  val rightToLeft: Float,
  val leftToLeft: Float
) : FilterConfig() {
  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter? {
    return ChannelMixPcmAudioFilter(output)
      .setRightToLeft(rightToLeft)
      .setLeftToLeft(leftToLeft)
      .setLeftToRight(leftToRight)
      .setRightToRight(rightToRight)
  }

  override fun isEnabled(): Boolean {
    return isSet(leftToLeft, 1.0f) || isSet(leftToRight, 0.0f)
      || isSet(rightToLeft, 0.0f) || isSet(rightToRight, 1.0f);
  }
}