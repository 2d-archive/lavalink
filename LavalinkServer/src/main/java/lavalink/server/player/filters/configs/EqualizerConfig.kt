package lavalink.server.player.filters.configs

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat

class EqualizerConfig(bands: List<Band>) : FilterConfig() {
  private val array = FloatArray(Equalizer.BAND_COUNT) { 0.0f }

  init {
    bands.forEach { array[it.band] = it.gain }
  }

  override fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter? =
    if (Equalizer.isCompatible(format)) Equalizer(format.channelCount, output, array) else null

  override fun isEnabled(): Boolean =
    array.any { isSet(it, 0f) }
}

data class Band(val band: Int, val gain: Float)