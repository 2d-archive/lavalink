package lavalink.server.player.filters.configs

import com.github.natanbc.lavadsp.rotation.RotationPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

class RotationConfig(private val speed: Float) : FilterConfig() {
  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter? {
    return RotationPcmAudioFilter(output, format.sampleRate)
      .setRotationSpeed(speed.toDouble())
  }

  override fun isEnabled(): Boolean = isSet(speed, 1f)
}