package lavalink.server.player

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.sentry.Sentry
import lavalink.server.util.Util
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.util.concurrent.CompletionStage
import javax.servlet.http.HttpServletRequest

@RestController
class AudioLoaderRestHandler(
  private val audioPlayerManager: AudioPlayerManager
) {
  @GetMapping(value = ["/loadtracks", "/search"], produces = ["application/json"])
  @ResponseBody
  fun getLoadTracks(
    request: HttpServletRequest,
    @RequestParam identifier: String
  ): CompletionStage<ResponseEntity<String>> {
    log.info("Got request to load for identifier \"$identifier\"")

    return AudioLoader(audioPlayerManager).load(identifier)
      .thenApply(this::encodeLoadResult)
      .thenApply { ResponseEntity(it.toString(), HttpStatus.OK) }
  }

  @GetMapping(value = ["/decodetrack"], produces = ["application/json"])
  @ResponseBody
  fun getDecodeTrack(request: HttpServletRequest, @RequestParam track: String): ResponseEntity<String> {
    log(request)
    val audioTrack = Util.toAudioTrack(audioPlayerManager, track)
    return ResponseEntity(trackToJSON(audioTrack).toString(), HttpStatus.OK)
  }

  @PostMapping(value = ["/decodetracks"], consumes = ["application/json"], produces = ["application/json"])
  @ResponseBody
  fun postDecodeTracks(request: HttpServletRequest, @RequestBody body: String): ResponseEntity<String> {
    log(request)

    val requestJson = JSONArray(body)
    val responseJson = JSONArray()

    for (i in 0 until requestJson.length()) {
      val track = requestJson.getString(i)
      val audioTrack = Util.toAudioTrack(audioPlayerManager, track)
      val trackJson = JSONObject()
        .put("track", track)
        .put("info", trackToJSON(audioTrack))

      responseJson.put(trackJson)
    }

    return ResponseEntity(responseJson.toString(), HttpStatus.OK)
  }

  private fun trackToJSON(track: AudioTrack): JSONObject =
    JSONObject()
      .put("source", track.sourceManager.sourceName)
      .put("title", track.info.title)
      .put("author", track.info.author)
      .put("length", track.info.length)
      .put("identifier", track.info.identifier)
      .put("uri", track.info.uri)
      .put("isStream", track.info.isStream)
      .put("isSeekable", track.isSeekable)
      .put("position", track.position);

  private fun log(request: HttpServletRequest) {
    val path = request.servletPath
    log.info("GET $path")
  }

  private fun encodeLoadResult(result: LoadResult): JSONObject {
    val json = JSONObject()
    val playlist = JSONObject()
    val tracks = JSONArray()

    for (track in result.tracks) {
      val trackJson = JSONObject()
      trackJson.put("info", trackToJSON(track))

      try {
        val encoded = Util.toMessage(audioPlayerManager, track)
        trackJson.put("track", encoded)
        tracks.put(trackJson)
      } catch (e: IOException) {
        Sentry.capture(e)
        log.warn("Failed to encode a track ${track.identifier}, skipping", e)
      }
    }

    playlist.put("name", result.playlistName)
    playlist.put("selectedTrack", result.selectedTrack)

    json.put("playlistInfo", playlist)
    json.put("loadType", result.loadResultType)
    json.put("tracks", tracks)

    if (result.loadResultType == ResultStatus.LOAD_FAILED && result.exception != null) {
      val exception = JSONObject()
      exception.put("message", result.exception.localizedMessage)
      exception.put("severity", result.exception.severity.name)

      json.put("exception", exception)
      log.error("Track loading failed", result.exception)
    }

    return json
  }

  companion object {
    private val log = LoggerFactory.getLogger(AudioLoaderRestHandler::class.java)
  }
}