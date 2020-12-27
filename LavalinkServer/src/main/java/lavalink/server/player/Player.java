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

package lavalink.server.player;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import io.netty.buffer.ByteBuf;
import lavalink.server.config.ServerConfig;
import lavalink.server.io.SocketContext;
import lavalink.server.io.SocketServer;
import lavalink.server.player.filters.FilterChain;
import moe.kyokobot.koe.VoiceConnection;
import moe.kyokobot.koe.media.OpusAudioFrameProvider;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Player extends AudioEventAdapter {

  private final SocketContext socketContext;
  private final ServerConfig serverConfig;

  private final String guildId;
  private final AudioPlayer player;
  private final AudioLossCounter audioLossCounter = new AudioLossCounter();
  private AudioFrame lastFrame = null;
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

  public void play(AudioTrack track) {
    player.playTrack(track);
    SocketServer.Companion.sendPlayerUpdate(socketContext, this);
  }

  public void stop() {
    player.stopTrack();
  }

  public void setPause(boolean b) {
    player.setPaused(b);
  }

  public String getGuildId() {
    return guildId;
  }

  public void seekTo(long position) {
    AudioTrack track = player.getPlayingTrack();

    if (track == null) throw new RuntimeException("Can't seek when not playing anything");

    track.setPosition(position);
  }

  public void setVolume(int volume) {
    player.setVolume(volume);
  }

  public JSONObject getState() {
    JSONObject json = new JSONObject();

    if (player.getPlayingTrack() != null)
      json.put("position", player.getPlayingTrack().getPosition());
    json.put("time", System.currentTimeMillis());

    return json;
  }

  SocketContext getSocket() {
    return socketContext;
  }

  @Nullable
  public AudioTrack getPlayingTrack() {
    return player.getPlayingTrack();
  }

  public boolean isPaused() {
    return player.isPaused();
  }

  public AudioLossCounter getAudioLossCounter() {
    return audioLossCounter;
  }

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

  private int getInterval() {
    return serverConfig.getPlayerUpdateInterval();
  }

  public void provideTo(VoiceConnection connection) {
    connection.setAudioSender(new Provider(connection));
  }

  @Nullable
  public FilterChain getFilters() {
    return filters;
  }

  public void setFilters(FilterChain filters) {
    this.filters = filters;

    if (filters.isEnabled()) {
      player.setFilterFactory(filters);
    } else {
      player.setFilterFactory(null);
    }
  }

  private class Provider extends OpusAudioFrameProvider {
    public Provider(VoiceConnection connection) {
      super(connection);
    }

    @Override
    public boolean canProvide() {
      lastFrame = player.provide();

      if (lastFrame == null) {
        audioLossCounter.onLoss();
        return false;
      } else {
        return true;
      }
    }

    @Override
    public void retrieveOpusFrame(ByteBuf buf) {
      audioLossCounter.onSuccess();
      buf.writeBytes(lastFrame.getData());
    }
  }

}
