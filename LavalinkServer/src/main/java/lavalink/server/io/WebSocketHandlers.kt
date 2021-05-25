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

package lavalink.server.io

import com.sedmelluq.discord.lavaplayer.track.TrackMarker
import lavalink.server.player.TrackEndMarkerHandler
import lavalink.server.player.filters.configs.Band
import lavalink.server.player.filters.FilterChain
import lavalink.server.util.Util
import moe.kyokobot.koe.VoiceServerInfo
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WebSocketHandlers(private val contextMap: Map<String, SocketContext>) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(WebSocketHandlers::class.java)
  }

  private var loggedVolumeDeprecationWarning = false
  private var loggedEqualizerDeprecationWarning = false

  fun voiceUpdate(context: SocketContext, json: JSONObject) {
    val sessionId = json.getString("sessionId")
    val guildId = json.getLong("guildId")

    val event = json.getJSONObject("event")
    val endpoint: String? = event.optString("endpoint")
    val token: String = event.getString("token")

    //discord sometimes send a partial server update missing the endpoint, which can be ignored.
    endpoint ?: return

    val player = context.getPlayer(guildId)
    val conn = context.getMediaConnection(player)
    conn.connect(VoiceServerInfo(sessionId, endpoint, token))
    player.provideTo(conn)
  }

  fun play(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    val noReplace = json.optBoolean("noReplace", false)

    if (noReplace && player.playingTrack != null) {
      log.info("Skipping play request because of noReplace")
      return
    }

    val track = Util.decodeAudioTrack(context.audioPlayerManager, json.getString("track"))

    if (json.has("startTime")) {
      track.position = json.getLong("startTime")
    }

    player.setPause(json.optBoolean("pause", false))
    if (json.has("volume")) {
      if (!loggedVolumeDeprecationWarning) log.warn("The volume property in the play operation has been deprecated" +
        "and will be removed in v4. Please configure a filter instead. Note that the new filter takes a " +
        "float value with 1.0 being 100%")
      loggedVolumeDeprecationWarning = true
      val filters = player.filters ?: FilterChain()
      filters.volume = json.getFloat("volume") / 100
      player.filters = filters
    }

    if (json.has("endTime")) {
      val stopTime = json.getLong("endTime")
      if (stopTime > 0) {
        val handler = TrackEndMarkerHandler(player)
        val marker = TrackMarker(stopTime, handler)
        track.setMarker(marker)
      }
    }

    player.play(track)

    val conn = context.getMediaConnection(player)
    context.getPlayer(json.getString("guildId")).provideTo(conn)
  }

  fun stop(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    player.stop()
  }

  fun pause(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    player.setPause(json.getBoolean("pause"))
    SocketServer.sendPlayerUpdate(context, player)
  }

  fun seek(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    player.seekTo(json.getLong("position"))
    SocketServer.sendPlayerUpdate(context, player)
  }

  fun volume(context: SocketContext, json: JSONObject) {
    val player = context.getPlayer(json.getString("guildId"))
    player.setVolume(json.getInt("volume"))
  }

  fun equalizer(context: SocketContext, json: JSONObject) {
    if (!loggedEqualizerDeprecationWarning) log.warn("The 'equalizer' op has been deprecated in favour of the " +
      "'filters' op. Please switch to use that one, as this op will get removed in v4.")

    loggedEqualizerDeprecationWarning = true

    val player = context.getPlayer(json.getString("guildId"))
    val list = mutableListOf<Band>()
    json.getJSONArray("bands").forEach { b ->
      val band = b as JSONObject
      list.add(Band(band.getInt("band"), band.getFloat("gain")))
    }

    val filters = player.filters ?: FilterChain()
    filters.equalizer = list
    player.filters = filters
  }

  fun destroy(context: SocketContext, json: JSONObject) {
    context.destroy(json.getLong("guildId"))
  }

  fun configureResuming(context: SocketContext, json: JSONObject) {
    context.resumeKey = json.optString("key", null)
    if (json.has("timeout")) context.resumeTimeout = json.getLong("timeout")
  }

  fun filters(context: SocketContext, guildId: String, json: String) {
    val player = context.getPlayer(guildId)

    try {
      val filters = FilterChain.parse(json)
      player.filters = filters
    } catch (ex: Exception) {
      log.error("Error while parsing filters.", ex)
    }

  }
}
