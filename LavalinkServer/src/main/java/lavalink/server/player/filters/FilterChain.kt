/*
 *  Copyright (c) 2021 Dimensional Fun
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */
package lavalink.server.player.filters

import com.google.gson.Gson
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.PcmFilterFactory
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import lavalink.server.player.filters.configs.*

class FilterChain : PcmFilterFactory {

  companion object {
    private val gson = Gson()
    fun parse(json: String) =
      gson.fromJson(json, FilterChain::class.java)!!
  }

  var volume: Float? = null
  var equalizer: List<Band>? = null
  private val karaoke: KaraokeConfig? = null
  private val timescale: TimescaleConfig? = null
  private val tremolo: TremoloConfig? = null
  private val distortion: DistortionConfig? = null
  private val lowPass: LowPassConfig? = null
  private val rotation: RotationConfig? = null
  private val channelMix: ChannelMixConfig? = null
  private val vibrato: VibratoConfig? = null

  private fun buildList() = listOfNotNull(
    volume?.let { VolumeConfig(it) },
    equalizer?.let { EqualizerConfig(it) },
    karaoke,
    timescale,
    tremolo,
    distortion,
    vibrato,
    lowPass,
    rotation,
    channelMix
  )

  val isEnabled get() = buildList().any { it.isEnabled() }

  override fun buildChain(
    track: AudioTrack?,
    format: AudioDataFormat,
    output: UniversalPcmAudioFilter
  ): MutableList<AudioFilter> {
    val enabledFilters = buildList()
      .filter { it.isEnabled() }
      .takeIf { it.isNotEmpty() }
      ?: return mutableListOf()

    val pipeline = mutableListOf<FloatPcmAudioFilter>()
    for (filter in enabledFilters) {
      val built = filter.build(format, pipeline.lastOrNull() ?: output)
      if (built != null) {
        pipeline.add(built)
      }
    }

    return pipeline.reversed().toMutableList()
  }

}
