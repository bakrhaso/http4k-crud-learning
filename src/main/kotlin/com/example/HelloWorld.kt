package com.example

import com.example.formats.jacksonMessage
import com.example.formats.jacksonMessageLens
import com.example.routes.ExampleContractRoute
import com.example.user.userRoutes
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.http4k.contract.bind
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.events.*
import org.http4k.filter.CorsPolicy
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.ResponseFilters
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.time.Clock

val app: HttpHandler = routes(
	"/ping" bind GET to {
		Response(OK).body("pong")
	},

	"/formats/json/jackson" bind GET to {
		Response(OK).with(jacksonMessageLens of jacksonMessage)
	},

	"/contract/api/v1" bind contract {
		renderer = OpenApi3(ApiInfo("HelloWorld API", "v1.0"))

		// Return Swagger API definition under /contract/api/v1/swagger.json
		descriptionPath = "/swagger.json"

		// You can use security filter tio protect routes
		security = ApiKeySecurity(Query.int().required("api"), { it == 42 }) // Allow only requests with &api=42

		// Add contract routes
		routes += ExampleContractRoute()
	},

	"api/v1/users" bind userRoutes()
)

private fun getClock(): Clock {
	return Clock.systemUTC()
}

fun main() {
	// clock
	val javaClock = getClock()

	// database config/connection
	val hikariConfig = HikariConfig().apply {
		jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
		driverClassName = "org.postgresql.Driver"
		username = "postgres"
		password = "examplepassword"
		maximumPoolSize = 6
		// if these options are set here, they do not need to be duplicated in DatabaseConfig
		isReadOnly = false
		transactionIsolation = "TRANSACTION_SERIALIZABLE"
	}
	val dataSource = HikariDataSource(hikariConfig)
	val database = Database.connect(dataSource, databaseConfig = DatabaseConfig())
	TransactionManager.defaultDatabase = database

	// structured logging
	val events = EventFilters.AddTimestamp(javaClock)
		.then(EventFilters.AddEventName())
		.then(EventFilters.AddZipkinTraces())
//		.then(addRequestCount())
		.then(AutoMarshallingEvents(Jackson))

	// cors
	val cors = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)

	val printingApp: HttpHandler = ResponseFilters.ReportHttpTransaction {
		// to "emit" an event, just invoke() the Events!
		events(
			IncomingHttpRequest(
				uri = it.request.uri.toString(),
				status = it.response.status.code,
				duration = it.duration.toMillis(),
				message = it.request.toMessage(),
			)
		)
	}
		.then(ServerFilters.CatchAll {
			events(
				ErrorEvent(
					message = it.message
				)
			)
			Response(INTERNAL_SERVER_ERROR).body("Server error")
		})
//		.then(DebuggingFilters.PrintRequestAndResponse())
		.then(cors)
		.then(app)

	val server = printingApp.asServer(SunHttp(9000)).start()

	println("Server started on " + server.port())
}

// this is our custom event which will be printed in a structured way
data class IncomingHttpRequest(
	val uri: String,
	val status: Int,
	val duration: Long,
	val message: String,
) : Event

data class ErrorEvent(
	val message: String?,
) : Event

// here is a new EventFilter that adds custom metadata to the emitted events
fun addRequestCount(): EventFilter {
	var requestCount = 0
	return EventFilter { next ->
		{
			next(it + ("requestCount" to requestCount++))
		}
	}
}
