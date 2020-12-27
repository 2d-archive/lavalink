/*
 * Copyright (c) 2017 Frederik Ar. Mikkelsen & NoobLance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lavalink.server.util;

import com.github.natanbc.lavadsp.natives.TimescaleNativeLibLoader;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageOutput;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.apache.commons.codec.binary.Base64;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Util {
  @Nullable
  private static Boolean TIMESCALE_ENABLED = null;

  public static Boolean isTimescaleLoaded() {
    if (TIMESCALE_ENABLED != null) {
      return TIMESCALE_ENABLED;
    }

    try {
      TimescaleNativeLibLoader.loadTimescaleLibrary();
      return TIMESCALE_ENABLED = true;
    } catch (Throwable e) {
      return TIMESCALE_ENABLED = false;
    }
  }

  public static AudioTrack toAudioTrack(AudioPlayerManager audioPlayerManager, String message) throws IOException {
    byte[] b64 = Base64.decodeBase64(message);
    ByteArrayInputStream in = new ByteArrayInputStream(b64);
    return audioPlayerManager.decodeTrack(new MessageInput(in)).decodedTrack;
  }

  public static String toMessage(AudioPlayerManager audioPlayerManager, AudioTrack track) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    audioPlayerManager.encodeTrack(new MessageOutput(out), track);
    return Base64.encodeBase64String(out.toByteArray());
  }

}
