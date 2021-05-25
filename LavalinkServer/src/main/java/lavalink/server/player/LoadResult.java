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

package lavalink.server.player;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

class LoadResult {
  public ResultStatus loadResultType;
  public List<AudioTrack> tracks;
  public @Nullable String playlistName;
  public @Nullable Integer selectedTrack;
  public FriendlyException exception;

  public LoadResult(ResultStatus loadResultType, List<AudioTrack> tracks,
                    @Nullable String playlistName, @Nullable Integer selectedTrack) {

    this.loadResultType = loadResultType;
    this.tracks = Collections.unmodifiableList(tracks);
    this.playlistName = playlistName;
    this.selectedTrack = selectedTrack;
    this.exception = null;
  }

  public LoadResult(FriendlyException exception) {
    this.loadResultType = ResultStatus.LOAD_FAILED;
    this.tracks = Collections.emptyList();
    this.playlistName = null;
    this.selectedTrack = null;
    this.exception = exception;
  }
}
