package lavalink.server.player.filters

import com.google.gson.Gson
import com.sedmelluq.discord.lavaplayer.filter.*
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

class FilterChain : PcmFilterFactory {

    companion object {
        private val gson = Gson()
        fun parse(json: String) = gson.fromJson(json, FilterChain::class.java)!!
    }

    var volume: Float? = null
    var equalizer: List<Band>? = null
    private val karaoke: KaraokeConfig? = null
    private val timescale: TimescaleConfig? = null
    private val tremolo: TremoloConfig? = null
    private val vibrato: VibratoConfig? = null

    private fun buildList() = listOfNotNull(
            volume?.let { VolumeConfig(it) },
            equalizer?.let { EqualizerConfig(it) },
            karaoke,
            timescale,
            tremolo,
            vibrato
    )

    val isEnabled get() = buildList().any { it.isEnabled }

    override fun buildChain(track: AudioTrack?, format: AudioDataFormat, output: UniversalPcmAudioFilter): MutableList<AudioFilter> {
        val builder = FilterChainBuilder()
        builder.addFirst(output)

        for (config in buildList()) {
            if (config.isEnabled) {
                val filter = config.build(format, builder.makeFirstFloat(format.channelCount))
                builder.addFirst(filter)
            }
        }

        val list = builder.build(null, format.channelCount).filters
        return list.subList(1, list.size)
    }

}