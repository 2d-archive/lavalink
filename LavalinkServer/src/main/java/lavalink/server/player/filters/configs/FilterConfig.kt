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
