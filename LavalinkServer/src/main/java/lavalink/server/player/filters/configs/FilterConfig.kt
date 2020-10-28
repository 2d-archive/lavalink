package lavalink.server.player.filters.configs

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import kotlin.math.abs

abstract class FilterConfig {
  /**
   * Builds the audio filter.
   */
  abstract fun build(format: AudioDataFormat, output: FloatPcmAudioFilter): FloatPcmAudioFilter?

  /**
   * Whether this filter is enabled or not.
   *
   * @return a boolean
   */
  abstract fun isEnabled(): Boolean

  /* Below this line is copy pasted code from https://github.com/natanbc/andesite/blob/master/api/src/main/java/andesite/player/filter/Config.java... lmao */

  /**
   * Returns true if the difference between `value` and `defaultValue`
   * is greater or equal to [.MINIMUM_FP_DIFF].
   *
   * @param value        Value to check.
   * @param defaultValue Default value.
   *
   * @return True if the difference is greater or equal to the minimum.
   */
  open fun isSet(value: Float, defaultValue: Float): Boolean {
    return abs(value - defaultValue) >= MINIMUM_FP_DIFF
  }

  companion object {
    /**
     * Minimum absolute difference for floating point values. Values whose difference to the default
     * value are smaller than this are considered equal to the default.
     */
    var MINIMUM_FP_DIFF = 0.01f
  }
}