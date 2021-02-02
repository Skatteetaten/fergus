package performance

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.util.Properties._

class PerformanceRefapp extends Simulation {

		val targetHost = propOrElse("gatling.targetHost", "http://localhost:8080")
		val numUsers = propOrElse("gatling.numUsers", "5").toInt
		val timeSpan = propOrElse("gatling.timeSpan", "2").toLong
		val korId = "PerformanceTest :" + System.currentTimeMillis().toString + "." + Math.random().toString
		println("Running Performance Test at host: " + targetHost)

		val httpProtocol = http
			.baseUrl(targetHost)
			.inferHtmlResources(BlackList(""".*\.css, .*\.js and .*\.ico"""), WhiteList())
			.header("korrelasjonsid", session => korId)
			.acceptHeader("*/*")
			.contentTypeHeader("application/json")
			.userAgentHeader("PostmanRuntime/6.4.1")

		val counterHeader = Map(
			"Postman-Token" -> "32dc1206-61d3-43e4-90a8-260d8739a17e",
			"accept-encoding" -> "gzip, deflate",
			"cache-control" -> "no-cache")

		val scn = scenario("PerformanceRefapp")
			.exec(http("counterRequest")
			.get("/api/counter")
			.headers(counterHeader))

		setUp(
			scn.inject(rampUsers(numUsers) during (timeSpan seconds))
  			.protocols(httpProtocol)
		).assertions(
			global.successfulRequests.percent.is(100),
			global.failedRequests.count.is(0)
		)
}