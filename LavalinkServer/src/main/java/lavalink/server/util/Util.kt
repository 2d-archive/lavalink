/*
 *  Copyright (c) 2021 Freya Arbjerg and contributors
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

package lavalink.server.util;

import com.github.natanbc.lavadsp.natives.TimescaleNativeLibLoader
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import org.apache.commons.codec.binary.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.jvm.Throws

object Util {
  private var TIMESCALE_ENABLED: Boolean? = null

  fun isTimescaleLoaded(): Boolean {
    if (TIMESCALE_ENABLED != null) {
      return TIMESCALE_ENABLED!!
    }

    TIMESCALE_ENABLED = try {
      TimescaleNativeLibLoader.loadTimescaleLibrary();
      true;
    } catch (e: Throwable) {
      false;
    }

    return TIMESCALE_ENABLED!!
  }

  /**
   * Decodes the supplied [message] into an [AudioTrack].
   *
   * @param apm The audio player manager
   * @param message The base64 encoded audio track
   */
  @JvmStatic
  fun decodeAudioTrack(apm: AudioPlayerManager, message: String): AudioTrack {
    val base64 = Base64.decodeBase64(message);
    return ByteArrayInputStream(base64) {
      val input = MessageInput(this)
      apm.decodeTrack(input).decodedTrack;
    }
  }

  /**
   * Encodes the supplied [AudioTrack] into a base64 string.
   *
   * @param apm The audio player manager.
   * @param track The audio track to encode.
   */
  @JvmStatic
  @Throws(java.io.IOException::class)
  fun encodeAudioTrack(apm: AudioPlayerManager, track: AudioTrack): String = ByteArrayOutputStream {
    apm.encodeTrack(MessageOutput(this), track)
    Base64.encodeBase64String(toByteArray())
  }

  /**
   * Convenience method for creatina a [ByteArrayOutputStream] and then closing it.
   *
   * @param block
   */
  fun <T> ByteArrayOutputStream(block: ByteArrayOutputStream.() -> T): T {
    return ByteArrayOutputStream()
      .use(block)
  }

  /**
   * Convenience method for creating a [ByteArrayInputStream] and then closing it
   *
   * @param block
   */
  fun <T> ByteArrayInputStream(buf: ByteArray, block: ByteArrayInputStream.() -> T): T {
    return ByteArrayInputStream(buf)
      .use(block)
  }
}
