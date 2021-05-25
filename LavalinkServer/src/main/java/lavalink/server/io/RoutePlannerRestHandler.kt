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

import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.planner.NanoIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner
import org.json.JSONObject
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
class RoutePlannerRestHandler(private val routePlanner: AbstractRoutePlanner?) {

  /**
   * Returns current information about the active AbstractRoutePlanner
   */
  @GetMapping("/routeplanner/status")
  fun getStatus(request: HttpServletRequest): ResponseEntity<RoutePlannerStatus> {
    val status = when (routePlanner) {
      null -> RoutePlannerStatus(null, null)
      else -> RoutePlannerStatus(
        routePlanner.javaClass.simpleName,
        getDetailBlock(routePlanner)
      )
    }
    return ResponseEntity.ok(status)
  }

  /**
   * Removes a single address from the addresses which are currently marked as failing
   */
  @PostMapping("/routeplanner/free/address")
  fun freeSingleAddress(request: HttpServletRequest, @RequestBody requestBody: String): ResponseEntity<Void> {
    routePlanner ?: throw RoutePlannerDisabledException()
    try {
      val jsonObject = JSONObject(requestBody)
      val address = InetAddress.getByName(jsonObject.getString("address"))
      routePlanner.freeAddress(address)
      return ResponseEntity.noContent().build()
    } catch (exception: UnknownHostException) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid address: " + exception.message, exception)
    }
  }

  /**
   * Removes all addresses from the list which holds the addresses which are marked failing
   */
  @PostMapping("/routeplanner/free/all")
  fun freeAllAddresses(request: HttpServletRequest): ResponseEntity<Void> {
    routePlanner ?: throw RoutePlannerDisabledException()
    routePlanner.freeAllAddresses()
    return ResponseEntity.noContent().build()
  }

  /**
   * Detail information block for an AbstractRoutePlanner
   */
  private fun getDetailBlock(planner: AbstractRoutePlanner): IRoutePlannerStatus {
    val ipBlock = planner.ipBlock
    val ipBlockStatus = IpBlockStatus(ipBlock.type.simpleName, ipBlock.size.toString())

    val failingAddresses = planner.failingAddresses
    val failingAddressesStatus = failingAddresses.entries.map {
      FailingAddress(it.key, it.value, Date(it.value).toString())
    }

    return when (planner) {
      is RotatingIpRoutePlanner -> RotatingIpRoutePlannerStatus(
        ipBlockStatus,
        failingAddressesStatus,
        planner.rotateIndex.toString(),
        planner.index.toString(),
        planner.currentAddress.toString()
      )
      is NanoIpRoutePlanner -> NanoIpRoutePlannerStatus(
        ipBlockStatus,
        failingAddressesStatus,
        planner.currentAddress.toString()
      )
      is RotatingNanoIpRoutePlanner -> RotatingNanoIpRoutePlannerStatus(
        ipBlockStatus,
        failingAddressesStatus,
        planner.currentBlock.toString(),
        planner.addressIndexInBlock.toString()
      )
      else -> GenericRoutePlannerStatus(ipBlockStatus, failingAddressesStatus)
    }
  }

  data class RoutePlannerStatus(val `class`: String?, val details: IRoutePlannerStatus?)

  interface IRoutePlannerStatus
  data class GenericRoutePlannerStatus(
    val ipBlock: IpBlockStatus,
    val failingAddresses: List<FailingAddress>
  ) : IRoutePlannerStatus

  data class RotatingIpRoutePlannerStatus(
    val ipBlock: IpBlockStatus,
    val failingAddresses: List<FailingAddress>,
    val rotateIndex: String,
    val ipIndex: String,
    val currentAddress: String
  ) : IRoutePlannerStatus

  data class NanoIpRoutePlannerStatus(
    val ipBlock: IpBlockStatus,
    val failingAddresses: List<FailingAddress>,
    val currentAddressIndex: String
  ) : IRoutePlannerStatus

  data class RotatingNanoIpRoutePlannerStatus(
    val ipBlock: IpBlockStatus,
    val failingAddresses: List<FailingAddress>,
    val blockIndex: String,
    val currentAddressIndex: String
  ) : IRoutePlannerStatus

  data class FailingAddress(val failingAddress: String, val failingTimestamp: Long, val failingTime: String)
  data class IpBlockStatus(val type: String, val size: String)

  class RoutePlannerDisabledException :
    ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Can't access disabled route planner")
}
