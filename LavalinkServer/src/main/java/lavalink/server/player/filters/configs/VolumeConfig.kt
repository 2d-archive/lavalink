package lavalink.server.player.filters.configs

import com.github.natanbc.lavadsp.volume.VolumePcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

class VolumeConfig(private var volume: Float) : FilterConfig() {
  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter {
    return VolumePcmAudioFilter(output, format.channelCount).also {
      it.volume = volume
    }
  }

  override fun isEnabled(): Boolean = isSet(volume, 1.0f)
}
