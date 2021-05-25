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

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import io.netty.buffer.ByteBuf;
import lavalink.server.config.ServerConfig;
import lavalink.server.io.SocketContext;
import lavalink.server.io.SocketServer;
import lavalink.server.player.filters.FilterChain;
import moe.kyokobot.koe.MediaConnection;
import moe.kyokobot.koe.media.OpusAudioFrameProvider;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Player extends AudioEventAdapter {

  private final SocketContext socketContext;
  private final ServerConfig serverConfig;

  private final String guildId;
  private final AudioPlayer player;
  private final AudioLossCounter audioLossCounter = new AudioLossCounter();

  private ScheduledFuture<?> myFuture = null;
  private FilterChain filters;

  public Player(SocketContext socketContext, String guildId, AudioPlayerManager audioPlayerManager, ServerConfig serverConfig) {
    this.socketContext = socketContext;
    this.serverConfig = serverConfig;

    this.guildId = guildId;
    this.player = audioPlayerManager.createPlayer();
    this.player.addListener(this);
    this.player.addListener(new EventEmitter(audioPlayerManager, this));
    this.player.addListener(audioLossCounter);
  }


  public JSONObject getState() {
    JSONObject json = new JSONObject();

    if (player.getPlayingTrack() != null)
      json.put("position", player.getPlayingTrack().getPosition());
    json.put("time", System.currentTimeMillis());

    return json;
  }


  public String getGuildId() {
    return guildId;
  }

  /**
   * The player update interval.
   */
  private int getInterval() {
    return serverConfig.getPlayerUpdateInterval();
  }

  /**
   * The socket context.
   */
  SocketContext getSocket() {
    return socketContext;
  }

  /**
   * The audio loss counter.
   */
  public AudioLossCounter getAudioLossCounter() {
    return audioLossCounter;
  }

  /**
   * The current audio track that is playing.
   */
  @Nullable
  public AudioTrack getPlayingTrack() {
    return player.getPlayingTrack();
  }

  /**
   * Returns the current filter chain.
   */
  @Nullable
  public FilterChain getFilters() {
    return filters;
  }

  /**
   * Configures the filter chain for this player.
   * @param filterChain The filter chain to use.
   */
  public void setFilters(FilterChain filterChain) {
    this.filters = filterChain;

    if (filterChain.isEnabled()) {
      player.setFilterFactory(filterChain);
    } else {
      player.setFilterFactory(null);
    }
  }

  /**
   * Sets the pause state.
   * @param state The pause state.
   */
  public void setPause(boolean state) {
    player.setPaused(state);
  }

  /**
   * Sets the volume of this player.
   * @param volume The volume to use.
   */
  public void setVolume(int volume) {
    player.setVolume(volume);
  }


  /**
   * Plays an audio track.
   * @param track The track to play.
   */
  public void play(AudioTrack track) {
    player.playTrack(track);
    SocketServer.Companion.sendPlayerUpdate(socketContext, this);
  }

  /**
   * Stops the currently playing track.
   */
  public void stop() {
    player.stopTrack();
  }

  /**
   * Seek to the specified position in the current playing song.
   * @param position The position to seek to.
   */
  public void seekTo(long position) {
    AudioTrack track = getPlayingTrack();
    if (track == null) {
      throw new RuntimeException("Can't seek when not playing anything");
    }

    track.setPosition(position);
  }

  /**
   * Whether this player has been paused.
   */
  public boolean isPaused() {
    return player.isPaused();
  }

  /**
   * Whether this player is playing something.
   */
  public boolean isPlaying() {
    return player.getPlayingTrack() != null && !player.isPaused();
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    myFuture.cancel(false);
  }

  @Override
  public void onTrackStart(AudioPlayer player, AudioTrack track) {
    if (myFuture == null || myFuture.isCancelled()) {
      myFuture = socketContext.getPlayerUpdateService().scheduleAtFixedRate(() -> {
        if (socketContext.getSessionPaused()) return;

        SocketServer.Companion.sendPlayerUpdate(socketContext, this);
      }, 0, this.getInterval(), TimeUnit.SECONDS);
    }
  }

  public void provideTo(MediaConnection connection) {
    connection.setAudioSender(new Provider(connection));
  }

  private class Provider extends OpusAudioFrameProvider {
    /**
     * The last frame that was sent.
     */
    private final MutableAudioFrame lastFrame = new MutableAudioFrame();

    public Provider(MediaConnection connection) {
      super(connection);

      ByteBuffer frameBuffer = ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize());
      lastFrame.setBuffer(frameBuffer);
    }

    @Override
    public boolean canProvide() {
      var sent = player.provide(lastFrame);
      if (!sent) {
        audioLossCounter.onLoss();
      }

      return sent;
    }

    @Override
    public void retrieveOpusFrame(ByteBuf buf) {
      audioLossCounter.onSuccess();
      buf.writeBytes(lastFrame.getData());
    }
  }

}
